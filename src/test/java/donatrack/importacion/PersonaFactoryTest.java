package donatrack.importacion;

import donatrack.model.persona.Persona;
import donatrack.model.persona.PersonaHumana;
import donatrack.model.persona.PersonaJuridica;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PersonaFactoryTest {

  @Test
  public void crearGeneraPersonaHumanaCorrectamente() {
    Persona persona = PersonaFactory.crear(camposCSVHumana());

    assertTrue(persona instanceof PersonaHumana);
    assertEquals("ana@mail.com", persona.getContactoPredeterminado().getValor());
  }

  @Test
  public void crearGeneraPersonaJuridicaCorrectamente() {
    Persona persona = PersonaFactory.crear(camposCSVJuridica());

    assertTrue(persona instanceof PersonaJuridica);
    assertEquals("contacto@empresa.com", persona.getContactoPredeterminado().getValor());
  }

  @Test
  public void crearLanzaExcepcionConCamposInsuficientes() {
    assertThrows(IllegalArgumentException.class, () -> {
      PersonaFactory.crear(camposIncompletos());
    });
  }

  @Test
  public void crearLanzaExcepcionConTipoPersonaDesconocido() {
    assertThrows(IllegalArgumentException.class, () -> {
      PersonaFactory.crear(camposRaros());
    });
  }

  private String[] camposCSVHumana() {
    return new String[]{"HUMANA", "DNI", "12345678", "Ana Perez", "ana@mail.com", "+54 11 5555-5555"};
  }

  private String[] camposCSVJuridica() {
    return new String[]{"JURIDICA", "CUIT", "30-12345678-9", "Arcos Plateados S.A.", "contacto@empresa.com", "+54 11 4444-4444"};
  }

  private String[] camposIncompletos() {
    return new String[]{"HUMANA", "DNI", "12345678"};
  }

  private String[] camposRaros() {
    return new String[]{"EXTRATERRESTRE", "DNI", "12345678", "ET", "et@espacio.com", "1111"};
  }
}