# ADRs

Las decisiones que explican por qué el servicio está como está. Cada una tiene contexto,
decisión y consecuencias, para que se pueda leer sin el código al lado.

| # | Decisión | Estado |
|---|---|---|
| [001](001-group-id.md) | El `groupId` de la cuenta técnica | propuesto, pendiente de confirmar la convención |
| [002](002-emisor-del-jwt.md) | El emisor del JWT de sesión es el servicio de turnos | decidido |
| [003](003-problem-json.md) | Los errores se traducen con `@RestControllerAdvice` y `problem+json` | decidido (desviación del repo de referencia) |
| [004](004-el-catalogo-no-se-replica.md) | El catálogo no se replica en este servicio | decidido (prohibición del enunciado) |
| [005](005-el-listener-no-pasa-por-la-fachada.md) | El consumidor de Kafka invoca el port `in` directo | decidido (desviación del repo de referencia) |
| [006](006-shared-no-es-un-contexto.md) | `shared/` no es un bounded context | decidido |
| [007](007-el-proceso-se-persiste-antes-de-llamar.md) | El proceso se persiste antes de la primera llamada a la cátedra | decidido |
| [008](008-se-reconcilia-antes-de-repetir.md) | Un timeout no es un fallo: se reconcilia antes de repetir | decidido |

Las desviaciones del repo de referencia del profesor (003 y 005) están justificadas por el
contrato con la cátedra: el contrato manda sobre la convención cuando chocan.
