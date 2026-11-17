package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.ComprobanteDTO;
import donatrack.donaciones.controller.dto.DonacionDTO;
import donatrack.donaciones.controller.dto.EstadoHistorialDTO;
import donatrack.donaciones.controller.dto.EventoEntregaRequest;
import donatrack.donaciones.controller.dto.EventoInicioRutaRequest;
import donatrack.donaciones.controller.dto.EventoRetornoDepositoRequest;
import donatrack.donaciones.controller.dto.EventoRutasPlanificadasRequest;
import donatrack.donaciones.controller.dto.ModificarDonacionRequest;
import donatrack.donaciones.controller.dto.NuevaDonacionRequest;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.estado.EstadosPosiblesDonacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.seguridad.Acceso;
import donatrack.donaciones.seguridad.FiltroDeAcceso;
import donatrack.donaciones.seguridad.UsuarioActual;
import donatrack.donaciones.service.DatosBien;
import donatrack.donaciones.service.DonacionesService;
import donatrack.donaciones.service.DonantesService;
import donatrack.donaciones.service.FiltroDonaciones;
import io.javalin.http.Context;

import java.util.List;

public class DonacionesController {

  private final DonacionesService service;
  private final DonantesService donantesService;

  public DonacionesController(DonacionesService service, DonantesService donantesService) {
    this.service = service;
    this.donantesService = donantesService;
  }

  public void listar(Context ctx) {
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
    ctx.json(DonacionDTO.desde(service.buscar(filtro, donanteFiltro, beneficiariaFiltro)));
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

  public void buscar(Context ctx) {
    Donacion donacion = service.buscar(idDe(ctx));
    Acceso.exigirPuedeVerDonacion(FiltroDeAcceso.usuarioDe(ctx), donacion);
    ctx.json(DonacionDTO.desde(donacion));
  }

  public void crear(Context ctx) {
    NuevaDonacionRequest request = ctx.bodyAsClass(NuevaDonacionRequest.class);
    UsuarioActual usuario = FiltroDeAcceso.usuarioDe(ctx);

    Donante donante = usuario.esAdministrador()
        ? donantesService.buscarDonantePorDocumento(request.donanteDocumento())
        : Acceso.perfilDonanteObligatorio(usuario);

    List<DatosBien> bienes = request.bienes().stream()
        .map(b -> new DatosBien(
            b.descripcion(), b.subcategoria(), b.cantidad(), b.condicion(), b.fechaVencimiento()))
        .toList();

    ctx.status(201).json(
        DonacionDTO.desde(service.crear(donante, bienes, request.descripcion(), null))
    );
  }

  public void modificar(Context ctx) {
    long id = idDe(ctx);
    Acceso.exigirPuedeModificarDonacion(FiltroDeAcceso.usuarioDe(ctx), service.buscar(id));
    ModificarDonacionRequest request = ctx.bodyAsClass(ModificarDonacionRequest.class);
    ctx.json(DonacionDTO.desde(
        service.modificar(id, request.descripcion(), request.fotoUrl(), request.datosBienes())));
  }

  public void eliminar(Context ctx) {
    service.eliminar(idDe(ctx));
    ctx.status(204);
  }

  public void comprobante(Context ctx) {
    Donacion donacion = service.buscar(idDe(ctx));
    Acceso.exigirPuedeVerDonacion(FiltroDeAcceso.usuarioDe(ctx), donacion);
    ctx.json(ComprobanteDTO.desde(donacion.generarComprobante()));
  }

  public void historial(Context ctx) {
    Donacion donacion = service.buscar(idDe(ctx));
    Acceso.exigirPuedeVerDonacion(FiltroDeAcceso.usuarioDe(ctx), donacion);
    ctx.json(EstadoHistorialDTO.desde(service.historial(donacion.getId())));
  }

  public void vencer(Context ctx) {
    service.marcarVencida(idDe(ctx));
    ctx.status(204);
  }

  // === Eventos ===

  public void eventoRutasPlanificadas(Context ctx) {
    EventoRutasPlanificadasRequest evento =
        ctx.bodyAsClass(EventoRutasPlanificadasRequest.class);
    service.planificarRutas(evento.donacionIds());
    ctx.status(200);
  }

  public void eventoInicioRuta(Context ctx) {
    EventoInicioRutaRequest evento = ctx.bodyAsClass(EventoInicioRutaRequest.class);
    service.iniciarRuta(evento.donacionIds());
    ctx.status(200);
  }

  public void eventoEntrega(Context ctx) {
    EventoEntregaRequest evento = ctx.bodyAsClass(EventoEntregaRequest.class);
    service.registrarEntrega(
        evento.donacionId(),
        evento.resultado(),
        evento.fechaHora(),
        evento.patenteCamion(),
        evento.motivo()
    );
    ctx.status(200);
  }

  public void eventoRetornoDeposito(Context ctx) {
    EventoRetornoDepositoRequest evento = ctx.bodyAsClass(EventoRetornoDepositoRequest.class);
    service.registrarRetornoADeposito(evento.donacionId());
    ctx.status(200);
  }

  private long idDe(Context ctx) {
    return ctx.pathParamAsClass("id", Long.class).get();
  }
}
