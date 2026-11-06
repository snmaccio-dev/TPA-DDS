package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.SubcategoriaDTO;
import donatrack.donaciones.repository.RepositorioSubcategorias;
import donatrack.donaciones.seguridad.RolDeAcceso;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class SubcategoriasController {

  private final RepositorioSubcategorias repositorio;

  public SubcategoriasController(RepositorioSubcategorias repositorio) {
    this.repositorio = repositorio;
  }

  public void registrarRutas(Javalin app) {
    // Abierto a los tres roles que cargan algo contra el catalogo: la administracion al
    // registrar donaciones en el deposito, la beneficiaria al declarar necesidades y la
    // persona donante al ofrecer una donacion. No es publico: es informacion operativa
    // del sistema y no hay razon para exponerla sin sesion.
    app.get("/subcategorias", this::todas,
        RolDeAcceso.ADMINISTRADOR, RolDeAcceso.BENEFICIARIA, RolDeAcceso.DONANTE);
  }

  private void todas(Context ctx) {
    ctx.json(SubcategoriaDTO.desde(repositorio.todas()));
  }
}
