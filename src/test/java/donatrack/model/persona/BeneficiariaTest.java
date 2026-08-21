package donatrack.model.persona;

import donatrack.model.catalogo.Categoria;
import donatrack.model.catalogo.Subcategoria;
import donatrack.model.necesidad.Necesidad;
import donatrack.model.necesidad.NecesidadExtraordinaria;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BeneficiariaTest {

  @Test
  public void registrarNecesidadAgregaCorrectamenteALaLista() {
    Beneficiaria escuela = beneficiaria("Escuela Parroquial San Jose");
    escuela.registrarNecesidad(necesidadBancosYSillas(30));

    assertEquals(1, escuela.getNecesidades().size());
    assertEquals("Reposicion tras inundacion", escuela.getNecesidades().get(0).getDescripcion());
    assertEquals(30, escuela.getNecesidades().get(0).getCantidad());
  }

  @Test
  public void beneficiariaSeCreaConListaDeNecesidadesVacia() {
    Beneficiaria comedor = beneficiaria("Comedor de Piru");
    assertTrue(comedor.getNecesidades().isEmpty());
  }

  @Test
  public void beneficiariaQuedaRegistradaComoRolEnLaPersonaJuridica() {
    PersonaJuridica organizacion = new PersonaJuridica("Fundacion X", TipoOrganizacion.ONG, "Asistencia");
    Beneficiaria rol = new Beneficiaria(organizacion);

    assertTrue(organizacion.tieneRol(Beneficiaria.class));
    assertSame(rol, organizacion.comoRol(Beneficiaria.class).orElseThrow());
    assertSame(organizacion, rol.getPersona());
  }

  @Test
  public void noPuedeAsignarseElMismoRolDosVeces() {
    PersonaJuridica organizacion = new PersonaJuridica("Fundacion X", TipoOrganizacion.ONG, "Asistencia");
    new Beneficiaria(organizacion);

    assertThrows(IllegalStateException.class, () -> new Beneficiaria(organizacion));
  }

  private Beneficiaria beneficiaria(String razonSocial) {
    PersonaJuridica organizacion = new PersonaJuridica(razonSocial, TipoOrganizacion.INSTITUCION, "Educacion");
    return new Beneficiaria(organizacion);
  }

  private Necesidad necesidadBancosYSillas(int cantidad) {
    return new NecesidadExtraordinaria("Reposicion tras inundacion", cantidad, new Subcategoria("Bancos y Sillas escolares", new Categoria("Mobiliario")));
  }
}
