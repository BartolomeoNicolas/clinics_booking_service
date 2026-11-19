# ADR-008 · Un timeout no es un fallo: se reconcilia antes de repetir

**Estado**: decidido.

## Contexto

La cátedra es un servicio aparte, y una llamada a un servicio aparte se pierde: el timeout
no dice si el otro lado aplicó el efecto o no. Repetir a ciegas una escritura con efectos
puede dejar dos efectos donde debía haber uno: dos retenes, dos reservas, dos cancelaciones.

El enunciado (§8) y el anexo (§18.4) son explícitos: antes de repetir una acción con
efectos hay que consultar el estado por los identificadores ya persistidos.

## Decisión

Ninguna escritura se repite a ciegas. Cuando una llamada se pierde, se consulta el estado
por los identificadores que ya están guardados y se decide:

- **El retén**: se consultan las ocupaciones del turno. Si el turno aparece tomado, el retén
  **sí** se creó y no se puede repetir —crearía otro— ni confirmar, porque su `holdId` se
  perdió. No se repite y el proceso se cierra esperando que el retén venza.
- **La confirmación**: se consulta el listado de reservas del paciente. Si ya hay una reserva
  con nuestro proceso, la confirmación **sí** pasó.

Además hay un **barrido** que corre al arrancar y cada tanto: compara los procesos que no
cerraron con el estado real de la cátedra y los deja donde están. Es lo que hace que, tras
un reinicio con operaciones pendientes, el servicio converja con la verdad.

## Consecuencias

- El `Idempotency-Key` no alcanza para todo: garantiza que no haya dos procesos, no que
  haya un solo efecto en la cátedra. Las dos cosas hacen falta y se resuelven en lugares
  distintos.
- El barrido **no fuerza transiciones**: si un evento no aplica desde el estado en que
  está el proceso, se salta y se sigue con el siguiente del camino. Lo que todavía puede
  pasar no se toca: un proceso que solo llegó a tomar el retén puede seguir esperando,
  porque la cátedra no lista los retenes y que no aparezca no prueba nada.
- Lo que sí se cierra es el proceso que esperaba una reserva, porque esa tenía que existir.
  Cerrar de más dejaría al usuario sin medio de reclamar su turno.
- Un fallo de la cátedra no tumba el servicio ni el barrido: se anota y se sigue. Un proceso
  sin reconciliar se reconcilia en el siguiente barrido.
