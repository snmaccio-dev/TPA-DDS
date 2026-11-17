package donatrack.donaciones.domain.donacion;

import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.catalogo.Unidades;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DonacionModificarTest {

  private final Subcategoria sillas =
      new Subcategoria("Sillas", new Categoria("Mobiliario"), Unidades.UNIDADES);

  @Test
  void enDepositoModificaDescripcionFotoYBienes() throws Exception {
    Bien silla = bien(1L, "Silla", 2);
    Bien banco = bien(2L, "Banco", 1);
    banco.setFechaVencimiento(LocalDate.of(2027, 1, 1));
    Donacion donacion = donacion(silla, banco);

    donacion.modificar(" Sillas de madera ", "http://fotos/1.jpg", List.of(
        new DatosModificacionBien(1L, "Silla de roble", 4.0),
        new DatosModificacionBien(2L, " Banco largo ", 3.0)));

    assertEquals("Sillas de madera", donacion.getDescripcion());
    assertEquals("http://fotos/1.jpg", donacion.getFotoUrl());
    assertEquals("Silla de roble", silla.getDescripcion());
    assertEquals(4.0, silla.getCantidad());
    assertEquals("Banco largo", banco.getDescripcion());
    assertEquals(3.0, banco.getCantidad());
    assertEquals(7.0, donacion.getCantidadTotal());
    assertEquals(sillas, banco.getSubcategoria());
    assertEquals(CondicionBien.USADO, banco.getCondicion());
    assertEquals(LocalDate.of(2027, 1, 1), banco.getFechaVencimiento());
  }

  @Test
  void unaFotoVaciaQuitaLaFoto() throws Exception {
    Donacion donacion = donacion(bien(1L, "Silla", 1));
    donacion.asignarFoto("http://fotos/vieja.jpg");

    donacion.modificar("desc", " ", List.of(new DatosModificacionBien(1L, "Silla", 1.0)));

    assertNull(donacion.getFotoUrl());
  }

  @Test
  void fueraDeDepositoFallaCon409YNoCambiaNada() throws Exception {
    Bien silla = bien(1L, "Silla", 1);
    Donacion donacion = donacion(silla);
    donacion.confirmarDestino(new Beneficiaria(
        new PersonaJuridica("30-11111111-1", "Escuela", TipoOrganizacion.INSTITUCION, "Edu")));

    ConflictoDeNegocioException error = assertThrows(ConflictoDeNegocioException.class, () ->
        donacion.modificar("otra", null, List.of(new DatosModificacionBien(1L, "Otra", 5.0))));

    assertEquals("Solo se puede modificar una donacion en deposito.", error.getMessage());
    assertEquals("desc", donacion.getDescripcion());
    assertEquals("Silla", silla.getDescripcion());
  }

  @Test
  void conUnBienFaltanteFalla() throws Exception {
    Donacion donacion = donacion(bien(1L, "Silla", 1), bien(2L, "Banco", 1));

    assertThrows(DatosInvalidosException.class, () ->
        donacion.modificar("desc", null, List.of(new DatosModificacionBien(1L, "Silla", 1.0))));
  }

  @Test
  void conUnBienSobranteSinIdFalla() throws Exception {
    Donacion donacion = donacion(bien(1L, "Silla", 1));

    assertThrows(DatosInvalidosException.class, () ->
        donacion.modificar("desc", null, List.of(
            new DatosModificacionBien(1L, "Silla", 1.0),
            new DatosModificacionBien(null, "Mesa", 1.0))));
  }

  @Test
  void conUnBienRepetidoFalla() throws Exception {
    Donacion donacion = donacion(bien(1L, "Silla", 1));

    assertThrows(DatosInvalidosException.class, () ->
        donacion.modificar("desc", null, List.of(
            new DatosModificacionBien(1L, "Silla", 1.0),
            new DatosModificacionBien(1L, "Silla", 2.0))));
  }

  @Test
  void conUnBienAjenoFalla() throws Exception {
    Donacion donacion = donacion(bien(1L, "Silla", 1));

    DatosInvalidosException error = assertThrows(DatosInvalidosException.class, () ->
        donacion.modificar("desc", null, List.of(
            new DatosModificacionBien(1L, "Silla", 1.0),
            new DatosModificacionBien(99L, "Ajeno", 1.0))));

    assertEquals("El bien con id 99 no pertenece a la donacion.", error.getMessage());
  }

  @Test
  void conCantidadCeroFallaYNoAplicaNingunCambio() throws Exception {
    Bien silla = bien(1L, "Silla", 1);
    Bien banco = bien(2L, "Banco", 1);
    Donacion donacion = donacion(silla, banco);

    assertThrows(DatosInvalidosException.class, () ->
        donacion.modificar("nueva", null, List.of(
            new DatosModificacionBien(1L, "Silla nueva", 3.0),
            new DatosModificacionBien(2L, "Banco", 0.0))));

    assertEquals("desc", donacion.getDescripcion());
    assertEquals("Silla", silla.getDescripcion());
    assertEquals(1.0, silla.getCantidad());
  }

  @Test
  void sinCantidadFalla() throws Exception {
    Donacion donacion = donacion(bien(1L, "Silla", 1));

    assertThrows(DatosInvalidosException.class, () ->
        donacion.modificar("desc", null, List.of(new DatosModificacionBien(1L, "Silla", null))));
  }

  @Test
  void conDescripcionDeLaDonacionVaciaFalla() throws Exception {
    Donacion donacion = donacion(bien(1L, "Silla", 1));

    assertThrows(DatosInvalidosException.class, () ->
        donacion.modificar(" ", null, List.of(new DatosModificacionBien(1L, "Silla", 1.0))));
  }

  @Test
  void conDescripcionDeUnBienVaciaFalla() throws Exception {
    Donacion donacion = donacion(bien(1L, "Silla", 1));

    assertThrows(DatosInvalidosException.class, () ->
        donacion.modificar("desc", null, List.of(new DatosModificacionBien(1L, "", 1.0))));
  }

  private Donacion donacion(Bien... bienes) {
    Donante donante = new Donante(
        new PersonaHumana("Ana", "Perez", 30, "11111111", Genero.FEMENINO));
    return new Donacion(List.of(bienes), donante, "desc");
  }

  private Bien bien(long id, String descripcion, double cantidad) throws Exception {
    Bien bien = new Bien(descripcion, sillas, cantidad, CondicionBien.USADO);
    Field campo = Bien.class.getDeclaredField("id");
    campo.setAccessible(true);
    campo.set(bien, id);
    return bien;
  }
}
