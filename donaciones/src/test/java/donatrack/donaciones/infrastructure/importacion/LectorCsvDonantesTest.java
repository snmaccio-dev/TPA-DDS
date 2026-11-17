package donatrack.donaciones.infrastructure.importacion;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LectorCsvDonantesTest {

  private final LectorCsvDonantes lector = new LectorCsvDonantes();

  @Test
  void parseaLasFilasValidasYSalteaElEncabezado() {
    List<LineaCsv> lineas = leer(
        "TipoPersona,TipoDoc,Documento,NombreRazonSocial,Email,Telefono\r\n"
            + "HUMANA,DNI,12345678,Ana Perez,ana@mail.com,+54 11 5555-5555\r\n"
            + "juridica,CUIT,30-12345678-9, Arcos S.A. ,contacto@arcos.com,\r\n");

    assertEquals(2, lineas.size());
    FilaDonanteCsv ana = assertInstanceOf(FilaDonanteCsv.class, lineas.get(0));
    assertEquals(2, ana.numero());
    assertEquals(FilaDonanteCsv.TipoPersona.HUMANA, ana.tipoPersona());
    assertEquals("DNI", ana.tipoDocumento());
    assertEquals("12345678", ana.documento());
    assertEquals("Ana Perez", ana.nombre());
    assertEquals("ana@mail.com", ana.email());
    assertEquals("+54 11 5555-5555", ana.telefono());

    FilaDonanteCsv arcos = assertInstanceOf(FilaDonanteCsv.class, lineas.get(1));
    assertEquals(3, arcos.numero());
    assertEquals(FilaDonanteCsv.TipoPersona.JURIDICA, arcos.tipoPersona());
    assertEquals("Arcos S.A.", arcos.nombre());
    assertEquals("", arcos.telefono());
    assertEquals("30123456789", arcos.documentoNormalizado());
  }

  @Test
  void informaLasFilasInvalidasSinCortarLaLectura() {
    List<LineaCsv> lineas = leer(
        "TipoPersona,TipoDoc,Documento,NombreRazonSocial,Email,Telefono\n"
            + "HUMANA,DNI,12345678\n"
            + "EXTRATERRESTRE,DNI,1,ET,et@espacio.com,1111\n"
            + "HUMANA,DNI,22222222,Sin Mail,,1111\n"
            + "\n"
            + "HUMANA,DNI,33333333,Ok Persona,ok@mail.com,\n");

    assertEquals(4, lineas.size());
    LineaInvalida columnas = assertInstanceOf(LineaInvalida.class, lineas.get(0));
    assertEquals(2, columnas.numero());
    assertTrue(columnas.mensaje().contains("columnas"));
    LineaInvalida tipo = assertInstanceOf(LineaInvalida.class, lineas.get(1));
    assertEquals(3, tipo.numero());
    assertTrue(tipo.mensaje().contains("EXTRATERRESTRE"));
    LineaInvalida email = assertInstanceOf(LineaInvalida.class, lineas.get(2));
    assertEquals(4, email.numero());
    assertTrue(email.mensaje().contains("email"));
    FilaDonanteCsv valida = assertInstanceOf(FilaDonanteCsv.class, lineas.get(3));
    assertEquals(6, valida.numero());
  }

  @Test
  void leeEnUtf8YDescartaElBom() {
    List<LineaCsv> lineas = leer(
        "﻿TipoPersona,TipoDoc,Documento,Nombre/Razón Social,Email,Teléfono\n"
            + "HUMANA,DNI,11780798,Benjamín Castro,benjamíncastro8929@yahoo.com,+54 11 4379-4026\n");

    FilaDonanteCsv fila = assertInstanceOf(FilaDonanteCsv.class, lineas.get(0));
    assertEquals("Benjamín Castro", fila.nombre());
    assertEquals("benjamíncastro8929@yahoo.com", fila.email());
  }

  @Test
  void leeLosArchivosDeEjemploSinErroresDeFormato() {
    try (Stream<LineaCsv> lineas =
             lector.leer(getClass().getResourceAsStream("/donantes_prueba_2.csv"))) {
      List<LineaCsv> todas = lineas.toList();
      assertEquals(20000, todas.size());
      assertTrue(todas.stream().allMatch(FilaDonanteCsv.class::isInstance));
    }
    try (Stream<LineaCsv> lineas =
             lector.leer(getClass().getResourceAsStream("/donantes_prueba.csv"))) {
      List<LineaCsv> todas = lineas.toList();
      assertEquals(5, todas.size());
      FilaDonanteCsv maria = assertInstanceOf(FilaDonanteCsv.class, todas.get(3));
      assertEquals("", maria.telefono());
    }
  }

  @Test
  void unArchivoInexistenteLanzaImportacionCsvException() {
    assertThrows(ImportacionCsvException.class,
        () -> lector.leer(Path.of("no-existe-" + System.nanoTime() + ".csv")));
  }

  private List<LineaCsv> leer(String contenido) {
    InputStream entrada = new ByteArrayInputStream(contenido.getBytes(StandardCharsets.UTF_8));
    try (Stream<LineaCsv> lineas = lector.leer(entrada)) {
      return lineas.toList();
    }
  }
}
