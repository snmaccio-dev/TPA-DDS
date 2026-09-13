package donatrack.donaciones.service;

import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.estado.EstadoDonacion;
import donatrack.donaciones.domain.donacion.estado.EstadosPosiblesDonacion;
import donatrack.donaciones.domain.notificacion.NotificadorDonacionObserver;
import donatrack.donaciones.domain.notificacion.NotificarBeneficiariaAsignacionObserver;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioPropuestas;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GestorDonaciones {

    private final SegmentadorDonaciones segmentacion = new SegmentadorDonaciones();
    private final RepositorioDonaciones repositorio =
        RepositorioDonaciones.getInstance();
    private final GestorNotificaciones gestorNotificaciones;
    private final RepositorioPropuestas repositorioPropuestas;

    public GestorDonaciones(GestorNotificaciones gestorNotificaciones,
                            RepositorioPropuestas repositorioPropuestas) {
        this.gestorNotificaciones = gestorNotificaciones;
        this.repositorioPropuestas = repositorioPropuestas;
    }

    public List<Donacion> todas() {
        return repositorio.todas();
    }

    public List<Donacion> porEstado(EstadosPosiblesDonacion estado) {
        return repositorio.todas().stream()
            .filter(d -> d.getEstado().getEstado() == estado)
            .toList();
    }

    public Donacion buscar(long id) {
        return repositorio.buscarPorId(id)
            .orElseThrow(() ->
                new RecursoInexistenteException(
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
        repositorioPropuestas.eliminar(id);
    }

    public List<EstadoDonacion> historial(long id) {
        return buscar(id).getHistorialEstados();
    }

    // === Eventos en lote (todo o nada) ===

    // Recibe el estado destino que informa el evento y una accion a aplicar sobre las que
    // pueden transicionar. Clasifica: ya-aplicadas (mismo estado), aplicables, imposibles.
    // Si hay imposibles: IllegalStateException sin aplicar nada.
    // Si todas ya estaban aplicadas: EventoYaAplicadoException.
    // Si no, ejecuta la accion sobre las aplicables.
    public void aplicarEventoEnLote(List<Long> donacionIds,
                                    EstadosPosiblesDonacion estadoDestino,
                                    List<EstadosPosiblesDonacion> estadosOrigenValidos,
                                    Consumer<Donacion> transicion) {
        List<Donacion> donaciones = donacionIds.stream()
            .map(this::buscar)
            .toList();

        List<Donacion> aAplicar = new ArrayList<>();
        List<Long> imposibles = new ArrayList<>();
        int yaAplicadas = 0;

        for (Donacion donacion : donaciones) {
            EstadosPosiblesDonacion actual = donacion.getEstado().getEstado();
            if (actual == estadoDestino) {
                yaAplicadas++;
            } else if (estadosOrigenValidos.contains(actual)) {
                aAplicar.add(donacion);
            } else {
                imposibles.add(donacion.getId());
            }
        }

        if (!imposibles.isEmpty()) {
            throw new IllegalStateException(
                "Las donaciones " + imposibles
                    + " no pueden transicionar a " + estadoDestino + "."
            );
        }
        if (aAplicar.isEmpty() && yaAplicadas == donaciones.size()) {
            throw new EventoYaAplicadoException(
                "Todas las donaciones ya estaban en estado " + estadoDestino + "."
            );
        }

        aAplicar.forEach(transicion);
    }
}