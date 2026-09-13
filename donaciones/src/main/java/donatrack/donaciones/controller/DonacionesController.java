package donatrack.donaciones.controller;

import donatrack.donaciones.contrato.Fechas;
import donatrack.donaciones.controller.dto.ComprobanteDTO;
import donatrack.donaciones.controller.dto.DonacionDTO;
import donatrack.donaciones.controller.dto.EstadoHistorialDTO;
import donatrack.donaciones.controller.dto.EventoEntregaRequest;
import donatrack.donaciones.controller.dto.EventoInicioRutaRequest;
import donatrack.donaciones.controller.dto.EventoRetornoDepositoRequest;
import donatrack.donaciones.controller.dto.EventoRutasPlanificadasRequest;
import donatrack.donaciones.controller.dto.NuevaDonacionRequest;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.CondicionBien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.estado.EstadosPosiblesDonacion;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioSubcategorias;
import donatrack.donaciones.service.EventoYaAplicadoException;
import donatrack.donaciones.service.GestorDonaciones;
import donatrack.donaciones.service.RecursoInexistenteException;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.util.List;

public class DonacionesController {

  private final GestorDonaciones gestor;
  private final RepositorioPersonas repositorioPersonas;
  private final RepositorioSubcategorias repositorioSubcategorias;

  public DonacionesController(GestorDonaciones gestor,
                              RepositorioPersonas repositorioPersonas,
                              RepositorioSubcategorias repositorioSubcategorias) {
    this.gestor = gestor;
    this.repositorioPersonas = repositorioPersonas;
    this.repositorioSubcategorias = repositorioSubcategorias;
  }

  public void registrarRutas(Javalin app) {
    app.get("/donaciones", this::listar);
    app.post("/donaciones", this::crear);
    app.get("/donaciones/{id}", this::buscar);
    app.delete("/donaciones/{id}", this::eliminar);
    app.get("/donaciones/{id}/comprobante", this::comprobante);
    app.get("/donaciones/{id}/historial", this::historial);
    app.post("/donaciones/{id}/vencer", this::vencer);

    app.post("/donaciones/eventos/rutas-planificadas", this::eventoRutasPlanificadas);
    app.post("/donaciones/eventos/inicio-ruta", this::eventoInicioRuta);
    app.post("/donaciones/eventos/entrega", this::eventoEntrega);
    app.post("/donaciones/eventos/retorno-deposito", this::eventoRetornoDeposito);
  }

  private void listar(Context ctx) {
    String estado = ctx.queryParam("estado");
    List<Donacion> donaciones = estado == null
        ? gestor.todas()
        : gestor.porEstado(EstadosPosiblesDonacion.valueOf(estado));
    ctx.json(DonacionDTO.desde(donaciones));
  }

  private void buscar(Context ctx) {
    ctx.json(DonacionDTO.desde(gestor.buscar(idDe(ctx))));
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
    ctx.json(ComprobanteDTO.desde(gestor.buscar(idDe(ctx)).generarComprobante()));
  }

  private void historial(Context ctx) {
    ctx.json(EstadoHistorialDTO.desde(gestor.historial(idDe(ctx))));
  }

  private void vencer(Context ctx) {
    gestor.marcarVencida(idDe(ctx));
    ctx.status(204);
  }

  // === Eventos ===

  private void eventoRutasPlanificadas(Context ctx) {
    EventoRutasPlanificadasRequest evento =
        ctx.bodyAsClass(EventoRutasPlanificadasRequest.class);
    gestor.aplicarEventoEnLote(
        evento.donacionIds(),
        EstadosPosiblesDonacion.LISTA_PARA_ENTREGAR,
        List.of(EstadosPosiblesDonacion.ASIGNACION_REALIZADA),
        d -> gestor.marcarListaParaEntregar(d.getId())
    );
    ctx.status(200);
  }

  private void eventoInicioRuta(Context ctx) {
    EventoInicioRutaRequest evento = ctx.bodyAsClass(EventoInicioRutaRequest.class);
    gestor.aplicarEventoEnLote(
        evento.donacionIds(),
        EstadosPosiblesDonacion.EN_TRASLADO,
        List.of(EstadosPosiblesDonacion.LISTA_PARA_ENTREGAR),
        d -> gestor.marcarEnTraslado(d.getId())
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
      gestor.marcarEntregada(donacion.getId(), Fechas.aFechaHora(evento.fechaHora()), evento.patenteCamion());
    } else if ("NO_RECIBIDA".equals(evento.resultado())) {
      if (actual == EstadosPosiblesDonacion.ENTREGA_FALLIDA) {
        throw new EventoYaAplicadoException(
            "La donación " + donacion.getId() + " ya estaba marcada como no recibida."
        );
      }
      gestor.marcarEntregaFallida(donacion.getId(), evento.motivo());
    } else {
      throw new IllegalArgumentException(
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
    Persona persona = repositorioPersonas.buscarPorDocumento(documento)
        .orElseThrow(() -> new RecursoInexistenteException(
            "No existe la persona con documento " + documento
        ));
    return persona.comoRol(Donante.class)
        .orElseThrow(() -> new IllegalArgumentException(
            "La persona con documento " + documento + " no tiene el rol Donante."
        ));
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