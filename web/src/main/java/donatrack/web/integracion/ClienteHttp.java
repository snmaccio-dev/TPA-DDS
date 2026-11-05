package donatrack.web.integracion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClienteHttp {

  private static final Duration ESPERA = Duration.ofSeconds(10);
  private static final String RECHAZO_SIN_DETALLE = "El pedido fue rechazado.";

  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(ESPERA)
      .build();

  private final ObjectMapper json = new ObjectMapper()
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private final String urlBase;
  private final String servicio;

  public ClienteHttp(String urlBase, String servicio) {
    if (urlBase == null || urlBase.isBlank()) {
      throw new IllegalArgumentException(
          "Debe indicarse la URL base del servicio de " + servicio + "."
      );
    }

    this.urlBase = urlBase.endsWith("/")
        ? urlBase.substring(0, urlBase.length() - 1)
        : urlBase;
    this.servicio = servicio;
  }

  public <T> T obtener(String ruta, TypeReference<T> tipo) {
    HttpResponse<String> respuesta = enviar(
        HttpRequest.newBuilder()
            .uri(URI.create(urlBase + ruta))
            .header("Accept", "application/json")
            .timeout(ESPERA)
            .GET()
            .build()
    );

    verificar(respuesta);

    return deserializar(respuesta.body(), tipo);
  }

  public void publicar(String ruta, Object cuerpo) {
    escribir("POST", ruta, cuerpo);
  }

  public void reemplazar(String ruta, Object cuerpo) {
    escribir("PUT", ruta, cuerpo);
  }

  public <T> T publicarArchivos(String ruta,
                                String campo,
                                List<ArchivoSubido> archivos,
                                TypeReference<T> tipo) {
    String frontera = "DonaTrack" + UUID.randomUUID();

    HttpResponse<String> respuesta = enviar(
        HttpRequest.newBuilder()
            .uri(URI.create(urlBase + ruta))
            .header("Content-Type", "multipart/form-data; boundary=" + frontera)
            .header("Accept", "application/json")
            .timeout(ESPERA)
            .POST(HttpRequest.BodyPublishers.ofByteArrays(
                cuerpoMultiparte(frontera, campo, archivos)))
            .build()
    );

    verificar(respuesta);

    return deserializar(respuesta.body(), tipo);
  }

  private List<byte[]> cuerpoMultiparte(String frontera,
                                        String campo,
                                        List<ArchivoSubido> archivos) {
    List<byte[]> partes = new ArrayList<>();

    for (ArchivoSubido archivo : archivos) {
      partes.add(texto("--" + frontera + "\r\n"
          + "Content-Disposition: form-data; name=\"" + campo + "\"; filename=\""
          + archivo.nombre().replace("\"", "") + "\"\r\n"
          + "Content-Type: " + archivo.tipoMime() + "\r\n\r\n"));
      partes.add(archivo.contenido());
      partes.add(texto("\r\n"));
    }

    partes.add(texto("--" + frontera + "--\r\n"));

    return partes;
  }

  private byte[] texto(String valor) {
    return valor.getBytes(StandardCharsets.UTF_8);
  }

  private void escribir(String metodo, String ruta, Object cuerpo) {
    verificar(enviar(
        HttpRequest.newBuilder()
            .uri(URI.create(urlBase + ruta))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .timeout(ESPERA)
            .method(metodo, HttpRequest.BodyPublishers.ofString(serializar(cuerpo)))
            .build()
    ));
  }

  private void verificar(HttpResponse<String> respuesta) {
    int estado = respuesta.statusCode();

    if (estado >= 400 && estado < 500) {
      throw new OperacionRechazadaException(mensajeDe(respuesta.body()));
    }

    if (estado < 200 || estado >= 300) {
      throw new ServicioNoDisponibleException(servicio, "respondió " + estado);
    }
  }

  private String mensajeDe(String cuerpo) {
    try {
      String mensaje = json.readValue(cuerpo, ErrorRecibido.class).mensaje();
      return mensaje == null || mensaje.isBlank() ? RECHAZO_SIN_DETALLE : mensaje;
    } catch (JsonProcessingException e) {
      return RECHAZO_SIN_DETALLE;
    }
  }

  private String serializar(Object cuerpo) {
    try {
      return json.writeValueAsString(cuerpo);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(
          "No se pudo armar el pedido para el servicio de " + servicio + ".", e
      );
    }
  }

  private <T> T deserializar(String cuerpo, TypeReference<T> tipo) {
    try {
      return json.readValue(cuerpo, tipo);
    } catch (JsonProcessingException e) {
      throw new ServicioNoDisponibleException(
          servicio, "devolvió una respuesta que no se pudo interpretar", e
      );
    }
  }

  private HttpResponse<String> enviar(HttpRequest pedido) {
    try {
      return http.send(pedido, HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ServicioNoDisponibleException(servicio, "no respondió a tiempo", e);
    } catch (IOException e) {
      throw new ServicioNoDisponibleException(servicio, "no está disponible", e);
    }
  }
}
