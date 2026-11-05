package donatrack.logistica.controller;

import donatrack.logistica.controller.dto.ConfirmarRecepcionRequest;
import donatrack.logistica.controller.dto.EntregaDTO;
import donatrack.logistica.controller.dto.EntregaPublicaDTO;
import donatrack.logistica.controller.dto.FotosSubidasDTO;
import donatrack.logistica.controller.dto.NoRecepcionRequest;
import donatrack.logistica.controller.dto.RegistrarMedidasRequest;
import donatrack.logistica.domain.almacenamiento.FotoParaSubir;
import donatrack.logistica.domain.integracion.IntegracionExternaException;
import donatrack.logistica.service.GestorEntregas;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class EntregasController {

  private final GestorEntregas gestor;

  public EntregasController(GestorEntregas gestor) {
    this.gestor = gestor;
  }

  public void registrarRutas(Javalin app) {
    app.get("/entregas", this::todas);
    // Rutas mas especificas antes que /entregas/{id}, para que Javalin no se coma
    // "pendientes-de-medicion" como si fuera un id.
    app.get("/entregas/pendientes-de-medicion", this::pendientesDeMedicion);
    app.get("/entregas/publicas", this::publicas);
    app.get("/entregas/{id}", this::buscar);
    app.post("/entregas/{id}/medidas", this::registrarMedidas);
    app.post("/entregas/{id}/fotos", this::subirFotos);
    app.post("/entregas/{id}/confirmacion", this::confirmar);
    app.post("/entregas/{id}/no-recepcion", this::noRecibida);
    app.post("/entregas/{id}/retorno-deposito", this::retornarADeposito);
  }

  private void todas(Context ctx) {
    ctx.json(EntregaDTO.desde(gestor.todas()));
  }

  private void buscar(Context ctx) {
    ctx.json(EntregaDTO.desde(gestor.buscar(idDe(ctx))));
  }

  private void pendientesDeMedicion(Context ctx) {
    ctx.json(EntregaDTO.desde(gestor.pendientesDeMedicion()));
  }

  private void publicas(Context ctx) {
    ctx.json(EntregaPublicaDTO.desde(gestor.entregadas()));
  }

  private void registrarMedidas(Context ctx) {
    RegistrarMedidasRequest request = ctx.bodyAsClass(RegistrarMedidasRequest.class);
    ctx.json(EntregaDTO.desde(
        gestor.registrarMedicion(idDe(ctx), request.pesoKg(), request.volumenM3())
    ));
  }

  private void subirFotos(Context ctx) {
    List<FotoParaSubir> fotos = ctx.uploadedFiles("fotos").stream()
        .map(archivo -> new FotoParaSubir(archivo.filename(), contenidoDe(archivo)))
        .toList();

    ctx.status(201).json(new FotosSubidasDTO(gestor.subirFotos(idDe(ctx), fotos)));
  }

  private byte[] contenidoDe(UploadedFile archivo) {
    try (InputStream contenido = archivo.content()) {
      return contenido.readAllBytes();
    } catch (IOException e) {
      throw new IntegracionExternaException(
          "No se pudo leer la foto " + archivo.filename() + ".", e
      );
    }
  }

  private void confirmar(Context ctx) {
    ConfirmarRecepcionRequest request = ctx.bodyAsClass(ConfirmarRecepcionRequest.class);
    ctx.json(EntregaDTO.desde(gestor.confirmarRecepcion(idDe(ctx), request.fotosOVacio())));
  }

  private void noRecibida(Context ctx) {
    NoRecepcionRequest request = ctx.bodyAsClass(NoRecepcionRequest.class);
    ctx.json(EntregaDTO.desde(gestor.marcarNoRecibida(idDe(ctx), request.motivo())));
  }

  private void retornarADeposito(Context ctx) {
    ctx.json(EntregaDTO.desde(gestor.retornarADeposito(idDe(ctx))));
  }

  private long idDe(Context ctx) {
    return ctx.pathParamAsClass("id", Long.class).get();
  }
}