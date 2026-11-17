# ADR-004 · El catálogo no se replica en este servicio

**Estado**: decidido. Es una prohibición del enunciado (§4.2), no una preferencia.

## Contexto

El servicio de turnos necesita saber qué profesionales hay, sus categorías y sus horarios
semanales para calcular la disponibilidad. El servicio de catálogo tiene esa información
—es una copia local de la cátedra— y podría exponerla.

## Decisión

El contexto `catalog` de este servicio es **una capa anticorrupción**, no un caché: lee del
servicio de catálogo por REST y no guarda nada. El `CatalogContextStructureTest` recorre el
paquete `catalog` y **falla si aparece una entidad JPA o un repository de Spring Data ahí**:
la prohibición es ejecutable, no un javadoc.

Lo que sí se guarda en este servicio es lo que es de la reserva: el proceso de reserva y la
reserva confirmada, con los datos descriptivos que la cátedra tuvo en ese momento.

## Consecuencias

- La disponibilidad depende de que el servicio de catálogo esté vivo. Si no lo está, no hay
  slots que ofrecer: es una dependencia real y no un detalle de performance.
- Los datos descriptivos que vienen en un evento (`professionalFirstName`) son **historia**,
  no el catálogo vigente: si un profesional cambia de nombre, lo que diga una reserva de hace
  tres meses no lo corrige. Por eso no se usan para mostrar disponibilidad.
- La prohibición se puede auditar con una búsqueda de texto: `git grep -l "@Entity" .../catalog`
  no devuelve nada.
- La clave ajena del catálogo no se toca en ningún sentido: este servicio no inserta ni
  actualiza nada del lado de la cátedra.
