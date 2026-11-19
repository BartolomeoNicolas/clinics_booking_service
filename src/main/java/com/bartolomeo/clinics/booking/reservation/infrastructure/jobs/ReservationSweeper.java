package com.bartolomeo.clinics.booking.reservation.infrastructure.jobs;

import com.bartolomeo.clinics.booking.reservation.domain.ports.in.ReconcileReservationsUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * El barrido que reconcilia los procesos pendientes con la cátedra.
 *
 * <p><strong>Corre al arrancar y cada tanto.</strong> Al arrancar es lo que cierra el
 * círculo del enunciado §8: si el servicio se apagó con procesos a medio hacer, al volver
 * los compara con el estado real de la cátedra y los deja donde tienen que estar. Después
 * sigue corriendo, porque los procesos vencen con el tiempo y no solo con los reinicios.
 *
 * <p><strong>Un barrido que falla no tumba el servicio.</strong> Si la cátedra no responde,
 * el barrido se anota y se sigue: el servicio tiene que seguir atendiendo las consultas de
 * los usuarios aunque la cátedra esté caída, y un proceso sin reconciliar se reconciliará en
 * el próximo barrido.
 *
 * <p><strong>No hay dos barridos a la vez.</strong> El planificador de Spring llama a un
 * método programado desde un solo hilo por tarea, así que el barrido periódico no se pisa
 * consigo mismo.
 */
@Component
@RequiredArgsConstructor
public class ReservationSweeper {

    private static final Logger log = LoggerFactory.getLogger(ReservationSweeper.class);

    private final ReconcileReservationsUseCase reconciliar;

    /**
     * Al arrancar: el servicio vuelve y hay procesos que se quedaron a medias.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        barrer("arranque");
    }

    /**
     * Cada tanto, porque los procesos vencen con el tiempo y no solo con los reinicios.
     */
    @Scheduled(fixedDelayString = "${clinics.reservation.sweep-interval-ms:60000}")
    public void barrerPeriodicamente() {
        barrer("periódico");
    }

    private void barrer(String motivo) {
        try {
            int convergidos = reconciliar.reconcilePending();
            if (convergidos > 0) {
                log.info("Barrido {}: {} procesos convergieron con el estado real de la cátedra",
                        motivo, convergidos);
            }
        } catch (RuntimeException ex) {
            log.warn("El barrido {} no pudo completarse: {}", motivo, ex.getMessage());
        }
    }
}
