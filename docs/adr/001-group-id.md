# ADR-001 · El `groupId` de la cuenta técnica

**Estado**: decidido. **Confirmado con el profesor: `nicobartok0-prog2`.**

## Contexto

El registro de la cuenta técnica (`POST /api/student/register`) pide el `groupId` y es
obligatorio: el contrato (§2.1 y §5.1) no lo genera el servidor. El mismo `groupId` aparece
después en los topics de Kafka (`alumnos.turnos.acciones.{groupId}`,
`catedra.turnos.telefono.{groupId}`) y en los namespaces de Redis (`alumnos:{groupId}:*`).

El registro es de una sola vez: no hay forma de comprobar si un nombre está libre sin
crearlo, y equivocarse deja la cuenta apuntando al namespace de otro.

## Decisión

Usamos **`nicobartok0-prog2`**: el nombre del usuario del curso, con el sufijo `-prog2`
para que normalice a sí mismo y no dependa de cómo se escriba. El profesor lo confirmó.

Es un valor configurable por entorno (`CATEDRA_GROUP_ID`) y no está escrito en el código: lo
que cambia de un entorno a otro es la configuración, no el binario.

## Consecuencias

- La convención queda confirmada y el registro de la cuenta es de una sola vez: no hay
  margen para corregir un `groupId` mal puesto.
- Los topics y los namespaces se derivan del `groupId`, así que un cambio de valor obliga a
  cambiar los tres a la vez. Por eso están juntos en la configuración y no repartidos.
- Si la cuenta se creara con un `groupId` equivocado, no hay arreglo local: hay que pedir a
  la cátedra que lo cambie. Por eso la decisión se toma antes de registrar la cuenta, no
  después.
