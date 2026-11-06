package donatrack.donaciones.controller.dto;

import donatrack.donaciones.contrato.Fechas;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.notificacion.Notificacion;

import java.util.List;

public record NotificacionDTO(
    Long id,
    String tipo,
    String mensaje,
    String fechaHora,
    boolean leida,
    Long donacionId,
    String canal,
    String estadoEnvio
) {

    public static NotificacionDTO desde(Notificacion n) {
        Donacion donacion = n.getDonacion();
        return new NotificacionDTO(
            n.getId(),
            n.getTipo().name(),
            n.getMensaje(),
            Fechas.aTexto(n.getFechaHoraCreacion()),
            n.estaLeida(),
            donacion == null ? null : donacion.getId(),
            n.getCanal() == null ? null : n.getCanal().name(),
            n.getEstadoEnvio().name()
        );
    }

    public static List<NotificacionDTO> desde(List<Notificacion> notificaciones) {
        return notificaciones.stream().map(NotificacionDTO::desde).toList();
    }
}
