package donatrack.donaciones.domain.notificacion;

import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.service.GestorNotificaciones;

// Observer concreto — notifica a la beneficiaria cuando queda confirmada como destinataria
public class NotificarBeneficiariaAsignacionObserver implements DonacionObserver {

    private static final String ESTADO_ASIGNACION = "ASIGNACION_REALIZADA";

    private final GestorNotificaciones gestor;

    public NotificarBeneficiariaAsignacionObserver(GestorNotificaciones gestor) {
        this.gestor = gestor;
    }

    @Override
    public void onCambioEstado(Donacion donacion, String estadoAnterior, String estadoNuevo) {
        if (!ESTADO_ASIGNACION.equals(estadoNuevo)) {
            return;
        }
        Beneficiaria destinatario = donacion.getDestinatarioAsignado();
        if (destinatario == null) {
            return;
        }
        String mensaje = "Se le asigno una donacion de ["
            + donacion.getSubcategoria().getNombre() + "].";
        gestor.notificar(destinatario.getPersona(), mensaje);
    }
}