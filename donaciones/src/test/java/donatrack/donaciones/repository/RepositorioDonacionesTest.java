package donatrack.donaciones.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import donatrack.donaciones.service.FiltroDonaciones;
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

  // === Tests del filtro combinable (buscar) ===

  @Test
  void buscarSinFiltroYSinDonanteNiBeneficiariaDevuelveTodas() {
    EscenarioFiltros esc = montarEscenarioFiltros();

    List<Donacion> todas = repositorio.buscar(filtroVacio(), null, null);

    assertEquals(esc.cantidadTotal, todas.size());
  }

  @Test
  void buscarPorEstadoFiltraPorEstado() {
    EscenarioFiltros esc = montarEscenarioFiltros();

    List<Donacion> enDeposito = repositorio.buscar(
        new FiltroDonaciones(EstadosPosiblesDonacion.EN_DEPOSITO, null, null), null, null);
    List<Donacion> asignadas = repositorio.buscar(
        new FiltroDonaciones(EstadosPosiblesDonacion.ASIGNACION_REALIZADA, null, null), null, null);
    List<Donacion> entregadas = repositorio.buscar(
        new FiltroDonaciones(EstadosPosiblesDonacion.ENTREGADA, null, null), null, null);

    // Ana tiene 3 donaciones en EN_DEPOSITO y Luis 2 ya asignadas a la escuela.
    assertEquals(3, enDeposito.size());
    assertEquals(2, asignadas.size());
    assertTrue(entregadas.isEmpty());
  }

  @Test
  void buscarPorCategoriaFiltraPorCategoria() {
    EscenarioFiltros esc = montarEscenarioFiltros();

    List<Donacion> mobiliario = repositorio.buscar(
        new FiltroDonaciones(null, esc.categoriaMobiliario, null), null, null);
    List<Donacion> alimentos = repositorio.buscar(
        new FiltroDonaciones(null, esc.categoriaAlimentos, null), null, null);
    List<Donacion> inexistente = repositorio.buscar(
        new FiltroDonaciones(null, "Categoria-No-Existe-" + System.nanoTime(), null), null, null);

    // Mobiliario: 2 de Sillas de Ana + 1 de Mesas y 1 de Sillas de Luis.
    assertEquals(4, mobiliario.size());
    assertEquals(1, alimentos.size());
    assertTrue(inexistente.isEmpty(), "Una categoria inexistente devuelve lista vacia.");
  }

  @Test
  void buscarPorSubcategoriaFiltraPorSubcategoria() {
    EscenarioFiltros esc = montarEscenarioFiltros();

    List<Donacion> sillas = repositorio.buscar(
        new FiltroDonaciones(null, null, List.of(esc.subSillas)), null, null);
    List<Donacion> inexistente = repositorio.buscar(
        new FiltroDonaciones(null, null, List.of("No-Existe-" + System.nanoTime())), null, null);

    // 2 de Ana y 1 de Luis.
    assertEquals(3, sillas.size());
    assertTrue(inexistente.isEmpty());
  }

  @Test
  void buscarPorVariasSubcategoriasAplicaIn() {
    EscenarioFiltros esc = montarEscenarioFiltros();

    List<Donacion> sillasYMesas = repositorio.buscar(
        new FiltroDonaciones(null, null, List.of(esc.subSillas, esc.subMesas)),
        null, null);

    // 3 de Sillas + 1 de Mesas.
    assertEquals(4, sillasYMesas.size());
  }

  @Test
  void buscarCombinaCategoriaYSubcategoriaEnAnd() {
    EscenarioFiltros esc = montarEscenarioFiltros();

    List<Donacion> mobiliarioYSillas = repositorio.buscar(
        new FiltroDonaciones(null, esc.categoriaMobiliario, List.of(esc.subSillas)),
        null, null);
    List<Donacion> alimentosYSillas = repositorio.buscar(
        new FiltroDonaciones(null, esc.categoriaAlimentos, List.of(esc.subSillas)),
        null, null);

    // Sillas pertenece a Mobiliario; AND limita a las 3 Sillas.
    assertEquals(3, mobiliarioYSillas.size());
    assertTrue(alimentosYSillas.isEmpty(),
        "Si categoria y subcategoria no coinciden, el AND descarta todo.");
  }

  @Test
  void buscarConDonanteNoDevuelveLasDeOtros() {
    EscenarioFiltros esc = montarEscenarioFiltros();

    List<Donacion> deAna = repositorio.buscar(filtroVacio(), esc.donanteAna, null);

    assertEquals(3, deAna.size());
    assertTrue(deAna.stream().allMatch(d -> d.getDonante().getId().equals(esc.donanteAna.getId())));
    assertFalse(deAna.stream().anyMatch(d -> d.getDonante().getId().equals(esc.donanteLuis.getId())));
  }

  @Test
  void buscarConBeneficiariaNoDevuelveLasDeOtraBeneficiaria() {
    EscenarioFiltros esc = montarEscenarioFiltros();

    List<Donacion> paraEscuela = repositorio.buscar(filtroVacio(), null, esc.beneficiariaEscuela);

    assertEquals(2, paraEscuela.size());
    assertTrue(paraEscuela.stream().allMatch(d ->
        d.getDestinatarioAsignado() != null
            && d.getDestinatarioAsignado().getId().equals(esc.beneficiariaEscuela.getId())));
  }

  @Test
  void buscarCombinaDonanteYSubcategoria() {
    EscenarioFiltros esc = montarEscenarioFiltros();

    List<Donacion> sillasDeAna = repositorio.buscar(
        new FiltroDonaciones(null, null, List.of(esc.subSillas)),
        esc.donanteAna, null);

    assertEquals(2, sillasDeAna.size());
    assertTrue(sillasDeAna.stream().allMatch(d -> d.getDonante().getId().equals(esc.donanteAna.getId())));
  }

  @Test
  void buscarCombinaBeneficiariaYEstado() {
    EscenarioFiltros esc = montarEscenarioFiltros();

    List<Donacion> asignadasDeEscuela = repositorio.buscar(
        new FiltroDonaciones(EstadosPosiblesDonacion.ASIGNACION_REALIZADA, null, null),
        null, esc.beneficiariaEscuela);
    List<Donacion> enDepositoDeEscuela = repositorio.buscar(
        new FiltroDonaciones(EstadosPosiblesDonacion.EN_DEPOSITO, null, null),
        null, esc.beneficiariaEscuela);

    assertEquals(2, asignadasDeEscuela.size());
    assertTrue(enDepositoDeEscuela.isEmpty(),
        "Las de la escuela quedaron en ASIGNACION_REALIZADA tras confirmar destino.");
  }

  // === Helpers del escenario de filtros ===

  // Monta un escenario con:
  //   - 2 donaciones de Ana (Sillas, Mobiliario) EN_DEPOSITO
  //   - 1 donacion de Ana (Fideos, Alimentos) EN_DEPOSITO
  //   - 2 donaciones de Luis asignadas a Escuela: (Mesas, Mobiliario) y (Sillas, Mobiliario)
  // De esa forma cada test puede chequear combinaciones sin pisarse con datos
  // de otro test (categorias y subcategorias usan nombres unicos).
  private EscenarioFiltros montarEscenarioFiltros() {
    Categoria mobiliario = new Categoria(DatosDeTest.textoUnico("Mobiliario"));
    Categoria alimentos = new Categoria(DatosDeTest.textoUnico("Alimentos"));
    entityManager().persist(mobiliario);
    entityManager().persist(alimentos);

    Subcategoria sillas = new Subcategoria(DatosDeTest.textoUnico("Sillas"), mobiliario, Unidades.UNIDADES);
    Subcategoria mesas = new Subcategoria(DatosDeTest.textoUnico("Mesas"), mobiliario, Unidades.UNIDADES);
    Subcategoria fideos = new Subcategoria(DatosDeTest.textoUnico("Fideos"), alimentos, Unidades.KILOGRAMOS);
    entityManager().persist(sillas);
    entityManager().persist(mesas);
    entityManager().persist(fideos);

    PersonaHumana personaAna = new PersonaHumana("Ana", "Perez", 30,
        DatosDeTest.documentoUnico(), Genero.FEMENINO);
    PersonaHumana personaLuis = new PersonaHumana("Luis", "Gomez", 40,
        DatosDeTest.documentoUnico(), Genero.MASCULINO);
    repoPersonas.guardar(personaAna);
    repoPersonas.guardar(personaLuis);
    Donante donanteAna = new Donante(personaAna);
    Donante donanteLuis = new Donante(personaLuis);
    entityManager().persist(donanteAna);
    entityManager().persist(donanteLuis);

    PersonaJuridica personaEscuela = new PersonaJuridica(
        DatosDeTest.cuitUnico(), "Escuela N" + DatosDeTest.textoUnico("x"),
        TipoOrganizacion.INSTITUCION, "Educacion");
    repoPersonas.guardar(personaEscuela);
    Beneficiaria escuela = new Beneficiaria(personaEscuela);
    repoEntidades.guardar(escuela);

    Donacion deAnaSillas1 = guardar(donacionCon("Silla A1", sillas, donanteAna, "sillas de Ana 1"));
    Donacion deAnaSillas2 = guardar(donacionCon("Silla A2", sillas, donanteAna, "sillas de Ana 2"));
    Donacion deAnaFideos = guardar(donacionCon("Fideos A", fideos, donanteAna, "fideos de Ana"));

    Donacion deLuisMesas = donacionCon("Mesa L", mesas, donanteLuis, "mesas de Luis");
    deLuisMesas.confirmarDestino(escuela);
    repositorio.guardar(deLuisMesas);
    Donacion deLuisSillas = donacionCon("Silla L", sillas, donanteLuis, "sillas de Luis");
    deLuisSillas.confirmarDestino(escuela);
    repositorio.guardar(deLuisSillas);

    entityManager().flush();
    entityManager().clear();

    // Las entidades quedaron detached tras el clear; para que los equals
    // funcionen en las queries, las releemos.
    Donante anaRefrescada = entityManager().find(Donante.class, donanteAna.getId());
    Donante luisRefrescado = entityManager().find(Donante.class, donanteLuis.getId());
    Beneficiaria escuelaRefrescada = entityManager().find(Beneficiaria.class, escuela.getId());

    return new EscenarioFiltros(
        mobiliario.getNombre(),
        alimentos.getNombre(),
        sillas.getNombre(),
        mesas.getNombre(),
        fideos.getNombre(),
        anaRefrescada,
        luisRefrescado,
        escuelaRefrescada,
        5
    );
  }

  private Donacion guardar(Donacion donacion) {
    repositorio.guardar(donacion);
    return donacion;
  }

  private Donacion donacionCon(String nombreBien,
                               Subcategoria subcategoria,
                               Donante donante,
                               String descripcion) {
    Bien bien = new Bien(nombreBien, subcategoria, 1, CondicionBien.USADO);
    return Donacion.crear(List.of(bien), donante, descripcion);
  }

  private FiltroDonaciones filtroVacio() {
    return new FiltroDonaciones(null, null, null);
  }

  private record EscenarioFiltros(
      String categoriaMobiliario,
      String categoriaAlimentos,
      String subSillas,
      String subMesas,
      String subFideos,
      Donante donanteAna,
      Donante donanteLuis,
      Beneficiaria beneficiariaEscuela,
      int cantidadTotal
  ) {}
}
