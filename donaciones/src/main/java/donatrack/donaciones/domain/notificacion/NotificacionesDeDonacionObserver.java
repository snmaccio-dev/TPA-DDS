package donatrack.donaciones.domain.notificacion;

import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.estado.EstadoDonacion;
import donatrack.donaciones.domain.donacion.estado.EstadoEntregaFallida;
import donatrack.donaciones.domain.donacion.estado.EstadosPosiblesDonacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.service.NotificacionesService;

import java.util.List;

// Observer unico: traduce cada cambio de estado de una Donacion al metodo
// correspondiente de NotificacionesService. Los links de seguimiento que
// arma INICIO_RUTA los recibe por constructor para no leer variables de
// entorno desde el dominio.
public class NotificacionesDeDonacionObserver implements DonacionObserver {

    private final NotificacionesService service;
    private final String linkSeguimientoDonante;
    private final String linkSeguimientoBeneficiario;

    public NotificacionesDeDonacionObserver(NotificacionesService service,
                                            String linkSeguimientoDonante,
                                            String linkSeguimientoBeneficiario) {
        this.service = service;
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
                    service.notificarDonacionAsignadaDonante(donante, donacion);
                }
                if (destinataria != null) {
                    service.notificarDonacionAsignadaBeneficiario(destinataria, donacion);
                }
            }
            case EN_TRASLADO -> {
                if (donante != null && linkSeguimientoDonante != null) {
                    service.notificarInicioRuta(donante, donacion, linkSeguimientoDonante);
                }
                if (destinataria != null && linkSeguimientoBeneficiario != null) {
                    service.notificarInicioRuta(destinataria, donacion, linkSeguimientoBeneficiario);
                }
            }
            case ENTREGADA -> {
                if (donante != null && destinataria != null) {
                    service.notificarEntregaExitosa(
                        donante, destinataria, donacion, donacion.generarComprobante());
                }
            }
            case ENTREGA_FALLIDA -> {
                if (donante != null && destinataria != null) {
                    service.notificarEntregaFallida(
                        donante, destinataria, donacion, motivoFallidaDe(donacion));
                }
            }
            case LISTA_PARA_ENTREGAR, EN_DEPOSITO, VENCIDA -> {
                if (donante != null) {
                    service.notificarCambioDeEstado(
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
