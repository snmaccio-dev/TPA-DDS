package donatrack.donaciones;

import donatrack.donaciones.controller.AdministradoresController;
import donatrack.donaciones.controller.AsignacionesController;
import donatrack.donaciones.controller.DonacionesController;
import donatrack.donaciones.controller.DonantesController;
import donatrack.donaciones.controller.EntidadesBeneficiariasController;
import donatrack.donaciones.controller.NecesidadesController;
import donatrack.donaciones.controller.NotificacionesController;
import donatrack.donaciones.controller.RegistroController;
import donatrack.donaciones.controller.SubcategoriasController;
import donatrack.donaciones.controller.UsuariosController;
import io.javalin.Javalin;

import static donatrack.donaciones.seguridad.RolDeAcceso.ADMINISTRADOR;
import static donatrack.donaciones.seguridad.RolDeAcceso.BENEFICIARIA;
import static donatrack.donaciones.seguridad.RolDeAcceso.CHOFER;
import static donatrack.donaciones.seguridad.RolDeAcceso.DONANTE;
import static donatrack.donaciones.seguridad.RolDeAcceso.PUBLICO;
import static donatrack.donaciones.seguridad.RolDeAcceso.SERVICIO_LOGISTICA;

// Punto unico donde se declara toda la API del modulo Donaciones. Mirar este archivo
// alcanza para ver que metodos, paths y roles se exponen.
//
// Regla: toda ruta declara sus roles. PUBLICO marca las que no piden token, porque
// FiltroDeAcceso rechaza con 403 cualquier ruta sin roles.
//
// Orden dentro de cada grupo: las rutas con segmentos fijos (p. ej. /donantes/inactivos,
// /entidades/consulta) van antes que las que tienen path params homologos
// (/donantes/{documento}, /entidades/{id}), para que Javalin no interprete el segmento
// fijo como valor del parametro.
public class Rutas {

  private final RegistroController registro;
  private final UsuariosController usuarios;
  private final AdministradoresController administradores;
  private final DonacionesController donaciones;
  private final AsignacionesController asignaciones;
  private final DonantesController donantes;
  private final EntidadesBeneficiariasController entidades;
  private final NecesidadesController necesidades;
  private final SubcategoriasController subcategorias;
  private final NotificacionesController notificaciones;

  public Rutas(RegistroController registro,
               UsuariosController usuarios,
               AdministradoresController administradores,
               DonacionesController donaciones,
               AsignacionesController asignaciones,
               DonantesController donantes,
               EntidadesBeneficiariasController entidades,
               NecesidadesController necesidades,
               SubcategoriasController subcategorias,
               NotificacionesController notificaciones) {
    this.registro = registro;
    this.usuarios = usuarios;
    this.administradores = administradores;
    this.donaciones = donaciones;
    this.asignaciones = asignaciones;
    this.donantes = donantes;
    this.entidades = entidades;
    this.necesidades = necesidades;
    this.subcategorias = subcategorias;
    this.notificaciones = notificaciones;
  }

  public void registrarEn(Javalin app) {
    registro(app, registro);
    usuarios(app, usuarios);
    administradores(app, administradores);
    donaciones(app, donaciones);
    asignaciones(app, asignaciones);
    donantes(app, donantes);
    entidades(app, entidades);
    necesidades(app, necesidades);
    subcategorias(app, subcategorias);
    notificaciones(app, notificaciones);
  }

  // === Registro publico (no piden token) ===
  public static void registro(Javalin app, RegistroController c) {
    app.post("/registro/donantes",      c::registrarDonante,      PUBLICO);
    app.post("/registro/beneficiarias", c::registrarBeneficiaria, PUBLICO);
  }

  // === Usuarios: perfil del usuario autenticado ===
  public static void usuarios(Javalin app, UsuariosController c) {
    app.get("/usuarios/yo", c::yo, DONANTE, BENEFICIARIA, ADMINISTRADOR, CHOFER);
  }

  // === Administradores: alta directa contra Keycloak ===
  public static void administradores(Javalin app, AdministradoresController c) {
    app.post("/administradores", c::crear, ADMINISTRADOR);
  }

  // === Donaciones: listado, CRUD y eventos del servicio de Logistica ===
  public static void donaciones(Javalin app, DonacionesController c) {
    app.get(   "/donaciones",                    c::listar,      ADMINISTRADOR, DONANTE, BENEFICIARIA, SERVICIO_LOGISTICA);
    app.post(  "/donaciones",                    c::crear,       ADMINISTRADOR);
    app.get(   "/donaciones/{id}",               c::buscar,      ADMINISTRADOR, DONANTE, BENEFICIARIA);
    app.delete("/donaciones/{id}",               c::eliminar,    ADMINISTRADOR);
    app.get(   "/donaciones/{id}/comprobante",   c::comprobante, ADMINISTRADOR, DONANTE, BENEFICIARIA);
    app.get(   "/donaciones/{id}/historial",     c::historial,   ADMINISTRADOR, DONANTE, BENEFICIARIA);
    app.post(  "/donaciones/{id}/vencer",        c::vencer,      ADMINISTRADOR);

    // Eventos emitidos por el servicio de Logistica. Van en este grupo porque todos
    // cuelgan del recurso /donaciones y los maneja DonacionesController.
    app.post("/donaciones/eventos/rutas-planificadas", c::eventoRutasPlanificadas, SERVICIO_LOGISTICA);
    app.post("/donaciones/eventos/inicio-ruta",        c::eventoInicioRuta,        SERVICIO_LOGISTICA);
    app.post("/donaciones/eventos/entrega",            c::eventoEntrega,           SERVICIO_LOGISTICA);
    app.post("/donaciones/eventos/retorno-deposito",   c::eventoRetornoDeposito,   SERVICIO_LOGISTICA);
  }

  // === Asignaciones: matchmaking y confirmacion de destinatario ===
  public static void asignaciones(Javalin app, AsignacionesController c) {
    app.get( "/asignaciones/propuestas",       c::propuestas,            ADMINISTRADOR);
    app.post("/asignaciones/ejecutar",         c::ejecutarMatchmaking,   ADMINISTRADOR);
    app.get( "/donaciones/{id}/propuesta",     c::propuestaDeDonacion,   ADMINISTRADOR);
    app.post("/donaciones/{id}/asignacion",    c::ejecutarAsignacion,    ADMINISTRADOR);
    app.post("/donaciones/{id}/destinatario",  c::confirmarDestinatario, ADMINISTRADOR);
  }

  // === Donantes (panel de administracion) ===
  public static void donantes(Javalin app, DonantesController c) {
    app.get(   "/donantes",                   c::todas,            ADMINISTRADOR);
    app.post(  "/donantes",                   c::registrar,        ADMINISTRADOR);
    // /donantes/inactivos y /donantes/avisar-inactivos tienen que registrarse ANTES
    // que /donantes/{documento} para que Javalin no tome el literal como documento.
    app.get(   "/donantes/inactivos",         c::inactivos,        ADMINISTRADOR);
    app.post(  "/donantes/avisar-inactivos",  c::avisarInactivos,  ADMINISTRADOR);
    app.get(   "/donantes/{documento}",       c::buscar,           ADMINISTRADOR);
    app.delete("/donantes/{documento}",       c::eliminar,         ADMINISTRADOR);
  }

  // === Entidades beneficiarias ===
  public static void entidades(Javalin app, EntidadesBeneficiariasController c) {
    app.get(   "/entidades",            c::todas,                   ADMINISTRADOR, DONANTE);
    // /entidades/consulta tiene que ir ANTES que /entidades/{id}: si no, "consulta" se
    // lee como id y el handler nunca recibe el request.
    app.get(   "/entidades/consulta",   c::consultarPorCuit,        ADMINISTRADOR);
    app.get(   "/entidades/{id}",       c::buscar,                  ADMINISTRADOR, DONANTE);
    app.post(  "/entidades/existente",  c::altaDePersonaExistente,  ADMINISTRADOR);
    app.post(  "/entidades/nueva",      c::altaConPersonaNueva,     ADMINISTRADOR);
    app.delete("/entidades/{id}",       c::eliminar,                ADMINISTRADOR);
  }

  // === Necesidades (declaradas por beneficiarias, administradas por admin) ===
  public static void necesidades(Javalin app, NecesidadesController c) {
    app.get(   "/necesidades",       c::todas,      ADMINISTRADOR, BENEFICIARIA);
    app.post(  "/necesidades",       c::crear,      ADMINISTRADOR, BENEFICIARIA);
    app.get(   "/necesidades/{id}",  c::buscar,     ADMINISTRADOR, BENEFICIARIA);
    app.put(   "/necesidades/{id}",  c::actualizar, ADMINISTRADOR, BENEFICIARIA);
    app.delete("/necesidades/{id}",  c::eliminar,   ADMINISTRADOR, BENEFICIARIA);
  }

  // === Subcategorias (catalogo) ===
  public static void subcategorias(Javalin app, SubcategoriasController c) {
    app.get("/subcategorias", c::todas, ADMINISTRADOR, BENEFICIARIA, DONANTE);
  }

  // === Notificaciones ===
  public static void notificaciones(Javalin app, NotificacionesController c) {
    app.get(  "/notificaciones/mias",                     c::mias,              DONANTE, BENEFICIARIA, ADMINISTRADOR);
    app.get(  "/notificaciones/mias/no-leidas/cantidad",  c::cantidadNoLeidas,  DONANTE, BENEFICIARIA, ADMINISTRADOR);
    app.patch("/notificaciones/{id}/leida",               c::marcarLeida,       DONANTE, BENEFICIARIA, ADMINISTRADOR);
    app.post( "/notificaciones/mias/leidas",              c::marcarTodasLeidas, DONANTE, BENEFICIARIA, ADMINISTRADOR);
    app.get(  "/notificaciones",                          c::todas,             ADMINISTRADOR);
  }
}
