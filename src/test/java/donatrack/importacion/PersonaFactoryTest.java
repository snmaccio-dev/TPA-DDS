package donatrack.importacion;

import donatrack.model.persona.Persona;
import donatrack.model.persona.PersonaHumana;
import donatrack.model.persona.PersonaJuridica;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PersonaFactoryTest {

  @Test
  void crearGeneraPersonaHumanaCorrectamente() {
    // Arrange: Simulamos una fila de CSV correspondiente a una persona humana
    String[] campos = {"HUMANA", "DNI", "12345678", "Ana Perez", "ana@mail.com", "+54 11 5555-5555"};

    // Act: Le pedimos al Factory que la procese
    Persona persona = PersonaFactory.crear(campos);

    // Assert: Verificamos que el tipo de objeto sea el correcto
    assertTrue(persona instanceof PersonaHumana, "Debería crear una instancia de PersonaHumana");

    // Verificamos que el email se haya cargado bien en el contacto
    assertEquals("ana@mail.com", persona.getContactoPredeterminado().getValor(),
        "El email predeterminado debería coincidir con la fila del CSV");
  }

  @Test
  void crearGeneraPersonaJuridicaCorrectamente() {
    // Arrange: Simulamos una fila de CSV correspondiente a una empresa
    String[] campos = {"JURIDICA", "CUIT", "30-12345678-9", "Arcos Plateados S.A.", "contacto@empresa.com", "+54 11 4444-4444"};

    // Act
    Persona persona = PersonaFactory.crear(campos);

    // Assert
    assertTrue(persona instanceof PersonaJuridica, "Debería crear una instancia de PersonaJuridica");
    assertEquals("contacto@empresa.com", persona.getContactoPredeterminado().getValor());
  }

  @Test
  void crearLanzaExcepcionConCamposInsuficientes() {
    // Arrange: Mandamos un array con solo 3 datos en lugar de 6
    String[] camposIncompletos = {"HUMANA", "DNI", "12345678"};

    // Act & Assert: Verificamos que el sistema salte y tire la excepcion de seguridad
    assertThrows(IllegalArgumentException.class, () -> {
      PersonaFactory.crear(camposIncompletos);
    }, "Debería fallar y lanzar IllegalArgumentException si la fila tiene menos de 6 campos");
  }

  @Test
  void crearLanzaExcepcionConTipoPersonaDesconocido() {
    // Arrange: Mandamos un tipo de organización que no existe
    String[] camposRaros = {"EXTRATERRESTRE", "DNI", "12345678", "ET", "et@espacio.com", "1111"};

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> {
      PersonaFactory.crear(camposRaros);
    }, "Debería fallar si el tipo de persona no es HUMANA ni JURIDICA");
  }
}
