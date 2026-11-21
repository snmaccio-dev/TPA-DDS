package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.DatosInvalidosException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReglasDePersonaTest {

  // === Documento ===

  @ParameterizedTest
  @ValueSource(strings = {"1234567", "12345678", " 12345678 "})
  void unDocumentoDe7u8DigitosEsValido(String documento) {
    assertEquals(documento.trim(), ReglasDePersona.documento(documento));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "123456", "123456789", "12.345.678", "1234567a"})
  void unDocumentoQueNoTiene7u8DigitosEsInvalido(String documento) {
    DatosInvalidosException error = assertThrows(DatosInvalidosException.class,
        () -> ReglasDePersona.documento(documento));
    assertTrue(error.getMessage().contains("documento"), error.getMessage());
  }

  // === Nombre y apellido ===

  @Test
  void unNombreSeNormalizaRecortandoYColapsandoEspacios() {
    assertEquals("Ana María", ReglasDePersona.nombre("El nombre", "  Ana    María "));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Ana", "Peña", "Müller", "O'Connor", "José Luis", "ÁÉÍÓÚ"})
  void unNombreConLetrasEspaciosYApostrofoEsValido(String nombre) {
    assertEquals(nombre, ReglasDePersona.nombre("El nombre", nombre));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "  ", "Al", "A'l", "Ana2", "Ana-María", "Ana.", "Ana_Luz"})
  void unNombreConMenosDe3LetrasOConOtrosSimbolosEsInvalido(String nombre) {
    DatosInvalidosException error = assertThrows(DatosInvalidosException.class,
        () -> ReglasDePersona.nombre("El apellido", nombre));
    assertTrue(error.getMessage().startsWith("El apellido"), error.getMessage());
  }

  // === Edad ===

  @Test
  void laEdadTieneQueSerAlMenos18() {
    assertEquals(18, ReglasDePersona.edad(18));
    assertThrows(DatosInvalidosException.class, () -> ReglasDePersona.edad(17));
    assertThrows(DatosInvalidosException.class, () -> ReglasDePersona.edad(null));
  }

  // === Correo ===

  @Test
  void unCorreoValidoSeGuardaEnMinusculas() {
    assertEquals("ana.perez@mail.com.ar",
        ReglasDePersona.email("El correo", " Ana.Perez@Mail.com.ar "));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "ana", "ana@mail", "ana@@mail.com", "ana perez@mail.com",
      "@mail.com", "ana@.com.", "ana@mail."})
  void unCorreoSinFormatoUsuarioDominioExtEsInvalido(String email) {
    DatosInvalidosException error = assertThrows(DatosInvalidosException.class,
        () -> ReglasDePersona.email("El correo del representante", email));
    assertTrue(error.getMessage().startsWith("El correo del representante"), error.getMessage());
  }

  // === Razon social ===

  @ParameterizedTest
  @ValueSource(strings = {"Escuela N° 14", "S.A. del Plata", "ONG 2024", "Abc"})
  void unaRazonSocialConAlMenos3LetrasEsValida(String razonSocial) {
    assertEquals(razonSocial, ReglasDePersona.razonSocial(razonSocial));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "S.A.", "12 34", "AB"})
  void unaRazonSocialConMenosDe3LetrasEsInvalida(String razonSocial) {
    assertThrows(DatosInvalidosException.class, () -> ReglasDePersona.razonSocial(razonSocial));
  }

  // === CUIT ===

  @Test
  void unCuitDe11DigitosConOSinGuionesSeGuardaSinGuiones() {
    assertEquals("30123456789", ReglasDePersona.cuit("30123456789"));
    assertEquals("30123456789", ReglasDePersona.cuit("30-12345678-9"));
    // No se valida el digito verificador ni el prefijo.
    assertEquals("99000000000", ReglasDePersona.cuit("99-00000000-0"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "3012345678", "301234567890", "30-1234567-89", "30 12345678 9",
      "30-12345678-9x"})
  void unCuitConOtroLargoOFormatoEsInvalido(String cuit) {
    DatosInvalidosException error = assertThrows(DatosInvalidosException.class,
        () -> ReglasDePersona.cuit(cuit));
    assertTrue(error.getMessage().contains("CUIT"), error.getMessage());
  }

  // === Telefono ===

  @ParameterizedTest
  @ValueSource(strings = {"11491110", "1149111001", "541149111001234"})
  void unTelefonoDe8a15DigitosEsValido(String telefono) {
    assertEquals(telefono, ReglasDePersona.telefono("El teléfono", telefono));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "1149111", "5411491110012345", "+541149111001", "11-4911-1001",
      "11 4911 1001"})
  void unTelefonoConSimbolosOFueraDeRangoEsInvalido(String telefono) {
    DatosInvalidosException error = assertThrows(DatosInvalidosException.class,
        () -> ReglasDePersona.telefono("El teléfono", telefono));
    assertEquals("El teléfono tiene que tener entre 8 y 15 dígitos, solo números.",
        error.getMessage());
  }

  // === Representante ===

  @Test
  void unRepresentanteNuevoTieneQueTenerNombreApellidoYCorreoValidos() {
    Representante valido = Representante.nuevo(" Marta ", "Quiroga", "Marta@Hogar.org");
    assertEquals("Marta", valido.getNombre());
    assertEquals("marta@hogar.org", valido.getEmail());

    assertThrows(DatosInvalidosException.class,
        () -> Representante.nuevo(null, "Quiroga", "marta@hogar.org"));
    assertThrows(DatosInvalidosException.class,
        () -> Representante.nuevo("Marta", "Q1", "marta@hogar.org"));
    assertThrows(DatosInvalidosException.class,
        () -> Representante.nuevo("Marta", "Quiroga", "marta@hogar"));
  }

  @Test
  void personaJuridicaNuevaValidaElCuitYLaRazonSocial() {
    PersonaJuridica juridica =
        PersonaJuridica.nueva("30-70000001-1", "Comedor Los Gorriones", null, "Comedor");
    assertEquals("30700000011", juridica.getCuit());

    assertThrows(DatosInvalidosException.class,
        () -> PersonaJuridica.nueva("30-7000-1", "Comedor", null, null));
    assertThrows(DatosInvalidosException.class,
        () -> PersonaJuridica.nueva("30700000011", "S.A.", null, null));
  }
}
