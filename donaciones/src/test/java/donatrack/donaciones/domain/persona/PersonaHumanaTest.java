package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.DatosInvalidosException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PersonaHumanaTest {

  @Test
  void actualizarDatosCambiaNombreApellidoEdadYGenero() {
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, "11111111", Genero.FEMENINO);

    persona.actualizarDatos(" Ana Maria ", " Gomez ", 31, Genero.OTRO);

    assertEquals("Ana Maria", persona.getNombre());
    assertEquals("Gomez", persona.getApellido());
    assertEquals(31, persona.getEdad());
    assertEquals(Genero.OTRO, persona.getGenero());
    assertEquals("11111111", persona.getDocumento());
  }

  @Test
  void actualizarDatosAceptaEdadCero() {
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, "11111111", Genero.FEMENINO);

    persona.actualizarDatos("Ana", "Perez", 0, Genero.FEMENINO);

    assertEquals(0, persona.getEdad());
  }

  @Test
  void actualizarDatosSinNombreFallaYNoCambiaNada() {
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, "11111111", Genero.FEMENINO);

    assertThrows(DatosInvalidosException.class, () ->
        persona.actualizarDatos(" ", "Gomez", 40, Genero.OTRO));

    assertEquals("Ana", persona.getNombre());
    assertEquals("Perez", persona.getApellido());
    assertEquals(30, persona.getEdad());
  }

  @Test
  void actualizarDatosSinApellidoFalla() {
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, "11111111", Genero.FEMENINO);

    assertThrows(DatosInvalidosException.class, () ->
        persona.actualizarDatos("Ana", null, 30, Genero.FEMENINO));
  }

  @Test
  void actualizarDatosConEdadNegativaOFaltanteFalla() {
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, "11111111", Genero.FEMENINO);

    assertThrows(DatosInvalidosException.class, () ->
        persona.actualizarDatos("Ana", "Perez", -1, Genero.FEMENINO));
    assertThrows(DatosInvalidosException.class, () ->
        persona.actualizarDatos("Ana", "Perez", null, Genero.FEMENINO));
    assertEquals(30, persona.getEdad());
  }

  @Test
  void actualizarNombrePermiteApellidoVacio() {
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, "11111111", Genero.FEMENINO);

    persona.actualizarNombre("Cher", "");

    assertEquals("Cher", persona.getNombre());
    assertEquals("", persona.getApellido());
  }
}
