package donatrack.gestion;

import donatrack.model.donacion.CambioEstado;
import donatrack.model.donacion.Donacion;
import donatrack.notificacion.Notificador;
import donatrack.notificacion.NotificadorWhatsApp;
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


    // GET /donaciones
    public List<Donacion> todas() {
        return repositorio.todas();
    }

    // GET /donaciones/{id}
    public Donacion buscar(long id) {
        return repositorio.buscarPorId(id)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "No existe la donación con ID " + id
                ));
    }

    // POST /donaciones
    public Donacion crear(Donacion donacion) {
        repositorio.guardar(donacion);
        return donacion;
    }

    // DELETE /donaciones/{id}
    public void eliminar(long id) {
        repositorio.eliminar(id);
    }

    // PATCH /donaciones/{id}/estado
    public void asignar(long id) {
        Donacion donacion = buscar(id);
        donacion.asignar();
    }

    public void planificarRuta(long id) {
        Donacion donacion = buscar(id);
        donacion.planificarRuta();
    }

    public void iniciarTraslado(long id) {
        Donacion donacion = buscar(id);
        donacion.iniciarTraslado();
    }

    public void confirmarEntrega(long id) {
        Donacion donacion = buscar(id);
        donacion.confirmarEntrega();
    }

    public void fallarEntrega(long id, String justificacion) {
        Donacion donacion = buscar(id);
        donacion.fallarEntrega(justificacion);
    }

    public void vencer(long id) {
        Donacion donacion = buscar(id);
        donacion.vencer();
    }

    // GET /donaciones/{id}/historial
    public List<CambioEstado> historial(long id) {
        Donacion donacion = buscar(id);
        return donacion.getHistorialEstados();
    }
}
