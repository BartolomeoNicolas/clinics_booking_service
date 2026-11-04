# clinics_booking_service
Sistema que maneja los turnos y reservas

## Stack

Java 25 · Spring Boot 4.1.1 · Gradle 9.6.0 · PostgreSQL 16 · Liquibase ·
spring-kafka · spring-data-redis · JPA/Hibernate · springdoc

## Arquitectura

Hexagonal, siguiendo el repo de referencia del profesor `dqmdz/hexagonal`.
**Cinco** bounded contexts:

- **`user`** — registro, login y emisión del JWT de sesión. Es el dueño de la identidad.
- **`availability`** — cálculo de slots disponibles.
- **`reservation`** — procesos de reserva y su máquina de estados.
- **`catalog`** — capa anti-corrupción sobre el servicio de catálogo. Es consumida por
  `availability` y por `reservation`, y por eso es contexto propio y no vive en uno de ellos.
- **`catedra`** — capa anti-corrupción sobre el servicio central de la cátedra: ocupaciones,
  retenes, confirmación, listado y cancelación. Mismo motivo que `catalog`.

```
com.bartolomeo.clinics.booking.<contexto>/
├── domain/            modelo + ports/{in,out}   ← sin Spring, sin JPA
├── application/       usecases/ · service/ · exception/
└── infrastructure/    persistence/ · web/ · messaging/ · catedra/ · security/
```

**Lo no obvio:** los ports `in` (interfaces de caso de uso) viven en `domain/ports/in`, no en
`application`. Es contraintuitivo y es la convención del profesor.

## Errores de la cátedra

La cátedra advierte que sus mensajes son texto libre y que **los clientes deben decidir por
`status` y `code`, nunca por el texto de `detail`**. Este servicio sigue esa regla y además la
aplica sobre sus propias respuestas: `CatedraProblemTranslator` recorre la matriz de la §13.1
de `INTEGRATION_REFERENCE-v2.md` y produce una excepción propia, con un `code` estable de
booking. Hay **un test por cada fila** (`CatedraProblemTranslatorTest`) y la verificación por
la red está en `CatedraAppointmentsContractTest`.

| `code` de la cátedra | HTTP | Excepción de booking | `code` de booking | HTTP |
| --- | --- | --- | --- | --- |
| `VALIDATION_ERROR` | 400 | `CatedraValidationException` | `validation-error` | 400 |
| `INVALID_DATE_RANGE` | 400 | `InvalidDateRangeException` | `validation-error` | 400 |
| `PROFESSIONAL_NOT_FOUND` | 404 | `ProfessionalNotFoundException` | `not-found` | 404 |
| `PROFESSIONAL_DISABLED` | 400 | `ProfessionalDisabledException` | `professional-disabled` | 400 |
| `INVALID_SLOT` | 400 | `InvalidSlotException` | `invalid-slot` | 400 |
| `SLOT_ALREADY_RESERVED` | 409 | `SlotAlreadyReservedException` | `slot-not-available` | 409 |
| `SLOT_ALREADY_HELD` | 409 | `SlotAlreadyHeldException` | `slot-not-available` | 409 |
| `HOLD_NOT_FOUND` | 404 | `HoldNotFoundException` | `not-found` | 404 |
| `HOLD_EXPIRED` | 404 | `HoldExpiredException` | `hold-expired` | 404 |
| `HOLD_OWNER_MISMATCH` | 403 | `HoldOwnerMismatchException` | `forbidden` | 403 |
| `EXTERNAL_PATIENT_MISMATCH` | 400 | `ExternalPatientMismatchException` | `external-patient-mismatch` | 400 |
| `RESERVATION_PROCESS_ALREADY_CONFIRMED` | 409 | `ReservationAlreadyConfirmedException` | `already-confirmed` | 409 |
| `RESERVATION_NOT_FOUND` | 404 | `ReservationNotFoundException` | `not-found` | 404 |
| `RESERVATION_OWNER_MISMATCH` | 403 | `ReservationOwnerMismatchException` | `forbidden` | 403 |
| `RESERVATION_NOT_CANCELLABLE` | 409 | `ReservationNotCancellableException` | `not-cancellable` | 409 |
| `STUDENT_INTEGRATION_NOT_FOUND` | 403 | `CatedraIntegrationMissingException` | `catedra-rejected` | 503 |
| sin `code` (rechazo de Spring Security) | 401 | `CatedraRejectedException` | `catedra-rejected` | 503 |
| sin `code` (rechazo de Spring Security) | 403 | `CatedraRejectedException` | `catedra-rejected` | 503 |
| sin `code` (infraestructura) | 503 | `CatedraUnavailableException` | `catedra-unavailable` | 503 |
| sin `code` (servidor) | 500 | `CatedraUnavailableException` | `catedra-unavailable` | 503 |
| código fuera de la matriz o cuerpo ilegible | cualquiera | `CatedraUnavailableException` | `catedra-unavailable` | 503 |

Dos decisiones que la tabla no muestra y conviene tener escritas:

- **Se responde 503, no 401 ni 403, cuando la falla es nuestra de credencial.** Un 401 le
  diría al usuario final que *él* no está autenticado —que es falso— y la app cerraría su
  sesión por un problema que no es suya.
- **Un código que no está en la matriz no se traduce adivinando.** Se cae en
  `catedra-unavailable`, con el status y el código en el mensaje: adivinar es peor, porque
  decirle a la app "ese turno no existe" cuando en realidad no se entendió la respuesta la
  haría mostrar algo falso.

### Filas que este servicio no traduce

| `code` | HTTP | Por qué no tiene traducción |
| --- | --- | --- |
| `USERNAME_ALREADY_EXISTS`, `EMAIL_ALREADY_EXISTS`, `STUDENT_GROUP_ALREADY_EXISTS`, `INVALID_GROUP_ID`, `INVALID_PASSWORD` | 400 | Pertenecen a `POST /api/student/register`. Booking no registra cuentas técnicas: no hay caso de uso que pueda recibirlos. |
| `STUDENT_INTEGRATION_NOT_FOUND` | 404 | Es la fila de `GET /api/student/integration`, que booking no llama. La misma fila con 403 —que sí le alcanza en listado, holds y cancelación— sí está traducida. |

## Requisitos

- JDK 25. En esta máquina: `~/Facultad/android-studio/jbr` (el Java del sistema es 21).
- PostgreSQL 16 (lo levanta `compose.yaml`, pendiente de M0-03).

## Cómo correrlo

```bash
export JAVA_HOME="$HOME/Facultad/android-studio/jbr"
./gradlew bootRun
curl localhost:8082/actuator/health
```

La documentación OpenAPI, cuando se abra la seguridad: `/swagger-ui.html`.

## Configuración

`src/main/resources/application.yml`, con perfiles `dev` y `test`.
**Los secretos no van en el repo**: se externalizan a `~/.config/clinics/secrets/` (M0-02).
Acá vive también el par de claves del JWT de sesión, nunca en el repositorio.

## Convenciones de commits

Los commits siguen [Conventional Commits](https://www.conventionalcommits.org/es/v1.0.0/):

```
tipo(ámbito): descripción
```

| Tipo | Para qué |
|---|---|
| `feat` | funcionalidad nueva |
| `fix` | corrección de defecto |
| `docs` | documentación |
| `test` | pruebas automatizadas |
| `refactor` | cambio sin alterar comportamiento |
| `chore` | tooling, configuración, dependencias |

**Ámbitos sugeridos en este repo**: booking, api, infra, docs, adrs

Ejemplos:

```
    feat(booking): crea el proceso de reserva antes del hold
    fix(booking): no retrocede un proceso en estado terminal
    feat(api)!: cambia el contrato de disponibilidad
```

Reglas adicionales:

- El tipo va en inglés (lo entienden las herramientas estándar); la descripción en español.
- La línea de asunto tiene máximo 72 caracteres y no termina en punto.
- Si un commit cierra una issue, se indica `Closes #N` en el cuerpo; si solo la menciona, `Refs #N`.
- Un `!` antes de los dos puntos (`feat(api)!:`) marca un cambio incompatible del contrato.
- Commits generados por Git (`Merge`, `Revert`, `squash!`, `fixup!`) se aceptan sin validar.

### Validación automática

El hook `scripts/git-hooks/commit-msg` rechaza los commits que no cumplen el formato.
Está versionado en este repo y se activa en cada clon con:

```bash
git config core.hooksPath scripts/git-hooks
```

Para saltearlo a propósito: `git commit --no-verify` (no se recomienda).
