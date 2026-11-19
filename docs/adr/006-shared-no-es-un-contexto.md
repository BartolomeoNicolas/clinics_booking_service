# ADR-006 · `shared/` no es un bounded context

**Estado**: decidido.

## Contexto

Cada servicio tiene contextos acotados —en booking: `user`, `catalog`, `availability`,
`reservation` y `catedra`— y hay cosas que no son de ninguno: la configuración de
seguridad, el manejador de errores, el reloj, el validador de la audiencia del token.

La tentación es meterlas en un contexto cualquiera ("que sean cosas del usuario" ✗). Pero
eso crea una flecha que no existe: `SecurityConfig` produce el `JwtDecoder` que usa
`SecurityConfig` y el `JwtEncoder` que usa el emisor de `user`, así que `shared` dependería
de `user`, y al revés tampoco ✗.

## Decisión

`shared/` **no es un contexto acotado**: agrupa lo que es uno solo para la aplicación y lo
comparte. La regla de corte está escrita en su `package-info`: lo que dos contextos usan
igual y no pertenece a ninguno va en `shared`.

Lo que es de un contexto va en el contexto, aunque lo use otro: `BCryptPasswordEncoderAdapter`
y `JwtTokenIssuerAdapter` implementan puertos de `user`, así que viven en `user/`.

## Consecuencias

- `shared` no tiene modelo de dominio ni casos de uso: es infraestructura y excepciones de
  aplicación. Si alguna vez tuviera un modelo, es que en realidad es un contexto.
- La dependencia entre contextos va siempre en un sentido: `reservation` usa `catalog` y
  `catedra`, y ninguno de los dos usa a `reservation`. Lo que rompe esa flecha es el
  `catalog` del servicio de sincronización, que es una capa anticorrupción sobre otro
  servicio y por eso es un contexto y no una carpeta de infraestructura.
- El refactor que hizo esto movió 19 archivos en el servicio de sincronización y 29 en el de
  turnos, y quedó en commits separados del trabajo de cada milestone: se puede revisar sin
  mezclarlo con otra cosa.
