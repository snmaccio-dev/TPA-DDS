package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonaContactosTest {

  @Test
  void reemplazarContactoConValorCreaElContactoSiNoExistia() {
    PersonaHumana persona = personaConEmail();

    persona.reemplazarContacto(TipoContacto.WHATSAPP, " 1111111111 ");

    assertEquals(List.of("1111111111"), valores(persona, TipoContacto.WHATSAPP));
    assertEquals(TipoContacto.EMAIL, persona.getContactoPredeterminado().getTipo());
  }

  @Test
  void reemplazarContactoConValorReemplazaElExistenteYDejaUnoSolo() {
    PersonaHumana persona = personaConEmail();
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.TELEFONO, "111"));
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.TELEFONO, "222"));

    persona.reemplazarContacto(TipoContacto.TELEFONO, "333");

    assertEquals(List.of("333"), valores(persona, TipoContacto.TELEFONO));
    assertEquals(2, persona.getContactos().size());
  }

  @Test
  void reemplazarContactoConservaElPredeterminadoSiEraDeEseTipo() {
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, "1", Genero.FEMENINO);
    MedioContacto telefono = new MedioContacto(TipoContacto.TELEFONO, "111");
    persona.agregarMedioContacto(telefono);

    persona.reemplazarContacto(TipoContacto.TELEFONO, "999");

    assertSame(telefono, persona.getContactoPredeterminado());
    assertEquals("999", persona.getContactoPredeterminado().getValor());
  }

  @Test
  void reemplazarContactoVacioONuloQuitaLosDeEseTipo() {
    PersonaHumana persona = personaConEmail();
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.TELEFONO, "111"));
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.WHATSAPP, "222"));

    persona.reemplazarContacto(TipoContacto.TELEFONO, "");
    persona.reemplazarContacto(TipoContacto.WHATSAPP, null);

    assertFalse(persona.tieneContactoDeTipo(TipoContacto.TELEFONO));
    assertFalse(persona.tieneContactoDeTipo(TipoContacto.WHATSAPP));
    assertTrue(persona.tieneContacto(TipoContacto.EMAIL, "ana@mail.com"));
  }

  @Test
  void quitarElContactoPredeterminadoLoDejaSinPredeterminado() {
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, "1", Genero.FEMENINO);
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.TELEFONO, "111"));
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "ana@mail.com"));

    persona.reemplazarContacto(TipoContacto.TELEFONO, null);

    assertNull(persona.getContactoPredeterminado());
  }

  @Test
  void elegirPredeterminadoDeUnTipoQueLaPersonaTiene() {
    PersonaHumana persona = personaConEmail();
    persona.reemplazarContacto(TipoContacto.WHATSAPP, "222");

    persona.elegirContactoPredeterminado(TipoContacto.WHATSAPP);

    assertEquals(TipoContacto.WHATSAPP, persona.getContactoPredeterminado().getTipo());
    assertEquals("222", persona.getContactoPredeterminado().getValor());
  }

  @Test
  void elegirPredeterminadoDeUnTipoQueLaPersonaNoTieneFalla() {
    PersonaHumana persona = personaConEmail();

    assertThrows(DatosInvalidosException.class, () ->
        persona.elegirContactoPredeterminado(TipoContacto.TELEFONO));
    assertEquals(TipoContacto.EMAIL, persona.getContactoPredeterminado().getTipo());
  }

  @Test
  void sinTipoMantieneElPredeterminadoActual() {
    PersonaHumana persona = personaConEmail();
    MedioContacto email = persona.getContactoPredeterminado();

    persona.elegirContactoPredeterminado(null);

    assertSame(email, persona.getContactoPredeterminado());
  }

  @Test
  void sinTipoYSinPredeterminadoPorqueSeQuitoFalla() {
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, "1", Genero.FEMENINO);
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.TELEFONO, "111"));
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "ana@mail.com"));
    persona.reemplazarContacto(TipoContacto.TELEFONO, null);

    assertThrows(DatosInvalidosException.class, () ->
        persona.elegirContactoPredeterminado(null));
  }

  @Test
  void quitarElPredeterminadoYElegirOtroQueda() {
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, "1", Genero.FEMENINO);
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.TELEFONO, "111"));
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "ana@mail.com"));

    persona.reemplazarContacto(TipoContacto.TELEFONO, null);
    persona.elegirContactoPredeterminado(TipoContacto.EMAIL);

    assertEquals("ana@mail.com", persona.getContactoPredeterminado().getValor());
  }

  private PersonaHumana personaConEmail() {
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, "1", Genero.FEMENINO);
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "ana@mail.com"));
    return persona;
  }

  private List<String> valores(Persona persona, TipoContacto tipo) {
    return persona.getContactos().stream()
        .filter(c -> c.getTipo() == tipo)
        .map(MedioContacto::getValor)
        .toList();
  }
}
