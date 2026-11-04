package com.bartolomeo.clinics.booking.catalog.domain.model;

/**
 * Un profesional tal como lo muestra el servicio de catálogo.
 *
 * <p><strong>Es registro y no clase con setters</strong>, a diferencia de los modelos de
 * {@code user}: esto no se construye por partes ni se muta después de nacer. Lo crea el
 * adaptador de un JSON que viene de otro servicio y de ahí en más es una respuesta a una
 * pregunta, no un estado que alguien vaya a cambiar. Sin setters no hay forma de que un caso
 * de uso "arregle" un profesional que leyó.
 *
 * <p><strong>Los campos son los que este servicio consume, y no todos los que llegan.</strong>
 * La respuesta real trae también {@code createdAt} y {@code updatedAt}, y acá no están: booking
 * no decide nada con eso. Que el adaptador pase de largo esos campos no es un descuido, es el
 * requisito de tolerancia a campos nuevos, y el test de contrato lo comprueba con la respuesta
 * real del servicio.
 *
 * @param id         identificador del profesional en el catálogo
 * @param categoryId categoría a la que pertenece
 * @param firstName  nombre
 * @param lastName   apellido
 * @param enabled    si está habilitado. Un profesional deshabilitado no debería poder recibir
 *                   reservas, y la cátedra es quien lo rechaza si acá se llega a pasar de largo
 */
public record Professional(
        long id,
        long categoryId,
        String firstName,
        String lastName,
        boolean enabled) {
}
