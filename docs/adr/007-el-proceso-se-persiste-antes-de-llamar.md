# ADR-007 · El proceso se persiste antes de la primera llamada a la cátedra

**Estado**: decidido.

## Contexto

Iniciar una reserva son dos escrituras contra la cátedra —tomar el retén y confirmarlo— y
un proceso local que las sobrevive. La tentación es hacer las llamadas y guardar el proceso
al final, que es lo que se escribe primero sin pensarlo.

## Decisión

El proceso se guarda **antes** de la primera llamada, en estado `INITIATED`, y después se
vaactualizando. Lo que no se puede es llamar primero y guardar después.

Además, la idempotencia va por **clave de solicitud** (`Idempotency-Key`), con un índice
único en la base por `(usuario, clave)`. Si la misma solicitud llega dos veces, la segunda
retoma el proceso que ya existe en vez de crear otro.

## Consecuencias

- Un timeout no deja nada atrás: queda un proceso con el turno, el usuario y la clave, que
  es exactamente lo que hace falta para reconciliar (anexo §18.4).
- El índice único es la garantía real de que un reintento no cree dos procesos: el código
  busca y la base frena. Si dos peticiones llegan a la vez, la segunda choca con el índice y
  lo que hace es devolver el proceso de la primera.
- La clave de solicitud es **obligatoria**. Sin ella no hay forma de distinguir un reintento
  de una reserva nueva, y el peor resultado posible —crear dos reservas para lo mismo— nace
  justamente de adivinar. Pedirla es más honesto que deducirla del turno: un usuario puede
  querer dos reservas para el mismo profesional y el mismo día, en horarios distintos, y eso
  solo se distingue por la clave.
- Un proceso guardado que la cátedra nunca vio se cierra solo: el barrido de reconciliación
  (ADR-008) lo detecta y lo deja vencido, sin dejar procesos imposibles.
