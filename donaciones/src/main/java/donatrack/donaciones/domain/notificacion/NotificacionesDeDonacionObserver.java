package donatrack.donaciones.domain.notificacion;

import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.estado.EstadoDonacion;
import donatrack.donaciones.domain.donacion.estado.EstadoEntregaFallida;
import donatrack.donaciones.domain.donacion.estado.EstadosPosiblesDonacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.service.GestorNotificaciones;

import java.util.List;

// Observer unico: traduce cada cambio de estado de una Donacion al metodo
// correspondiente de GestorNotificaciones. Los links de seguimiento que
// arma INICIO_RUTA los recibe por constructor para no leer variables de
// entorno desde el dominio.
public class NotificacionesDeDonacionObserver implements DonacionObserver {

    private final GestorNotificaciones gestor;
    private final String linkSeguimientoDonante;
    private final String linkSeguimientoBeneficiario;

    public NotificacionesDeDonacionObserver(GestorNotificaciones gestor,
                                            String linkSeguimientoDonante,
                                            String linkSeguimientoBeneficiario) {
        this.gestor = gestor;
        this.linkSeguimientoDonante = linkSeguimientoDonante;
        this.linkSeguimientoBeneficiario = linkSeguimientoBeneficiario;
    }

    @Override
    public void onCambioEstado(Donacion donacion, String estadoAnterior, String estadoNuevo) {
        EstadosPosiblesDonacion nuevo;
        try {
            nuevo = EstadosPosiblesDonacion.valueOf(estadoNuevo);
        } catch (IllegalArgumentException e) {
            return;
        }

        Donante donante = donacion.getDonante();
        Beneficiaria destinataria = donacion.getDestinatarioAsignado();

        switch (nuevo) {
            case ASIGNACION_REALIZADA -> {
                if (donante != null) {
                    gestor.notificarDonacionAsignadaDonante(donante, donacion);
                }
                if (destinataria != null) {
                    gestor.notificarDonacionAsignadaBeneficiario(destinataria, donacion);
                }
            }
            case EN_TRASLADO -> {
                if (donante != null && linkSeguimientoDonante != null) {
                    gestor.notificarInicioRuta(donante, donacion, linkSeguimientoDonante);
                }
                if (destinataria != null && linkSeguimientoBeneficiario != null) {
                    gestor.notificarInicioRuta(destinataria, donacion, linkSeguimientoBeneficiario);
                }
            }
            case ENTREGADA -> {
                if (donante != null && destinataria != null) {
                    gestor.notificarEntregaExitosa(
                        donante, destinataria, donacion, donacion.generarComprobante());
                }
            }
            case ENTREGA_FALLIDA -> {
                if (donante != null && destinataria != null) {
                    gestor.notificarEntregaFallida(
                        donante, destinataria, donacion, motivoFallidaDe(donacion));
                }
            }
            case LISTA_PARA_ENTREGAR, EN_DEPOSITO, VENCIDA -> {
                if (donante != null) {
                    gestor.notificarCambioDeEstado(
                        donante, donacion, estadoAnterior, estadoNuevo);
                }
            }
            default -> { /* Nada que hacer para los estados no listados. */ }
        }
    }

    private static String motivoFallidaDe(Donacion donacion) {
        List<EstadoDonacion> historial = donacion.getHistorialEstados();
        if (historial.isEmpty()) {
            return null;
        }
        EstadoDonacion ultimo = historial.get(historial.size() - 1);
        if (ultimo instanceof EstadoEntregaFallida fallida) {
            return fallida.getJustificacion();
        }
        return null;
    }
}
