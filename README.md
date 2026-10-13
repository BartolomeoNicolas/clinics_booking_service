# clinics_booking_service
Sistema que maneja los turnos y reservas

## Stack

Java 25 · Spring Boot 4.1.1 · Gradle 9.6.0 · PostgreSQL 16 · Liquibase ·
spring-kafka · spring-data-redis · JPA/Hibernate · springdoc

## Arquitectura

Hexagonal, siguiendo el repo de referencia del profesor `dqmdz/hexagonal`.
**Tres** bounded contexts:

- **`user`** — registro, login y emisión del JWT de sesión. Es el dueño de la identidad.
- **`availability`** — cálculo de slots disponibles.
- **`reservation`** — procesos de reserva y su máquina de estados.

```
com.bartolomeo.clinics.booking.<contexto>/
├── domain/            modelo + ports/{in,out}   ← sin Spring, sin JPA
├── application/       usecases/ · service/ · exception/
└── infrastructure/    persistence/ · web/ · messaging/ · catedra/ · security/
```

**Lo no obvio:** los ports `in` (interfaces de caso de uso) viven en `domain/ports/in`, no en
`application`. Es contraintuitivo y es la convención del profesor.

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
