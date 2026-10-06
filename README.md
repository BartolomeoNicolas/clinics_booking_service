# clinics_booking_service
Sistema que maneja los turnos y reservas

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
