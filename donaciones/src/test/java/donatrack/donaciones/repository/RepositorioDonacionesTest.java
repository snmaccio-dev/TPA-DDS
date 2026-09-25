package donatrack.donaciones.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class RepositorioDonacionesTest implements SimplePersistenceTest {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();
  private final RepositorioDonaciones repositorio = RepositorioDonaciones.getInstance();

  @Test
  void unaDonacionGuardadaVuelveConSusDosBienes() {
    Donacion donacion = crearDonacionConDosBienes();
    repositorio.guardar(donacion);

    entityManager().flush();
    entityManager().clear();

    Donacion recuperada = repositorio.buscarPorId(donacion.getId()).orElseThrow();

    assertEquals(2, recuperada.getBienes().size());
  }

  @Test
  void unaDonacionGuardadaVuelveEnEnDepositoConHistorialDeUnElemento() {
    Donacion donacion = crearDonacionConDosBienes();
    repositorio.guardar(donacion);

    entityManager().flush();
    entityManager().clear();

    Donacion recuperada = repositorio.buscarPorId(donacion.getId()).orElseThrow();

    assertEquals(EstadosPosiblesDonacion.EN_DEPOSITO, recuperada.getEstado().getEstado());
    assertEquals(1, recuperada.getHistorialEstados().size());
  }

  @Test
  void enEstadoDevuelveLasDeEseEstadoYNoLasDeOtro() {
    Donacion donacion = crearDonacionConDosBienes();
    repositorio.guardar(donacion);

    entityManager().flush();
    entityManager().clear();

    List<Donacion> enDeposito = repositorio.enEstado(EstadosPosiblesDonacion.EN_DEPOSITO);
    List<Donacion> entregadas = repositorio.enEstado(EstadosPosiblesDonacion.ENTREGADA);

    assertEquals(1, enDeposito.size());
    assertTrue(entregadas.isEmpty());
  }

  @Test
  void despuesDeGuardarElIdNoEsNullYCantidadDaUno() {
    Donacion donacion = crearDonacionConDosBienes();
    repositorio.guardar(donacion);

    entityManager().flush();

    assertNotNull(donacion.getId());
    assertEquals(1, repositorio.cantidad());
  }

  private Donacion crearDonacionConDosBienes() {
    Categoria mobiliario = new Categoria("Mobiliario");
    entityManager().persist(mobiliario);
    Subcategoria sillas = new Subcategoria("Sillas", mobiliario, Unidades.UNIDADES);
    entityManager().persist(sillas);

    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "12345678", Genero.FEMENINO);
    repoPersonas.guardar(ana);
    Donante donante = new Donante(ana);
    entityManager().persist(donante);

    Bien silla1 = new Bien("Silla oficina 1", sillas, 1, CondicionBien.USADO);
    Bien silla2 = new Bien("Silla oficina 2", sillas, 1, CondicionBien.USADO);
    return Donacion.crear(List.of(silla1, silla2), donante, "Sillas de mudanza");
  }

  @Test
  void unaDonacionEntregadaVuelveConSusDatosDeEntrega() {
    PersonaJuridica escuelaOrg = new PersonaJuridica(
        "30-77777777-1", "Escuela Rural N10", TipoOrganizacion.INSTITUCION, "Educacion"
    );
    repoPersonas.guardar(escuelaOrg);
    Beneficiaria escuela = new Beneficiaria(escuelaOrg);
    repoEntidades.guardar(escuela);

    Donacion donacion = crearDonacionConDosBienes();
    donacion.confirmarDestino(escuela);
    donacion.marcarListaParaEntregar();
    donacion.marcarEnTraslado();
    LocalDateTime fechaEntrega = LocalDateTime.of(2026, 3, 15, 10, 30);
    donacion.marcarEntregada(fechaEntrega, "AAA111");
    repositorio.guardar(donacion);

    entityManager().flush();
    entityManager().clear();

    Donacion recuperada = repositorio.buscarPorId(donacion.getId()).orElseThrow();

    assertEquals(EstadosPosiblesDonacion.ENTREGADA, recuperada.getEstado().getEstado());
    assertEquals(fechaEntrega, recuperada.getDatosEntrega().getFechaHora());
    assertEquals("AAA111", recuperada.getDatosEntrega().getPatenteCamion());
    assertEquals(5, recuperada.getHistorialEstados().size());
  }
}
