package donatrack.donaciones.service;

import donatrack.donaciones.domain.TransicionNoPermitidaException;
import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.estado.EstadoDonacion;
import donatrack.donaciones.domain.donacion.estado.EstadosPosiblesDonacion;
import donatrack.donaciones.domain.notificacion.NotificacionesDeDonacionObserver;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import donatrack.donaciones.repository.RepositorioPropuestas;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GestorDonaciones implements WithSimplePersistenceUnit {

    private final SegmentadorDonaciones segmentacion = new SegmentadorDonaciones();
    private final RepositorioDonaciones repositorio =
        RepositorioDonaciones.getInstance();
    private final GestorNotificaciones gestorNotificaciones;
    private final RepositorioPropuestas repositorioPropuestas;
    private final RepositorioNotificaciones repositorioNotificaciones;
    // Links a la vista de seguimiento del web; se usan al armar la notificacion
    // INICIO_RUTA y los recibimos ya armados desde DonacionesApp para no leer
    // variables de entorno desde aca.
    private final String linkSeguimientoDonante;
    private final String linkSeguimientoBeneficiario;

    public GestorDonaciones(GestorNotificaciones gestorNotificaciones,
                            RepositorioPropuestas repositorioPropuestas,
                            RepositorioNotificaciones repositorioNotificaciones,
                            String linkSeguimientoDonante,
                            String linkSeguimientoBeneficiario) {
        this.gestorNotificaciones = gestorNotificaciones;
        this.repositorioPropuestas = repositorioPropuestas;
        this.repositorioNotificaciones = repositorioNotificaciones;
        this.linkSeguimientoDonante = linkSeguimientoDonante;
        this.linkSeguimientoBeneficiario = linkSeguimientoBeneficiario;
    }

    // Agrega el observer unico a una donacion. Es idempotente del lado de
    // Donacion.agregarObserver, asi que podemos llamar siempre sin chequear.
    public Donacion conObservers(Donacion donacion) {
        if (donacion == null) {
            return null;
        }
        donacion.agregarObserver(new NotificacionesDeDonacionObserver(
            gestorNotificaciones, linkSeguimientoDonante, linkSeguimientoBeneficiario));
        return donacion;
    }

    public List<Donacion> todas() {
        List<Donacion> lista = repositorio.todas();
        lista.forEach(this::conObservers);
        return lista;
    }

    public List<Donacion> porEstado(EstadosPosiblesDonacion estado) {
        List<Donacion> lista = repositorio.enEstado(estado);
        lista.forEach(this::conObservers);
        return lista;
    }

    public List<Donacion> buscar(FiltroDonaciones filtro,
                                 Donante donanteOpcional,
                                 Beneficiaria beneficiariaOpcional) {
        List<Donacion> lista = repositorio.buscar(filtro, donanteOpcional, beneficiariaOpcional);
        lista.forEach(this::conObservers);
        return lista;
    }

    public Donacion buscar(long id) {
        Donacion donacion = repositorio.buscarPorId(id)
            .orElseThrow(() ->
                new RecursoInexistenteException(
                    "No existe la donación con ID " + id
                ));
        return conObservers(donacion);
    }

    public List<Donacion> crear(List<Bien> bienes, Donante donante, String descripcion) {
        List<Donacion> segmentadas = segmentacion.segmentar(bienes, donante, descripcion);
        withTransaction(() -> segmentadas.forEach(this::registrarDonacion));
        return segmentadas;
    }

    private void registrarDonacion(Donacion donacion) {
        conObservers(donacion);
        donacion.getDonante().registrarDonacion(donacion);
        repositorio.guardar(donacion);
    }

    // Borrado en cascada de una donacion: primero sacamos sus notificaciones
    // y la propuesta de asignacion (si quedo alguna). Los bienes y el historial
    // de estados caen por cascade ALL del mapeo de Donacion.
    public void eliminar(long id) {
        Donacion donacion = buscar(id);
        withTransaction(() -> {
            repositorioNotificaciones.eliminarDeDonacion(donacion);
            repositorioPropuestas.eliminar(id);
            repositorio.eliminar(id);
        });
    }

    // === Transiciones del ciclo de la Donacion ===

    public void marcarListaParaEntregar(long id) {
        withTransaction(() -> buscar(id).marcarListaParaEntregar());
    }

    public void marcarEnTraslado(long id) {
        withTransaction(() -> buscar(id).marcarEnTraslado());
    }

    public void marcarEntregada(long id, java.time.LocalDateTime fechaHora, String patenteCamion) {
        withTransaction(() -> buscar(id).marcarEntregada(fechaHora, patenteCamion));
    }

    public void marcarEntregaFallida(long id, String motivo) {
        withTransaction(() -> buscar(id).marcarEntregaFallida(motivo));
    }

    public void marcarEnDeposito(long id) {
        withTransaction(() -> buscar(id).marcarEnDeposito());
    }

    public void marcarVencida(long id) {
        withTransaction(() -> {
            buscar(id).marcarVencida();
            repositorioPropuestas.eliminar(id);
        });
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
        // buscar(...) ya le enchufa el observer a cada donacion recuperada.
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
            throw new TransicionNoPermitidaException(
                "Las donaciones " + imposibles
                    + " no pueden transicionar a " + estadoDestino + "."
            );
        }
        if (aAplicar.isEmpty() && yaAplicadas == donaciones.size()) {
            throw new EventoYaAplicadoException(
                "Todas las donaciones ya estaban en estado " + estadoDestino + "."
            );
        }

        withTransaction(() -> aAplicar.forEach(transicion));
    }
}
