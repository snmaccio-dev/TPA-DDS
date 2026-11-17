package donatrack.donaciones.infrastructure.importacion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class LectorCsvDonantes {

  private static final int COLUMNAS = 6;
  private static final char BOM = '﻿';

  public Stream<LineaCsv> leer(Path archivo) {
    try {
      return leer(Files.newInputStream(archivo));
    } catch (IOException e) {
      throw new ImportacionCsvException("No se pudo abrir el archivo CSV: " + archivo, e);
    }
  }

  public Stream<LineaCsv> leer(InputStream entrada) {
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
    AtomicInteger numero = new AtomicInteger();
    return reader.lines()
        .map(linea -> {
          int actual = numero.incrementAndGet();
          if (actual == 1 || linea.isBlank()) {
            return null;
          }
          return parsear(actual, linea);
        })
        .filter(Objects::nonNull)
        .onClose(() -> cerrar(reader));
  }

  LineaCsv parsear(int numero, String linea) {
    String limpia = linea.charAt(0) == BOM ? linea.substring(1) : linea;
    String[] campos = limpia.split(",", -1);
    if (campos.length != COLUMNAS) {
      return new LineaInvalida(numero,
          "La fila tiene " + campos.length + " columnas y se esperaban " + COLUMNAS + ".");
    }
    for (int i = 0; i < campos.length; i++) {
      campos[i] = campos[i].trim();
    }

    FilaDonanteCsv.TipoPersona tipo;
    try {
      tipo = FilaDonanteCsv.TipoPersona.valueOf(campos[0].toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return new LineaInvalida(numero,
          "El tipo de persona '" + campos[0] + "' no es valido: tiene que ser HUMANA o JURIDICA.");
    }
    if (campos[2].isEmpty()) {
      return new LineaInvalida(numero, "El documento es obligatorio.");
    }
    if (campos[3].isEmpty()) {
      return new LineaInvalida(numero, "El nombre o razon social es obligatorio.");
    }
    if (campos[4].isEmpty()) {
      return new LineaInvalida(numero, "El email es obligatorio.");
    }
    return new FilaDonanteCsv(
        numero, tipo, campos[1], campos[2], campos[3], campos[4], campos[5]);
  }

  private static void cerrar(BufferedReader reader) {
    try {
      reader.close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
