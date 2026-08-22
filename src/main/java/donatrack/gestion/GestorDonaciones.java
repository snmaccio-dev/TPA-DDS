package donatrack.gestion;

import donatrack.model.donacion.CambioEstado;
import donatrack.model.donacion.Donacion;
import donatrack.notificacion.NotificadorDonacionObserver;
import donatrack.notificacion.NotificarBeneficiariaAsignacionObserver;
import donatrack.notificacion.Notificador;
import donatrack.repositorio.RepositorioDonaciones;

import java.util.List;

public class GestorDonaciones {

    private final SegmentadorDonaciones segmentacion = new SegmentadorDonaciones();
    private final RepositorioDonaciones repositorio =
        RepositorioDonaciones.getInstance();
    private final Notificador notificador;

    public GestorDonaciones(Notificador notificador) {
        this.notificador = notificador;
    }

    public List<Donacion> todas() {
        return repositorio.todas();
    }

    public Donacion buscar(long id) {
        return repositorio.buscarPorId(id)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "No existe la donación con ID " + id
                ));
    }

    public Donacion crear(Donacion donacion) {
        donacion.agregarObserver(
            new NotificadorDonacionObserver(donacion.getDonante(), notificador)
        );
        donacion.agregarObserver(
            new NotificarBeneficiariaAsignacionObserver(notificador)
        );
        repositorio.guardar(donacion);
        return donacion;
    }

    public void eliminar(long id) {
        repositorio.eliminar(id);
    }

    // === Transiciones del ciclo de la Donacion ===

    public void confirmarRecepcion(long id, java.util.List<String> fotos) {
        buscar(id).confirmarRecepcion(fotos);
    }

    public void marcarEntregaFallida(long id, String motivo) {
        buscar(id).marcarEntregaFallida(motivo);
    }

    public void marcarEnDeposito(long id) {
        buscar(id).marcarEnDeposito();
    }

    public void vencer(long id) {
        buscar(id).vencer();
    }

    public List<CambioEstado> historial(long id) {
        return buscar(id).getHistorialEstados();
    }
}
