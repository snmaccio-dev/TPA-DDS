package donatrack.donaciones.service;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.TransicionNoPermitidaException;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.CondicionBien;
import donatrack.donaciones.domain.donacion.DatosModificacionBien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.SegmentadorDonaciones;
import donatrack.donaciones.domain.donacion.estado.EstadoDonacion;
import donatrack.donaciones.domain.donacion.estado.EstadosPosiblesDonacion;
import donatrack.donaciones.domain.notificacion.NotificacionesDeDonacionObserver;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import donatrack.donaciones.repository.RepositorioPropuestas;
import donatrack.donaciones.repository.RepositorioSubcategorias;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DonacionesService implements WithSimplePersistenceUnit {

    private final RepositorioDonaciones repositorio;
    private final RepositorioSubcategorias repositorioSubcategorias;
    private final RepositorioPropuestas repositorioPropuestas;
    private final RepositorioNotificaciones repositorioNotificaciones;
    private final SegmentadorDonaciones segmentador;
    private final NotificacionesService notificacionesService;
    private final String linkSeguimientoDonante;
    private final String linkSeguimientoBeneficiario;

    public DonacionesService(RepositorioDonaciones repositorio,
                             RepositorioSubcategorias repositorioSubcategorias,
                             RepositorioPropuestas repositorioPropuestas,
                             RepositorioNotificaciones repositorioNotificaciones,
                             SegmentadorDonaciones segmentador,
                             NotificacionesService notificacionesService,
                             String linkSeguimientoDonante,
                             String linkSeguimientoBeneficiario) {
        this.repositorio = repositorio;
        this.repositorioSubcategorias = repositorioSubcategorias;
        this.repositorioPropuestas = repositorioPropuestas;
        this.repositorioNotificaciones = repositorioNotificaciones;
        this.segmentador = segmentador;
        this.notificacionesService = notificacionesService;
        this.linkSeguimientoDonante = linkSeguimientoDonante;
        this.linkSeguimientoBeneficiario = linkSeguimientoBeneficiario;
    }

    public Donacion conObservers(Donacion donacion) {
        if (donacion == null) {
            return null;
        }
        donacion.agregarObserver(new NotificacionesDeDonacionObserver(
            notificacionesService, linkSeguimientoDonante, linkSeguimientoBeneficiario));
        return donacion;
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

    public List<Subcategoria> subcategorias() {
        return repositorioSubcategorias.todas();
    }

    public Subcategoria subcategoriaPorNombre(String nombre) {
        return repositorioSubcategorias.buscarPorNombre(nombre)
            .orElseThrow(() -> new RecursoInexistenteException(
                "No existe la subcategoría '" + nombre + "'."
            ));
    }

    public List<Donacion> crear(Donante donante,
                                List<DatosBien> bienes,
                                String descripcion,
                                String fotoUrl) {
        List<Bien> armados = bienes.stream().map(this::bienDe).toList();
        List<Donacion> segmentadas = segmentador.segmentar(armados, donante, descripcion);

        if (fotoUrl != null && !fotoUrl.isBlank()) {
            segmentadas.forEach(donacion -> donacion.asignarFoto(fotoUrl));
        }

        withTransaction(() -> segmentadas.forEach(this::registrarDonacion));
        return segmentadas;
    }

    private Bien bienDe(DatosBien datos) {
        Subcategoria subcategoria = subcategoriaPorNombre(datos.subcategoria());
        Bien bien = new Bien(datos.descripcion(), subcategoria, datos.cantidad(),
            condicionDe(datos.condicion()));
        if (datos.fechaVencimiento() != null && !datos.fechaVencimiento().isBlank()) {
            bien.setFechaVencimiento(fechaDe(datos.fechaVencimiento()));
        }
        return bien;
    }

    private static CondicionBien condicionDe(String condicion) {
        if (condicion == null) {
            return null;
        }
        try {
            return CondicionBien.valueOf(condicion);
        } catch (IllegalArgumentException e) {
            throw new DatosInvalidosException(
                "La condicion '" + condicion + "' no es valida.");
        }
    }

    private static LocalDate fechaDe(String fecha) {
        try {
            return LocalDate.parse(fecha);
        } catch (DateTimeParseException e) {
            throw new DatosInvalidosException(
                "La fecha de vencimiento '" + fecha + "' no es valida.");
        }
    }

    private void registrarDonacion(Donacion donacion) {
        conObservers(donacion);
        donacion.getDonante().registrarDonacion(donacion);
        repositorio.guardar(donacion);
    }

    public Donacion modificar(long id,
                              String descripcion,
                              String fotoUrl,
                              List<DatosModificacionBien> bienes) {
        Donacion[] modificada = new Donacion[1];
        try {
            withTransaction(() -> {
                modificada[0] = buscar(id);
                modificada[0].modificar(descripcion, fotoUrl, bienes);
                repositorioPropuestas.eliminar(id);
            });
        } catch (RuntimeException e) {
            entityManager().clear();
            throw e;
        }
        return modificada[0];
    }

    public void eliminar(long id) {
        Donacion donacion = buscar(id);
        withTransaction(() -> {
            repositorioNotificaciones.eliminarDeDonacion(donacion);
            repositorioPropuestas.eliminar(id);
            repositorio.eliminar(id);
        });
    }

    public void marcarListaParaEntregar(long id) {
        withTransaction(() -> buscar(id).marcarListaParaEntregar());
    }

    public void marcarEnTraslado(long id) {
        withTransaction(() -> buscar(id).marcarEnTraslado());
    }

    public void marcarEntregada(long id, LocalDateTime fechaHora, String patenteCamion) {
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

    public void planificarRutas(List<Long> donacionIds) {
        aplicarEventoEnLote(
            donacionIds,
            EstadosPosiblesDonacion.LISTA_PARA_ENTREGAR,
            List.of(EstadosPosiblesDonacion.ASIGNACION_REALIZADA),
            Donacion::marcarListaParaEntregar
        );
    }

    public void iniciarRuta(List<Long> donacionIds) {
        aplicarEventoEnLote(
            donacionIds,
            EstadosPosiblesDonacion.EN_TRASLADO,
            List.of(EstadosPosiblesDonacion.LISTA_PARA_ENTREGAR),
            Donacion::marcarEnTraslado
        );
    }

    public void registrarEntrega(long id,
                                 String resultado,
                                 String fechaHora,
                                 String patenteCamion,
                                 String motivo) {
        Donacion donacion = buscar(id);
        EstadosPosiblesDonacion actual = donacion.getEstado().getEstado();

        if ("ENTREGADA".equals(resultado)) {
            if (actual == EstadosPosiblesDonacion.ENTREGADA) {
                throw new EventoYaAplicadoException(
                    "La donación " + donacion.getId() + " ya estaba entregada."
                );
            }
            marcarEntregada(donacion.getId(), fechaHoraDe(fechaHora), patenteCamion);
        } else if ("NO_RECIBIDA".equals(resultado)) {
            if (actual == EstadosPosiblesDonacion.ENTREGA_FALLIDA) {
                throw new EventoYaAplicadoException(
                    "La donación " + donacion.getId() + " ya estaba marcada como no recibida."
                );
            }
            marcarEntregaFallida(donacion.getId(), motivo);
        } else {
            throw new DatosInvalidosException(
                "El resultado del evento debe ser ENTREGADA o NO_RECIBIDA (recibido: "
                    + resultado + ")."
            );
        }
    }

    private static LocalDateTime fechaHoraDe(String fechaHora) {
        if (fechaHora == null || fechaHora.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(fechaHora, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new DatosInvalidosException(
                "La fecha '" + fechaHora + "' no respeta el formato ISO-8601 (2026-09-09T14:30:00)."
            );
        }
    }

    public void registrarRetornoADeposito(long id) {
        Donacion donacion = buscar(id);
        if (donacion.getEstado().getEstado() == EstadosPosiblesDonacion.EN_DEPOSITO) {
            throw new EventoYaAplicadoException(
                "La donación " + donacion.getId() + " ya estaba en depósito."
            );
        }
        marcarEnDeposito(donacion.getId());
    }

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
