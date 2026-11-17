# ADR-005 · El consumidor de Kafka invoca el port `in` directo

**Estado**: decidido. Es la segunda desviación del repo de referencia, también deliberada.

## Contexto

En el repo de referencia, todo pasa por la fachada: los controllers la inyectan y los
consumidores de mensajes también. La regla que el profesor escribe es "todo pasa por la
fachada".

Pero una fachada de servicio tiene sentido para lo que un cliente **pide**: listar, buscar,
crear. Un listener de Kafka no pide nada: reacciona a algo que ya pasó. Y una fachada que
mezclara las dos cosas tendría un método para "aplicar un evento" que no es una operación
de servicio sino una reacción.

## Decisión

El listener de eventos invoca **el port `in` directamente**, sin pasar por la fachada. El
resto —los controllers— sigue pasando por la fachada, como manda el repo de referencia.

La diferencia está marcada en el propio código: `ProcessReservationEventUseCase` es un port
`in` del contexto `reservation` y el listener lo usa igual que lo usaría un controller.

## Consecuencias

- El port `in` que consume el listener no devuelve nada: es una reacción, no una consulta.
  La idempotencia (el `eventId` ya procesado) vive en ese port, no en el listener.
- El listener queda con una sola responsabilidad: entender el mensaje, traducirlo al idioma
  del dominio y delegar. Lo que no puede entender (un mensaje ilegible, una versión de
  contrato desconocida) lo registra como irrecuperable y lo confirma; lo que sí puede
  fallar más tarde (una base caída) lo deja pasar hacia el contenedor, que reintenta con
  un tope, **sin confirmar el offset**: el mensaje vuelve a llegar cuando el problema se haya
  resuelto.
- La regla del profesor no se cumple al pie de la letra, y está escrito por qué. Es la misma
  forma de desviarse que en el ADR-003: cuando el contrato o la forma del problema manda más
  que la convención, se documenta la desviación y el motivo.
