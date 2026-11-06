package donatrack.donaciones.seguridad;

import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.catalogo.Unidades;
import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.CondicionBien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.necesidad.Necesidad;
import donatrack.donaciones.domain.necesidad.NecesidadExtraordinaria;
import donatrack.donaciones.domain.notificacion.Notificacion;
import donatrack.donaciones.domain.notificacion.TipoNotificacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import io.javalin.http.ForbiddenResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Tests unitarios puros (sin DB, sin servidor) sobre los chequeos de propiedad.
class AccesoTest {

  @Test
  void unDonanteVeSuDonacionYNoLaAjena() throws Exception {
    Donante donanteAna = donanteConId("Ana", "11111111", 10L);
    Donante donanteLuis = donanteConId("Luis", "22222222", 20L);

    UsuarioActual usuario = usuarioDonante("kc-ana", "ana@mail.com", donanteAna);

    Donacion propia = donacionDe(donanteAna);
    Donacion ajena = donacionDe(donanteLuis);

    assertDoesNotThrow(() -> Acceso.exigirPuedeVerDonacion(usuario, propia));
    assertThrows(ForbiddenResponse.class, () -> Acceso.exigirPuedeVerDonacion(usuario, ajena));
  }

  @Test
  void unaBeneficiariaVeLasAsignadasAEllaYNoLasOtras() throws Exception {
    Beneficiaria escuela = beneficiariaConId("30-11111111-1", 100L);
    Beneficiaria comedor = beneficiariaConId("30-22222222-2", 200L);
    Donante donante = donanteConId("Ana", "11111111", 10L);

    UsuarioActual usuario = usuarioBeneficiaria("kc-escuela", "escuela@mail.com", escuela);

    Donacion asignadaAEscuela = donacionDe(donante);
    asignadaAEscuela.confirmarDestino(escuela);

    Donacion asignadaAComedor = donacionDe(donante);
    asignadaAComedor.confirmarDestino(comedor);

    assertDoesNotThrow(() -> Acceso.exigirPuedeVerDonacion(usuario, asignadaAEscuela));
    assertThrows(ForbiddenResponse.class, () -> Acceso.exigirPuedeVerDonacion(usuario, asignadaAComedor));
  }

  @Test
  void elAdminVeCualquierDonacion() throws Exception {
    Donante donante = donanteConId("Ana", "11111111", 10L);
    UsuarioActual admin = new UsuarioActual(
        "kc-admin", "admin@mail.com",
        Set.of(RolDeAcceso.ADMINISTRADOR),
        Optional.empty(), Optional.empty());

    assertDoesNotThrow(() -> Acceso.exigirPuedeVerDonacion(admin, donacionDe(donante)));
  }

  @Test
  void unUsuarioDonanteSinPerfilRecibe403() {
    UsuarioActual sinPerfil = new UsuarioActual(
        "kc-xx", "xx@mail.com",
        Set.of(RolDeAcceso.DONANTE),
        Optional.empty(), Optional.empty());

    assertThrows(ForbiddenResponse.class, () -> Acceso.perfilDonanteObligatorio(sinPerfil));
  }

  @Test
  void unDonanteVeSusNotificacionesYNoLasDeOtro() throws Exception {
    Donante donanteAna = donanteConId("Ana", "11111111", 10L);
    Donante donanteLuis = donanteConId("Luis", "22222222", 20L);

    UsuarioActual usuario = usuarioDonante("kc-ana", "ana@mail.com", donanteAna);

    Notificacion propia = Notificacion.paraDonante(
        donanteAna, TipoNotificacion.CAMBIO_ESTADO, "mia", null);
    Notificacion ajena = Notificacion.paraDonante(
        donanteLuis, TipoNotificacion.CAMBIO_ESTADO, "ajena", null);

    assertDoesNotThrow(() -> Acceso.exigirPuedeVerNotificacion(usuario, propia));
    assertThrows(ForbiddenResponse.class,
        () -> Acceso.exigirPuedeVerNotificacion(usuario, ajena));
  }

  @Test
  void unaBeneficiariaVeSusNotificaciones() throws Exception {
    Beneficiaria propia = beneficiariaConId("30-11111111-1", 100L);
    Beneficiaria otra = beneficiariaConId("30-22222222-2", 200L);

    UsuarioActual usuario = usuarioBeneficiaria("kc-benef", "benef@mail.com", propia);

    Notificacion mia = Notificacion.paraBeneficiaria(
        propia, TipoNotificacion.DONACION_ASIGNADA, "mia", null);
    Notificacion otraN = Notificacion.paraBeneficiaria(
        otra, TipoNotificacion.DONACION_ASIGNADA, "otra", null);

    assertDoesNotThrow(() -> Acceso.exigirPuedeVerNotificacion(usuario, mia));
    assertThrows(ForbiddenResponse.class,
        () -> Acceso.exigirPuedeVerNotificacion(usuario, otraN));
  }

  @Test
  void lasDeAdministradoresSoloLasVenAdmins() throws Exception {
    Donante donante = donanteConId("Ana", "11111111", 10L);
    UsuarioActual admin = new UsuarioActual(
        "kc-admin", "admin@mail.com",
        Set.of(RolDeAcceso.ADMINISTRADOR),
        Optional.empty(), Optional.empty());
    UsuarioActual donanteUsuario = usuarioDonante("kc-d", "d@mail.com", donante);

    Notificacion paraAdmins = Notificacion.paraAdministradores(
        TipoNotificacion.ENTREGA_FALLIDA, "ups", null);

    assertDoesNotThrow(() -> Acceso.exigirPuedeVerNotificacion(admin, paraAdmins));
    assertThrows(ForbiddenResponse.class,
        () -> Acceso.exigirPuedeVerNotificacion(donanteUsuario, paraAdmins));
  }

  @Test
  void unaBeneficiariaVeSusNecesidadesYNoLasDeOtra() throws Exception {
    Beneficiaria propia = beneficiariaConId("30-11111111-1", 100L);
    Beneficiaria otra = beneficiariaConId("30-22222222-2", 200L);

    UsuarioActual usuario = usuarioBeneficiaria("kc-benef", "benef@mail.com", propia);

    Subcategoria sub = new Subcategoria("Sillas", new Categoria("Mobiliario"), Unidades.UNIDADES);
    Necesidad miNecesidad = new NecesidadExtraordinaria("Sillas", 10, sub, propia);
    Necesidad necesidadAjena = new NecesidadExtraordinaria("Sillas", 5, sub, otra);

    assertDoesNotThrow(() -> Acceso.exigirPuedeVerNecesidad(usuario, miNecesidad));
    assertThrows(ForbiddenResponse.class, () -> Acceso.exigirPuedeVerNecesidad(usuario, necesidadAjena));
  }

  // === Helpers ===

  private UsuarioActual usuarioDonante(String kcId, String email, Donante donante) {
    return new UsuarioActual(
        kcId, email, Set.of(RolDeAcceso.DONANTE),
        Optional.of(donante), Optional.empty());
  }

  private UsuarioActual usuarioBeneficiaria(String kcId, String email, Beneficiaria beneficiaria) {
    return new UsuarioActual(
        kcId, email, Set.of(RolDeAcceso.BENEFICIARIA),
        Optional.empty(), Optional.of(beneficiaria));
  }

  private Donacion donacionDe(Donante donante) {
    Subcategoria sub = new Subcategoria("Sillas", new Categoria("Mobiliario"), Unidades.UNIDADES);
    Bien silla = new Bien("Silla", sub, 1, CondicionBien.USADO);
    return new Donacion(List.of(silla), donante, "test");
  }

  private Donante donanteConId(String nombre, String documento, long id) throws Exception {
    Donante donante = new Donante(new PersonaHumana(nombre, "Test", 30, documento, Genero.FEMENINO));
    setId(donante, "id", id);
    return donante;
  }

  private Beneficiaria beneficiariaConId(String cuit, long id) throws Exception {
    Beneficiaria beneficiaria = new Beneficiaria(
        new PersonaJuridica(cuit, "Test " + cuit, TipoOrganizacion.ONG, "Rubro")
    );
    setId(beneficiaria, "id", id);
    return beneficiaria;
  }

  // Los ids se autogeneran en la DB, pero en un test unitario sin DB los
  // forzamos por reflexion para comparar por id en Acceso.
  private void setId(Object objeto, String nombreCampo, long valor) throws Exception {
    Field campo = objeto.getClass().getDeclaredField(nombreCampo);
    campo.setAccessible(true);
    campo.set(objeto, valor);
  }
}