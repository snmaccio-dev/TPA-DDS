package donatrack.notificacion;

import donatrack.model.donacion.Donacion;
import donatrack.model.persona.Beneficiaria;

// Observer concreto — notifica a la beneficiaria cuando queda confirmada como destinataria
public class NotificarBeneficiariaAsignacionObserver implements DonacionObserver {

    private static final String ESTADO_ASIGNACION = "ASIGNACION_REALIZADA";

    private final Notificador notificador;

    public NotificarBeneficiariaAsignacionObserver(Notificador notificador) {
        this.notificador = notificador;
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
        notificador.notificar(destinatario.getPersona().getRazonSocial(), mensaje);
    }
}
