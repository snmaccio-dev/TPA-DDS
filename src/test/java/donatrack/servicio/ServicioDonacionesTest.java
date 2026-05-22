package donatrack.servicio;

import donatrack.model.catalogo.Subcategoria;
import donatrack.model.donacion.Bien;
import donatrack.model.donacion.Donacion;
import donatrack.model.donacion.Unidades;
import donatrack.model.persona.PersonaJuridica;
import donatrack.model.persona.TipoOrganizacion;
import donatrack.repositorio.RepositorioDonaciones;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ServicioDonacionesTest {

  private ServicioDonaciones servicio;
  private PersonaJuridica donante;
  private Subcategoria sillas;
  private Subcategoria fideos;

  @BeforeEach
  void setUp() {
    // 1. Instanciamos el servicio y los datos base
    servicio = new ServicioDonaciones();
    donante = new PersonaJuridica("Arcos Plateados", TipoOrganizacion.EMPRESA, "Mudanza");
    sillas = new Subcategoria("Sillas");
    fideos = new Subcategoria("Fideos");
  }

  @Test
  void ingresarDonacionSegmentaCorrectamentePorSubcategoria() {
    // Arrange: Preparamos 3 bienes, pero que corresponden a solo 2 subcategorías
    List<Bien> bienes = List.of(
        new Bien("Silla usada de oficina 1", sillas, Unidades.UNIDADES),
        new Bien("Silla usada de oficina 2", sillas, Unidades.UNIDADES),
        new Bien("Paquete fideos 500g", fideos, Unidades.KILOGRAMOS)
    );

    // Act: El servicio procesa la carga
    List<Donacion> resultado = servicio.ingresarDonacion(bienes, donante);

    // Assert: Se deben generar exactamente 2 donaciones independientes
    assertEquals(2, resultado.size(), "El servicio debería haber creado 2 donaciones separadas");
  }
}