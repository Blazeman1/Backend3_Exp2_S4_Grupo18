# Banco XYZ — Backend for Frontend (BFF)

**Curso:** Desarrollo Backend III (PBY2203) — Duoc UC
**Actividad:** Exp2, Semana 4 — Analizando y aplicando el patrón arquitectónico Backend for Frontend (BFF)
**Autor:** Grupo 18: Karla Santibañez Gutierrez - Fernando Fuentes Allende

## 1. Objetivo del proyecto

Implementar el patrón arquitectónico **Backend for Frontend (BFF)** para el sistema del Banco XYZ, creando un backend personalizado para cada tipo de cliente —**web**, **móvil** y **cajero automático**— que optimice la comunicación y adapte los datos a las necesidades reales de cada canal, en lugar de forzar a un único backend genérico a servir a los tres por igual.

Los datos provienen del mismo dataset legacy usado en la actividad Exp1 de este curso: [`bank_legacy_data`](https://github.com/KariVillagran/bank_legacy_data) (carpeta `data/semana_3`), reutilizando `intereses.csv` (maestro de cuentas) y `cuentas_anuales.csv` (historial de movimientos).

## 2. ¿Qué es el patrón BFF y por qué se eligió esta solución?

Un **patrón de diseño/arquitectónico** es una solución probada para un problema recurrente del desarrollo de software. El problema que resuelve **Backend for Frontend** es concreto: cuando varios tipos de cliente (un navegador de escritorio, una app móvil, un cajero automático) consumen el mismo backend, cada uno necesita datos distintos, en formatos distintos, con requisitos de seguridad distintos y con distinta tolerancia a la latencia y al peso de la respuesta. Un único backend "genérico" que intente satisfacer a los tres termina, con el tiempo, acumulando parámetros condicionales, ramas de código específicas por cliente y una superficie de autenticación mezclada — exactamente el escenario descrito en la guía de aprendizaje de esta semana ("Contexto de origen").

BFF resuelve esto invirtiendo el problema: en lugar de un backend que se adapta a todos, se construye **un backend dedicado por cada frontend**, que conoce exactamente las necesidades de su canal y expone solo lo que ese canal necesita. La transformación y optimización de datos ocurre en el BFF, no en el frontend ni en un backend centralizado que no puede permitirse esa especialización.

### 2.1 Análisis de la estrategia de implementación (instrucción específica N.º 1)

La guía de la semana describe tres estrategias no excluyentes para implementar BFF:

1. **Backends independientes por cada tipo de cliente**: un servicio desplegable por separado por canal, con su propio repositorio/ciclo de vida.
2. **Diseño de endpoints personalizados**: un mismo servicio, con rutas distintas por cliente (`/web/...`, `/mobile/...`).
3. **Aprovechar microservicios y delegar funciones al BFF**: el BFF como capa que combina y resume respuestas de microservicios existentes.

Para este proyecto se analizaron las tres a la luz de dos restricciones explícitas de las instrucciones específicas: *"Cada cliente deberá tener su propio Backend"* y *"Gestionar autenticación y autorización específicas para cada canal"*. La estrategia de **endpoints personalizados sobre un único servicio** queda descartada de inmediato: un solo proceso Spring Boot no puede tener "su propio backend" por canal en el sentido que piden las instrucciones, y mezclar tres esquemas de autenticación distintos (JWT de 30 minutos, JWT de 5 minutos, sesión opaca de 2 minutos) dentro de una misma aplicación habría recreado exactamente el backend monolítico y sobrecargado que el patrón BFF busca evitar.

Por eso, este proyecto adopta una **combinación deliberada de las estrategias 1 y 3**:

- **Estrategia 1 (backends independientes)** para satisfacer el requisito explícito: existen **cuatro aplicaciones Spring Boot completamente autónomas** —`core-service`, `bff-web`, `bff-mobile`, `bff-atm`—, cada una con su propio `pom.xml`, su propio `main()`, su propio puerto y su propio ciclo de vida. Ninguna depende de que las demás estén compiladas para arrancar.
- **Estrategia 3 (delegar en un backend generalizado)** para evitar el problema opuesto: si cada BFF leyera los CSV legacy y reimplementara su propia lógica de validación de datos, la lógica de negocio (qué es una cuenta válida, cómo se calcula el interés, etc.) quedaría triplicada y desincronizada. En su lugar, `core-service` consolida los datos legacy una sola vez y los expone mediante una API interna; los tres BFF son clientes de esa API, y cada uno decide **qué subconjunto de esos datos reenviar, en qué formato, y bajo qué reglas de autenticación**.

Esta combinación es, además, coherente con lo que la propia guía advierte: "el BFF puede funcionar sobre microservicios, gestionando datos específicos para cada frontend desde su propio backend dedicado". `core-service` cumple aquí el papel del "backend/microservicio" sobre el que operan los tres BFF.

## 3. Arquitectura general

```
                    ┌──────────────┐        ┌──────────────┐        ┌──────────────┐
                    │  Navegador   │        │   App Móvil  │        │Cajero Automát.│
                    │   (Web)      │        │              │        │    (ATM)      │
                    └──────┬───────┘        └──────┬───────┘        └──────┬───────┘
                           │ HTTP/JSON             │ HTTP/JSON             │ HTTP/JSON
                           │ JWT 30 min            │ JWT 5 min             │ Sesión opaca 2 min
                     ┌─────▼──────┐          ┌─────▼──────┐          ┌─────▼──────┐
                     │  bff-web   │          │ bff-mobile │          │  bff-atm   │
                     │ :8081      │          │ :8082      │          │ :8083      │
                     └─────┬──────┘          └─────┬──────┘          └─────┬──────┘
                           │                        │                       │
                           │   X-Internal-Api-Key (solo los BFF la conocen) │
                           └────────────────┬───────┴───────────────────────┘
                                            ▼
                                  ┌───────────────────┐
                                  │   core-service     │   ← backend generalizado,
                                  │   :8080            │     NUNCA expuesto a un frontend
                                  │ (consolida legacy:  │
                                  │  intereses.csv +    │
                                  │  cuentas_anuales.csv)│
                                  └────────────────────┘
```

Ningún frontend llama jamás directamente a `core-service`. La única forma de acceder a sus datos es a través de uno de los tres BFF, cada uno de los cuales conoce la clave interna compartida (`X-Internal-Api-Key`) que `core-service` exige en toda petición. Esto materializa, a nivel de código, el principio central del patrón: **el backend generalizado no sabe ni le importa qué frontend existe**; son los BFF quienes conocen a sus clientes.

## 4. Personalización de la información por canal (evidencia del criterio "Personaliza la información según las necesidades de cada frontend")

Los tres BFF consultan exactamente la misma fuente de verdad (`core-service`) pero devuelven respuestas radicalmente distintas para la misma cuenta:

| | **BFF Web** (`GET /api/web/cuentas/101`) | **BFF Móvil** (`GET /api/mobile/cuentas/101/resumen`) | **BFF Cajero** (`GET /api/atm/cuentas/101/saldo`) |
|---|---|---|---|
| Nombre del titular | Sí | No | No |
| Edad del titular | Sí | No | No |
| Saldo | Sí | Sí | Sí (único dato, además del propio endpoint) |
| Tasa de interés e interés proyectado | Sí | No | No |
| Totales agregados (depósitos, retiros, compras) | Sí | No | No |
| Historial de movimientos | Completo, filtrable por tipo/fecha | Solo los últimos 3 | Ninguno |
| Operaciones permitidas | Solo lectura | Solo lectura | Lectura de saldo **y retiro de efectivo** |
| Tamaño aproximado de la respuesta | El mayor de los tres (pensado para tablas y gráficos de escritorio) | Reducido a propósito (ahorra ancho de banda móvil) | El más pequeño posible (pantalla de cajero, mínima exposición de datos personales) |

Esta tabla no es una descripción de intenciones: es literalmente el comportamiento de `CuentaWebResponse`, `CuentaMobileResumenResponse` y `SaldoResponse` (ver sección 6). Cada clase documenta, en su propio javadoc, por qué omite o incluye cada campo.

## 5. Autenticación y autorización específicas por canal

| Canal | Credencial | Mecanismo | Duración de sesión | Razonamiento |
|---|---|---|---|---|
| **Web** | N.º de cuenta + nombre del titular | JWT firmado (HS256), claim con nombre | 30 minutos | Un usuario de escritorio puede dejar la sesión abierta mientras revisa varias pantallas; el riesgo de robo del dispositivo es menor que en un teléfono. |
| **Móvil** | N.º de cuenta + PIN de 4 dígitos | JWT firmado (HS256), sin nombre en el claim | 5 minutos | Un teléfono se pierde/roba con más frecuencia; se fuerza a renovar el token seguido. Un PIN es la credencial habitual de una app bancaria móvil. |
| **Cajero** | N.º de tarjeta + PIN (dos factores) | Token de sesión **opaco**, guardado en el servidor, invalidado automáticamente tras un retiro exitoso | 2 minutos, y de un solo uso para operaciones críticas | Es la operación de más riesgo (dinero en efectivo, en un espacio público); un token opaco permite revocación inmediata del lado del servidor, algo que un JWT autocontenido no ofrece sin infraestructura adicional (lista de revocación). |

> **Nota importante sobre las credenciales:** el dataset legacy del Banco XYZ no incluye contraseñas, PIN ni números de tarjeta reales. Para poder demostrar un flujo de autenticación end-to-end verificable, este proyecto genera credenciales **sintéticas y deterministas** a partir del `cuentaId` (ver `PinGenerator` y `TarjetaGenerator`), documentado explícitamente en el código como una simplificación académica. En un sistema real, estas credenciales se almacenarían hasheadas en un servicio de identidad dedicado, nunca derivadas matemáticamente.

## 6. Organización del código (evidencia del criterio "Organiza su código según la estrategia de implementación elegida")

La estructura de carpetas refleja directamente la estrategia elegida en la sección 2.1: **cuatro proyectos Maven independientes**, agregados solo por comodidad de build bajo un `pom.xml` raíz (`packaging=pom`), pero deployables por separado:

```
banco-xyz-bff/
├── pom.xml                        (agregador, NO es el padre de Spring Boot de los módulos)
├── core-service/                  (backend generalizado — NUNCA expuesto a un frontend)
│   └── src/main/java/com/bancoxyz/bff/core/
│       ├── model/                 Cuenta, Movimiento (dominio)
│       ├── repository/            CuentaRepository (repositorio en memoria)
│       ├── service/               CargaDatosService (carga y valida los CSV al iniciar)
│       ├── util/                  FechaFlexibleParser
│       ├── config/                InternalApiKeyFilter (exige X-Internal-Api-Key)
│       ├── controller/            CuentaInternalController (API interna, sin personalizar)
│       ├── dto/                   CuentaInternalDTO, MovimientoDTO, ActualizarSaldoRequest
│       └── exception/             manejo centralizado de errores
├── bff-web/                       (canal navegador — datos completos)
│   └── src/main/java/com/bancoxyz/bff/web/
│       ├── client/                CoreServiceClient + DTOs espejo de core-service
│       ├── security/              JwtService (30 min), JwtAuthFilter
│       ├── controller/            AuthController, CuentaWebController
│       ├── dto/                   CuentaWebResponse (respuesta rica y agregada)
│       └── exception/
├── bff-mobile/                    (canal app móvil — datos esenciales)
│   └── src/main/java/com/bancoxyz/bff/mobile/     (misma organización interna que bff-web)
│       ├── security/              JwtService (5 min), PinGenerator
│       └── dto/                   CuentaMobileResumenResponse (respuesta reducida)
├── bff-atm/                       (canal cajero — operaciones críticas)
│   └── src/main/java/com/bancoxyz/bff/atm/        (misma organización interna)
│       ├── security/              SesionAtmService (token opaco), TarjetaGenerator, PinGenerator
│       ├── controller/            SesionController, CuentaAtmController (saldo + retiro)
│       └── dto/                   SaldoResponse (la respuesta más reducida del proyecto)
├── scripts/probar_apis.sh         Prueba end-to-end de los 4 servicios con curl real
└── .github/workflows/evidencia-ejecucion.yml   Evidencia de ejecución real (ver sección 8)
```

Cada módulo repite deliberadamente el mismo esqueleto interno (`client/`, `security/`, `controller/`, `dto/`, `exception/`): esto no es duplicación accidental, es la consecuencia directa de la estrategia elegida — si `bff-mobile` tuviera una estructura completamente distinta a `bff-web`, sería una señal de que en realidad no se está tratando a cada canal como un backend independiente y autónomo, sino como variaciones ad-hoc de un mismo código base.

### 6.1 Por qué no se comparte código (un `common`) entre los tres BFF

Cada BFF define su propia copia de `CoreServiceProperties`, sus propios DTOs espejo (`CuentaCoreDTO`, `MovimientoCoreDTO`) y, en el caso de `bff-mobile`/`bff-atm`, su propia copia de `PinGenerator`. Esto es intencional: la estrategia de "backends independientes" busca que cada BFF pueda evolucionar, desplegarse y versionarse sin coordinar cambios con los demás. Introducir una librería compartida recrearía un acoplamiento oculto entre los tres canales — si un cambio en esa librería obligara a recompilar y redesplegar los tres BFF a la vez, ya no serían realmente independientes. El único acoplamiento real y deliberado del proyecto es el contrato HTTP/JSON de `core-service`, documentado y estable.

## 7. Cómo ejecutar el proyecto

### 7.1 Requisitos

- JDK 21
- Maven 3.9+ (o el wrapper, si se agrega)
- Un puerto libre 8080–8083

### 7.2 Compilar y ejecutar localmente

```bash
# Desde la raíz del proyecto, compila los 4 módulos:
mvn -DskipTests package

# En 4 terminales distintas (el orden importa: core-service primero):
java -jar core-service/target/core-service.jar
java -jar bff-web/target/bff-web.jar
java -jar bff-mobile/target/bff-mobile.jar
java -jar bff-atm/target/bff-atm.jar

# En una quinta terminal, una vez los 4 procesos estén arriba:
bash scripts/probar_apis.sh
```

### 7.3 Credenciales de demostración

Calculadas a partir del dataset real (`intereses.csv`, semana 3) tras la validación y el upsert que aplica `CargaDatosService` (idéntico criterio, documentado y verificado con evidencia real, al usado en el proyecto Exp1 de este curso):

| cuentaId | Titular | Tipo | Saldo | PIN (móvil/cajero) | N.º de tarjeta (cajero) |
|---|---|---|---|---|---|
| 101 | John Doe | préstamo | 10.000 | 7373 | 4915000000000101 |
| 105 | Steve Rogers | ahorro | 10.000 | 7665 | 4915000000000105 |
| 108 | Diana Prince | ahorro | 10.000 | 7884 | 4915000000000108 |
| 118 | Jane Smith | ahorro | 12.000 | 8614 | 4915000000000118 |

(Login web: `{"cuentaId": <id>, "nombre": "<Titular>"}`. Login móvil: `{"cuentaId": <id>, "pin": "<PIN>"}`. Sesión de cajero: `{"numeroTarjeta": "<tarjeta>", "pin": "<PIN>"}`.)

## 8. Evidencia de ejecución

La evidencia de ejecución real se genera con **GitHub Actions** (`.github/workflows/evidencia-ejecucion.yml`). El workflow:

1. Compila los 4 módulos con Maven.
2. Levanta `core-service`, `bff-web`, `bff-mobile` y `bff-atm` como procesos reales, en ese orden, esperando activamente a que cada uno responda antes de continuar.
3. Ejecuta `scripts/probar_apis.sh` contra los 4 servicios reales corriendo en el runner de GitHub.
4. Publica los logs de arranque de cada servicio y la salida completa de las pruebas como un artefacto descargable (`evidencias-ejecucion-bff`) en la pestaña **Actions** del repositorio, en la sección **Artifacts** del run correspondiente.

El script de pruebas (sección 7.2) ejercita, con datos reales, los cuatro flujos completos: rechazo de acceso directo a `core-service`, login y consulta completa en el canal web (incluyendo la verificación de que un token no puede acceder a la cuenta de otra persona), login y resumen reducido en el canal móvil, y el flujo completo de cajero (apertura de sesión, consulta de saldo, retiro exitoso, invalidación automática de la sesión tras el retiro, y rechazo de un retiro que excede el límite máximo por operación).

### 8.1 Evidencia de ejecución verificada (03-09-2026)

El workflow se ejecutó exitosamente en GitHub Actions, con los 4 módulos compilando sin errores (`BUILD SUCCESS`) y los 4 servicios arrancando correctamente sobre datos legacy reales (`core-service` cargó 49 cuentas y 885 movimientos válidos desde los CSV). El script de pruebas (`evidencia06-pruebas-apis.log`) confirmó, contra las APIs reales corriendo en el runner, todos los comportamientos esperados:

- `core-service` rechaza (HTTP 403) cualquier llamada sin el header `X-Internal-Api-Key`, y responde con el dominio completo cuando la clave es correcta — el backend generalizado nunca queda expuesto directamente a un frontend.
- **BFF Web**: login exitoso, consulta de cuenta completa con agregados (tasa de interés, interés proyectado, totales por tipo de movimiento) e historial filtrable por tipo de movimiento; un token válido de la cuenta 101 recibe HTTP 403 al intentar consultar la cuenta 105 (autorización por titularidad funcionando).
- **BFF Móvil**: login con PIN determinista y respuesta deliberadamente reducida (saldo, tipo de cuenta y solo los últimos 3 movimientos, sin nombre ni edad del titular), evidenciando la personalización por canal frente al payload completo del canal Web.
- **BFF Cajero**: apertura de sesión con tarjeta + PIN, consulta de saldo mínima (sin historial ni datos personales), retiro de $1.000 exitoso, invalidación automática de la sesión inmediatamente después del retiro (HTTP 401 al reutilizarla) y rechazo correcto de un retiro que excede el límite máximo por operación.

Los 6 archivos de log generados por este run (`evidencia01-build.log` a `evidencia06-pruebas-apis.log`) quedan como artefacto descargable del run correspondiente en la pestaña Actions del repositorio, y constituyen la evidencia de ejecución real exigida por la pauta para los 4 criterios de esta actividad.

## 9. Decisiones de diseño y simplificaciones (transparencia académica)

- **Persistencia en memoria, no una base de datos real.** El foco de esta actividad es el patrón BFF, no la capa de persistencia. `core-service` carga los CSV legacy en un repositorio en memoria al iniciar. Podría reemplazarse por PostgreSQL/JPA (como en el proyecto Exp1) sin que ningún BFF se entere: el contrato que consumen es la API HTTP de `core-service`, nunca su almacenamiento interno.
- **Consolidación de dos fuentes legacy en un solo dominio.** `intereses.csv` (maestro de cuentas) y `cuentas_anuales.csv` (historial de movimientos) eran, en el proyecto Exp1, dos procesos batch independientes sin relación directa entre sí. Para esta actividad se unifican bajo un mismo `cuentaId`, tal como exigiría un ejercicio real de modernización que consolida silos de datos legacy dispersos en un modelo de dominio único y consultable.
- **Credenciales sintéticas y deterministas**, ya documentadas en la sección 5, necesarias porque el dataset legacy no incluye contraseñas, PIN ni números de tarjeta.
- **Límite de reintentos/validación de datos al cargar CSV**: se reutiliza el mismo criterio de validación (tipo de cuenta soportado, edad 18–90, saldo no negativo, nombre no vacío/"Unknown") ya verificado con evidencia real en el proyecto Exp1, para no introducir criterios de calidad de datos nuevos y no probados.

## 10. Trazabilidad con la pauta de evaluación formativa

| Criterio de la pauta | Dónde se evidencia |
|---|---|
| Demuestra una comprensión clara del patrón BFF | Sección 2 (definición, origen del problema, comparación con monolito/API Gateway/microservicios) |
| Implementa una de las estrategias de BFF | Sección 2.1 (análisis explícito de las 3 estrategias y justificación de la elegida) + las 4 aplicaciones Spring Boot funcionando end-to-end (sección 8) |
| Personaliza la información según las necesidades de cada frontend | Sección 4 (tabla comparativa) + `CuentaWebResponse`, `CuentaMobileResumenResponse`, `SaldoResponse` |
| Organiza su código según la estrategia de implementación elegida de BFF | Sección 6 (estructura de módulos independientes, esqueleto interno replicado deliberadamente, ausencia intencional de código compartido) |
