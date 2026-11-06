package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.ComprobanteDTO;
import donatrack.donaciones.controller.dto.DonacionDTO;
import donatrack.donaciones.controller.dto.EstadoHistorialDTO;
import donatrack.donaciones.controller.dto.EventoEntregaRequest;
import donatrack.donaciones.controller.dto.EventoInicioRutaRequest;
import donatrack.donaciones.controller.dto.EventoRetornoDepositoRequest;
import donatrack.donaciones.controller.dto.EventoRutasPlanificadasRequest;
import donatrack.donaciones.controller.dto.NuevaDonacionRequest;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.CondicionBien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.estado.EstadosPosiblesDonacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioSubcategorias;
import donatrack.donaciones.seguridad.Acceso;
import donatrack.donaciones.seguridad.FiltroDeAcceso;
import donatrack.donaciones.seguridad.RolDeAcceso;
import donatrack.donaciones.seguridad.UsuarioActual;
import donatrack.donaciones.service.EventoYaAplicadoException;
import donatrack.donaciones.service.FiltroDonaciones;
import donatrack.donaciones.service.GestorDonaciones;
import donatrack.donaciones.service.RecursoInexistenteException;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.util.List;

public class DonacionesController {

  private final GestorDonaciones gestor;
  private final RepositorioPersonas repositorioPersonas;
  private final RepositorioDonantes repositorioDonantes;
  private final RepositorioSubcategorias repositorioSubcategorias;

  public DonacionesController(GestorDonaciones gestor,
                              RepositorioPersonas repositorioPersonas,
                              RepositorioDonantes repositorioDonantes,
                              RepositorioSubcategorias repositorioSubcategorias) {
    this.gestor = gestor;
    this.repositorioPersonas = repositorioPersonas;
    this.repositorioDonantes = repositorioDonantes;
    this.repositorioSubcategorias = repositorioSubcategorias;
  }

  public void registrarRutas(Javalin app) {
    app.get("/donaciones", this::listar,
        RolDeAcceso.ADMINISTRADOR, RolDeAcceso.DONANTE, RolDeAcceso.BENEFICIARIA, RolDeAcceso.SERVICIO_LOGISTICA);
    app.post("/donaciones", this::crear, RolDeAcceso.ADMINISTRADOR);
    app.get("/donaciones/{id}", this::buscar,
        RolDeAcceso.ADMINISTRADOR, RolDeAcceso.DONANTE, RolDeAcceso.BENEFICIARIA);
    app.delete("/donaciones/{id}", this::eliminar, RolDeAcceso.ADMINISTRADOR);
    app.get("/donaciones/{id}/comprobante", this::comprobante,
        RolDeAcceso.ADMINISTRADOR, RolDeAcceso.DONANTE, RolDeAcceso.BENEFICIARIA);
    app.get("/donaciones/{id}/historial", this::historial,
        RolDeAcceso.ADMINISTRADOR, RolDeAcceso.DONANTE, RolDeAcceso.BENEFICIARIA);
    app.post("/donaciones/{id}/vencer", this::vencer, RolDeAcceso.ADMINISTRADOR);

    app.post("/donaciones/eventos/rutas-planificadas", this::eventoRutasPlanificadas, RolDeAcceso.SERVICIO_LOGISTICA);
    app.post("/donaciones/eventos/inicio-ruta", this::eventoInicioRuta, RolDeAcceso.SERVICIO_LOGISTICA);
    app.post("/donaciones/eventos/entrega", this::eventoEntrega, RolDeAcceso.SERVICIO_LOGISTICA);
    app.post("/donaciones/eventos/retorno-deposito", this::eventoRetornoDeposito, RolDeAcceso.SERVICIO_LOGISTICA);
  }

  private void listar(Context ctx) {
    UsuarioActual usuario = FiltroDeAcceso.usuarioDe(ctx);
    FiltroDonaciones filtro = filtroDesde(ctx);

    Donante donanteFiltro = null;
    Beneficiaria beneficiariaFiltro = null;
    if (usuario.esAdministrador() || usuario.esServicioLogistica()) {
      // sin restriccion extra: el admin y el servicio de logistica ven todas.
    } else if (usuario.esDonante()) {
      donanteFiltro = Acceso.perfilDonanteObligatorio(usuario);
    } else if (usuario.esBeneficiaria()) {
      beneficiariaFiltro = Acceso.perfilBeneficiariaObligatorio(usuario);
    } else {
      ctx.json(DonacionDTO.desde(List.<Donacion>of()));
      return;
    }
    ctx.json(DonacionDTO.desde(gestor.buscar(filtro, donanteFiltro, beneficiariaFiltro)));
  }

  private FiltroDonaciones filtroDesde(Context ctx) {
    String estadoParam = ctx.queryParam("estado");
    EstadosPosiblesDonacion estado = null;
    if (estadoParam != null && !estadoParam.isBlank()) {
      try {
        estado = EstadosPosiblesDonacion.valueOf(estadoParam);
      } catch (IllegalArgumentException e) {
        throw new DatosInvalidosException(
            "El estado '" + estadoParam + "' no es valido.");
      }
    }
    String categoriaParam = ctx.queryParam("categoria");
    String categoria = (categoriaParam == null || categoriaParam.isBlank()) ? null : categoriaParam;
    List<String> subcategoriasParam = ctx.queryParams("subcategoria");
    List<String> subcategorias = (subcategoriasParam == null || subcategoriasParam.isEmpty())
        ? null
        : subcategoriasParam;
    return new FiltroDonaciones(estado, categoria, subcategorias);
  }

  private void buscar(Context ctx) {
    Donacion donacion = gestor.buscar(idDe(ctx));
    Acceso.exigirPuedeVerDonacion(FiltroDeAcceso.usuarioDe(ctx), donacion);
    ctx.json(DonacionDTO.desde(donacion));
  }

  private void crear(Context ctx) {
    NuevaDonacionRequest request = ctx.bodyAsClass(NuevaDonacionRequest.class);
    Donante donante = donanteDe(request.donanteDocumento());
    List<Bien> bienes = request.bienes().stream()
        .map(this::bienDe)
        .toList();
    ctx.status(201).json(DonacionDTO.desde(gestor.crear(bienes, donante, request.descripcion())));
  }

  private void eliminar(Context ctx) {
    gestor.eliminar(idDe(ctx));
    ctx.status(204);
  }

  private void comprobante(Context ctx) {
    Donacion donacion = gestor.buscar(idDe(ctx));
    Acceso.exigirPuedeVerDonacion(FiltroDeAcceso.usuarioDe(ctx), donacion);
    ctx.json(ComprobanteDTO.desde(donacion.generarComprobante()));
  }

  private void historial(Context ctx) {
    Donacion donacion = gestor.buscar(idDe(ctx));
    Acceso.exigirPuedeVerDonacion(FiltroDeAcceso.usuarioDe(ctx), donacion);
    ctx.json(EstadoHistorialDTO.desde(gestor.historial(donacion.getId())));
  }

  private void vencer(Context ctx) {
    gestor.marcarVencida(idDe(ctx));
    ctx.status(204);
  }

  // === Eventos ===

  private void eventoRutasPlanificadas(Context ctx) {
    EventoRutasPlanificadasRequest evento =
        ctx.bodyAsClass(EventoRutasPlanificadasRequest.class);
    // Pasamos el metodo del dominio directo: la donacion ya la cargo
    // aplicarEventoEnLote y la transicion ocurre dentro de su propio
    // withTransaction. Si usaramos gestor.marcarListaParaEntregar(id) la
    // llamada abriria un withTransaction anidado y, al salir, haria commit
    // de la transaccion de afuera: el lote dejaria de ser "todo o nada".
    gestor.aplicarEventoEnLote(
        evento.donacionIds(),
        EstadosPosiblesDonacion.LISTA_PARA_ENTREGAR,
        List.of(EstadosPosiblesDonacion.ASIGNACION_REALIZADA),
        Donacion::marcarListaParaEntregar
    );
    ctx.status(200);
  }

  private void eventoInicioRuta(Context ctx) {
    EventoInicioRutaRequest evento = ctx.bodyAsClass(EventoInicioRutaRequest.class);
    gestor.aplicarEventoEnLote(
        evento.donacionIds(),
        EstadosPosiblesDonacion.EN_TRASLADO,
        List.of(EstadosPosiblesDonacion.LISTA_PARA_ENTREGAR),
        Donacion::marcarEnTraslado
    );
    ctx.status(200);
  }

  private void eventoEntrega(Context ctx) {
    EventoEntregaRequest evento = ctx.bodyAsClass(EventoEntregaRequest.class);
    Donacion donacion = gestor.buscar(evento.donacionId());
    EstadosPosiblesDonacion actual = donacion.getEstado().getEstado();

    if ("ENTREGADA".equals(evento.resultado())) {
      if (actual == EstadosPosiblesDonacion.ENTREGADA) {
        throw new EventoYaAplicadoException(
            "La donación " + donacion.getId() + " ya estaba entregada."
        );
      }
      gestor.marcarEntregada(donacion.getId(), evento.fechaHoraMedida(), evento.patenteCamion());
    } else if ("NO_RECIBIDA".equals(evento.resultado())) {
      if (actual == EstadosPosiblesDonacion.ENTREGA_FALLIDA) {
        throw new EventoYaAplicadoException(
            "La donación " + donacion.getId() + " ya estaba marcada como no recibida."
        );
      }
      gestor.marcarEntregaFallida(donacion.getId(), evento.motivo());
    } else {
      throw new DatosInvalidosException(
          "El resultado del evento debe ser ENTREGADA o NO_RECIBIDA (recibido: "
              + evento.resultado() + ")."
      );
    }
    ctx.status(200);
  }

  private void eventoRetornoDeposito(Context ctx) {
    EventoRetornoDepositoRequest evento = ctx.bodyAsClass(EventoRetornoDepositoRequest.class);
    Donacion donacion = gestor.buscar(evento.donacionId());
    EstadosPosiblesDonacion actual = donacion.getEstado().getEstado();
    if (actual == EstadosPosiblesDonacion.EN_DEPOSITO) {
      throw new EventoYaAplicadoException(
          "La donación " + donacion.getId() + " ya estaba en depósito."
      );
    }
    gestor.marcarEnDeposito(donacion.getId());
    ctx.status(200);
  }

  // === Helpers ===

  private Donante donanteDe(String documento) {
    return repositorioDonantes.buscarPorDocumento(documento)
        .orElseGet(() -> {
          repositorioPersonas.buscarPorDocumento(documento)
              .orElseThrow(() -> new RecursoInexistenteException(
                  "No existe la persona con documento " + documento
              ));
          throw new DatosInvalidosException(
              "La persona con documento " + documento + " no tiene el rol Donante."
          );
        });
  }

  private Bien bienDe(NuevaDonacionRequest.BienRequest req) {
    Subcategoria subcategoria = repositorioSubcategorias.buscarPorNombre(req.subcategoria())
        .orElseThrow(() -> new RecursoInexistenteException(
            "No existe la subcategoría '" + req.subcategoria() + "'."
        ));
    CondicionBien condicion = req.condicion() == null ? null : CondicionBien.valueOf(req.condicion());
    Bien bien = new Bien(req.descripcion(), subcategoria, req.cantidad(), condicion);
    if (req.fechaVencimiento() != null && !req.fechaVencimiento().isBlank()) {
      bien.setFechaVencimiento(LocalDate.parse(req.fechaVencimiento()));
    }
    return bien;
  }

  private long idDe(Context ctx) {
    return ctx.pathParamAsClass("id", Long.class).get();
  }
}