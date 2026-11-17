package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonaJuridicaTest {

  @Test
  void actualizarDatosCambiaRazonSocialTipoYRubroPeroNoElCuit() {
    PersonaJuridica juridica = juridica();

    juridica.actualizarDatos(" Comedor Nuevo ", TipoOrganizacion.ONG, " Alimentos ");

    assertEquals("Comedor Nuevo", juridica.getRazonSocial());
    assertEquals(TipoOrganizacion.ONG, juridica.getTipo());
    assertEquals("Alimentos", juridica.getRubro());
    assertEquals("30111111111", juridica.getCuit());
  }

  @Test
  void actualizarDatosPermiteTipoYRubroNulos() {
    PersonaJuridica juridica = juridica();

    juridica.actualizarDatos("Comedor", null, null);

    assertNull(juridica.getTipo());
    assertNull(juridica.getRubro());
  }

  @Test
  void actualizarDatosSinRazonSocialFallaYNoCambiaNada() {
    PersonaJuridica juridica = juridica();

    assertThrows(DatosInvalidosException.class, () ->
        juridica.actualizarDatos(" ", TipoOrganizacion.ONG, "Otro"));

    assertEquals("Escuela", juridica.getRazonSocial());
    assertEquals(TipoOrganizacion.INSTITUCION, juridica.getTipo());
    assertEquals("Educacion", juridica.getRubro());
  }

  @Test
  void reemplazarRepresentantesReemplazaLaListaYAjustaLosEmails() {
    PersonaJuridica juridica = juridica();
    juridica.agregarMedioContacto(new MedioContacto(TipoContacto.TELEFONO, "111"));
    juridica.agregarRepresentante(new Representante("Ana", "Uno", "ana@escuela.com"));
    juridica.agregarRepresentante(new Representante("Luis", "Dos", "luis@escuela.com"));

    juridica.reemplazarRepresentantes(List.of(
        new Representante("Luis", "Dos", "luis@escuela.com"),
        new Representante("Eva", "Tres", "eva@escuela.com")));

    assertEquals(List.of("luis@escuela.com", "eva@escuela.com"),
        juridica.getRepresentantes().stream().map(Representante::getEmail).toList());
    assertFalse(juridica.tieneContacto(TipoContacto.EMAIL, "ana@escuela.com"));
    assertTrue(juridica.tieneContacto(TipoContacto.EMAIL, "luis@escuela.com"));
    assertTrue(juridica.tieneContacto(TipoContacto.EMAIL, "eva@escuela.com"));
    assertTrue(juridica.tieneContacto(TipoContacto.TELEFONO, "111"));
  }

  @Test
  void reemplazarRepresentantesNoQuitaElEmailPredeterminado() {
    PersonaJuridica juridica = juridica();
    juridica.agregarRepresentante(new Representante("Ana", "Uno", "ana@escuela.com"));
    assertEquals("ana@escuela.com", juridica.getContactoPredeterminado().getValor());

    juridica.reemplazarRepresentantes(List.of(new Representante("Eva", "Tres", "eva@escuela.com")));

    assertTrue(juridica.tieneContacto(TipoContacto.EMAIL, "ana@escuela.com"));
    assertEquals("ana@escuela.com", juridica.getContactoPredeterminado().getValor());
    assertEquals(List.of("eva@escuela.com"),
        juridica.getRepresentantes().stream().map(Representante::getEmail).toList());
  }

  @Test
  void reemplazarRepresentantesNoQuitaEmailsQueNoEranDeRepresentantes() {
    PersonaJuridica juridica = juridica();
    juridica.agregarMedioContacto(new MedioContacto(TipoContacto.TELEFONO, "111"));
    juridica.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "cuenta@escuela.com"));
    juridica.agregarRepresentante(new Representante("Ana", "Uno", "ana@escuela.com"));

    juridica.reemplazarRepresentantes(List.of(new Representante("Eva", "Tres", "eva@escuela.com")));

    assertTrue(juridica.tieneContacto(TipoContacto.EMAIL, "cuenta@escuela.com"));
    assertFalse(juridica.tieneContacto(TipoContacto.EMAIL, "ana@escuela.com"));
  }

  @Test
  void reemplazarRepresentantesSinNingunoFallaYNoCambiaNada() {
    PersonaJuridica juridica = juridica();
    juridica.agregarRepresentante(new Representante("Ana", "Uno", "ana@escuela.com"));

    assertThrows(DatosInvalidosException.class, () -> juridica.reemplazarRepresentantes(List.of()));
    assertThrows(DatosInvalidosException.class, () -> juridica.reemplazarRepresentantes(null));

    assertEquals(1, juridica.getRepresentantes().size());
  }

  private PersonaJuridica juridica() {
    return new PersonaJuridica("30-11111111-1", "Escuela", TipoOrganizacion.INSTITUCION, "Educacion");
  }
}
