package donatrack.donaciones.service;

import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.estado.EstadoDonacion;
import donatrack.donaciones.domain.notificacion.NotificadorDonacionObserver;
import donatrack.donaciones.domain.notificacion.NotificarBeneficiariaAsignacionObserver;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.repository.RepositorioDonaciones;

import java.util.List;

public class GestorDonaciones {

    private final SegmentadorDonaciones segmentacion = new SegmentadorDonaciones();
    private final RepositorioDonaciones repositorio =
        RepositorioDonaciones.getInstance();
    private final GestorNotificaciones gestorNotificaciones;

    public GestorDonaciones(GestorNotificaciones gestorNotificaciones) {
        this.gestorNotificaciones = gestorNotificaciones;
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

    public List<Donacion> crear(List<Bien> bienes, Donante donante, String descripcion) {
        List<Donacion> segmentadas = segmentacion.segmentar(bienes, donante, descripcion);
        segmentadas.forEach(this::registrarDonacion);
        return segmentadas;
    }

    private void registrarDonacion(Donacion donacion) {
        donacion.agregarObserver(
            new NotificadorDonacionObserver(donacion.getDonante(), gestorNotificaciones)
        );
        donacion.agregarObserver(
            new NotificarBeneficiariaAsignacionObserver(gestorNotificaciones)
        );
        donacion.getDonante().registrarDonacion(donacion);
        repositorio.guardar(donacion);
    }

    public void eliminar(long id) {
        repositorio.eliminar(id);
    }

    // === Transiciones del ciclo de la Donacion ===

    public void confirmarDestino(long id, Beneficiaria destinatario) {
        buscar(id).confirmarDestino(destinatario);
    }

    public void marcarListaParaEntregar(long id) {
        buscar(id).marcarListaParaEntregar();
    }

    public void marcarEnTraslado(long id) {
        buscar(id).marcarEnTraslado();
    }

    public void marcarEntregada(long id, java.time.LocalDateTime fechaHora, String patenteCamion) {
        buscar(id).marcarEntregada(fechaHora, patenteCamion);
    }

    public void marcarEntregaFallida(long id, String motivo) {
        buscar(id).marcarEntregaFallida(motivo);
    }

    public void marcarEnDeposito(long id) {
        buscar(id).marcarEnDeposito();
    }

    public void marcarVencida(long id) {
        buscar(id).marcarVencida();
    }

    public List<EstadoDonacion> historial(long id) {
        return buscar(id).getHistorialEstados();
    }
}
