package donatrack.model.entidad;

import donatrack.model.catalogo.Subcategoria;
import donatrack.model.necesidad.Necesidad;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EntidadBeneficiariaTest {

  @Test
  public void registrarNecesidadAgregaCorrectamenteALaLista() {
    EntidadBeneficiaria escuela = entidad("Escuela Parroquial San Jose");
    escuela.registrarNecesidades(necesidadBancosYSillas(30));

    assertEquals(1, escuela.getNecesidades().size());
    assertEquals("Reposicion tras inundacion", escuela.getNecesidades().get(0).getDescripcion());
    assertEquals(30, escuela.getNecesidades().get(0).getCantidad());
  }

  @Test
  public void entidadSeCreaConListaDeNecesidadesVacia() {
    EntidadBeneficiaria comedor = entidad("Comedor de Piru");
    assertTrue(comedor.getNecesidades().isEmpty());
  }

  private EntidadBeneficiaria entidad(String nombre) {
    return new EntidadBeneficiaria(nombre);
  }

  private Necesidad necesidadBancosYSillas(int cantidad) {
    return new Necesidad("Reposicion tras inundacion", cantidad, new Subcategoria("Bancos y Sillas escolares"));
  }
}
