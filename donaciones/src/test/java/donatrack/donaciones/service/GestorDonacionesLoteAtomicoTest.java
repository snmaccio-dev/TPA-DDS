package donatrack.donaciones.service;

import donatrack.donaciones.DatosDeTest;
import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.catalogo.Unidades;
import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.CondicionBien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.estado.EstadosPosiblesDonacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioPropuestas;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

// Test enfocado en que aplicarEventoEnLote sea "todo o nada": no lo hacemos
// con SimplePersistenceTest porque necesitamos que la transaccion del gestor
// se abra, commitee y haga rollback de verdad. Manejamos nosotros la tx para
// armar los datos previos.
class GestorDonacionesLoteAtomicoTest implements WithSimplePersistenceUnit {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioDonantes repoDonantes = new RepositorioDonantes();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();
  private final RepositorioDonaciones repoDonaciones = RepositorioDonaciones.getInstance();

  @Test
  void siLaSegundaTransicionFallaElLoteSeRevierteYLaPrimeraNoCambia() {
    GestorDonaciones gestor = new GestorDonaciones(
        mock(GestorNotificaciones.class),
        new RepositorioPropuestas(),
        mock(RepositorioNotificaciones.class),
        "http://web/donante",
        "http://web/beneficiario"
    );

    long[] ids = new long[2];
    withTransaction(() -> {
      // Armamos dos donaciones ya asignadas a una beneficiaria, para que
      // marcarListaParaEntregar sea una transicion valida desde ASIGNACION_REALIZADA.
      Beneficiaria beneficiaria = beneficiariaPersistida();
      Donante donante = donantePersistido();
      Donacion donacion1 = donacionAsignadaA(donante, beneficiaria);
      Donacion donacion2 = donacionAsignadaA(donante, beneficiaria);
      ids[0] = donacion1.getId();
      ids[1] = donacion2.getId();
    });

    // Transicion que marca la primera como LISTA_PARA_ENTREGAR y lanza en la
    // segunda. Si el withTransaction del lote estuviera comiteando al salir
    // de una llamada anidada, la primera ya hubiera quedado persistida; el
    // rollback de aca tiene que revertirla.
    Consumer<Donacion> fallaEnLaSegunda = new Consumer<>() {
      private int llamadas = 0;

      @Override
      public void accept(Donacion donacion) {
        llamadas++;
        if (llamadas == 1) {
          donacion.marcarListaParaEntregar();
        } else {
          throw new RuntimeException("Simulacion de falla en la segunda donacion.");
        }
      }
    };

    assertThrows(RuntimeException.class, () ->
        gestor.aplicarEventoEnLote(
            List.of(ids[0], ids[1]),
            EstadosPosiblesDonacion.LISTA_PARA_ENTREGAR,
            List.of(EstadosPosiblesDonacion.ASIGNACION_REALIZADA),
            fallaEnLaSegunda
        ));

    // La primera no debe haber cambiado en la base: el rollback del lote
    // la devolvio a ASIGNACION_REALIZADA.
    entityManager().clear();
    Donacion recargadaPrimera = entityManager().find(Donacion.class, ids[0]);
    assertEquals(EstadosPosiblesDonacion.ASIGNACION_REALIZADA,
        recargadaPrimera.getEstado().getEstado(),
        "Si la segunda tirara, la primera no debe quedar committeada.");
  }

  // === Helpers ===

  private Beneficiaria beneficiariaPersistida() {
    PersonaJuridica juridica = new PersonaJuridica(
        DatosDeTest.cuitUnico(), DatosDeTest.textoUnico("Benef"),
        TipoOrganizacion.ONG, "Rubro");
    repoPersonas.guardar(juridica);
    Beneficiaria beneficiaria = new Beneficiaria(juridica);
    repoEntidades.guardar(beneficiaria);
    return beneficiaria;
  }

  private Donante donantePersistido() {
    PersonaHumana persona = new PersonaHumana(
        "D", "onante", 30, DatosDeTest.documentoUnico(), Genero.FEMENINO);
    repoPersonas.guardar(persona);
    Donante donante = new Donante(persona);
    repoDonantes.guardar(donante);
    return donante;
  }

  private Donacion donacionAsignadaA(Donante donante, Beneficiaria beneficiaria) {
    Categoria cat = new Categoria(DatosDeTest.textoUnico("Cat"));
    entityManager().persist(cat);
    Subcategoria sub = new Subcategoria(
        DatosDeTest.textoUnico("Sub"), cat, Unidades.UNIDADES);
    entityManager().persist(sub);
    Bien bien = new Bien("Silla", sub, 1, CondicionBien.USADO);
    Donacion donacion = new Donacion(List.of(bien), donante, "demo");
    donacion.confirmarDestino(beneficiaria);
    repoDonaciones.guardar(donacion);
    return donacion;
  }
}
