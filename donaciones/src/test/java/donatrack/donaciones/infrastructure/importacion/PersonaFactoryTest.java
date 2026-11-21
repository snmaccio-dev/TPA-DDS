package donatrack.donaciones.infrastructure.importacion;

import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonaFactoryTest {

  @Test
  void creaUnaPersonaHumanaConEmailYTelefono() {
    Persona persona = PersonaFactory.crear(new FilaDonanteCsv(2, FilaDonanteCsv.TipoPersona.HUMANA,
        "DNI", "12345678", "Ana Perez Gomez", "ana@mail.com", "1155555555"));

    PersonaHumana humana = assertInstanceOf(PersonaHumana.class, persona);
    assertEquals("Ana", humana.getNombre());
    assertEquals("Perez Gomez", humana.getApellido());
    assertEquals("ana@mail.com", persona.getContactoPredeterminado().getValor());
    assertTrue(persona.tieneContacto(TipoContacto.TELEFONO, "1155555555"));
  }

  @Test
  void creaUnaPersonaJuridicaConElCuitNormalizado() {
    Persona persona = PersonaFactory.crear(new FilaDonanteCsv(2, FilaDonanteCsv.TipoPersona.JURIDICA,
        "CUIT", "30-12345678-9", "Arcos Plateados S.A.", "contacto@arcos.com", ""));

    PersonaJuridica juridica = assertInstanceOf(PersonaJuridica.class, persona);
    assertEquals("30123456789", juridica.getCuit());
    assertEquals("Arcos Plateados S.A.", juridica.getRazonSocial());
    assertFalse(persona.tieneContactoDeTipo(TipoContacto.TELEFONO));
  }

  @Test
  void separarNombreSinApellidoDejaElApellidoVacio() {
    PersonaFactory.NombreYApellido nombre = PersonaFactory.separarNombre("Cher");

    assertEquals("Cher", nombre.nombre());
    assertEquals("", nombre.apellido());
  }
}
