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
  void actualizarDatosConservaLaEdadDeUnRegistroViejoSiNoCambia() {
    // Importado por CSV: sin edad (0).
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 0, "11111111", Genero.FEMENINO);

    persona.actualizarDatos("Ana", "Gomez", 0, Genero.FEMENINO);

    assertEquals(0, persona.getEdad());
    assertEquals("Gomez", persona.getApellido());
  }

  @Test
  void actualizarDatosRechazaUnaEdadNuevaMenorA18() {
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, "11111111", Genero.FEMENINO);

    assertThrows(DatosInvalidosException.class, () ->
        persona.actualizarDatos("Ana", "Perez", 17, Genero.FEMENINO));
    assertEquals(30, persona.getEdad());
  }

  @Test
  void actualizarDatosValidaSoloLosCamposQueCambian() {
    // Registro viejo con un nombre que hoy no cumple las reglas.
    PersonaHumana persona = new PersonaHumana("X", "Perez", 30, "11111111", Genero.FEMENINO);

    persona.actualizarDatos("X", "Perez", 31, Genero.FEMENINO);
    assertEquals(31, persona.getEdad());

    assertThrows(DatosInvalidosException.class, () ->
        persona.actualizarDatos("Al", "Perez", 31, Genero.FEMENINO));
    assertEquals("X", persona.getNombre());
  }

  @Test
  void actualizarDatosRechazaUnApellidoConNumeros() {
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, "11111111", Genero.FEMENINO);

    assertThrows(DatosInvalidosException.class, () ->
        persona.actualizarDatos("Ana", "Perez2", 30, Genero.FEMENINO));
  }

  @Test
  void nuevaValidaYNormalizaLosDatos() {
    PersonaHumana persona =
        PersonaHumana.nueva("  María   José ", "O'Connor", 18, "1234567", Genero.FEMENINO);

    assertEquals("María José", persona.getNombre());
    assertEquals("O'Connor", persona.getApellido());
    assertEquals("1234567", persona.getDocumento());
  }

  @Test
  void nuevaRechazaMenoresDeEdadYDocumentosInvalidos() {
    assertThrows(DatosInvalidosException.class, () ->
        PersonaHumana.nueva("Ana", "Perez", 17, "12345678", Genero.FEMENINO));
    assertThrows(DatosInvalidosException.class, () ->
        PersonaHumana.nueva("Ana", "Perez", null, "12345678", Genero.FEMENINO));
    assertThrows(DatosInvalidosException.class, () ->
        PersonaHumana.nueva("Ana", "Perez", 30, "12.345.678", Genero.FEMENINO));
  }

  @Test
  void importadaNoPideEdad() {
    PersonaHumana persona = PersonaHumana.importada("Ana", "Perez", "12345678");

    assertEquals(0, persona.getEdad());
    assertEquals(Genero.OTRO, persona.getGenero());
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
