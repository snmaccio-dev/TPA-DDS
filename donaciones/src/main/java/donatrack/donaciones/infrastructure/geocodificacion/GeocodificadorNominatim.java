package donatrack.donaciones.infrastructure.geocodificacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import donatrack.donaciones.domain.geocodificacion.Geocodificador;
import donatrack.donaciones.domain.persona.Coordenadas;
import donatrack.donaciones.infrastructure.IntegracionExternaException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

// Geocodificacion con Nominatim (OpenStreetMap). La politica de uso del servicio publico
// pide un User-Agent que identifique a la aplicacion y como maximo una consulta por
// segundo: el User-Agent es configurable y las consultas se serializan, esperando lo que
// falte para que entre una y la siguiente pase al menos intervaloMinimo.
// https://operations.osmfoundation.org/policies/nominatim/
public class GeocodificadorNominatim implements Geocodificador {

    public static final String URL_PUBLICA = "https://nominatim.openstreetmap.org";
    public static final Duration UNA_POR_SEGUNDO = Duration.ofSeconds(1);

    private static final Duration ESPERA = Duration.ofSeconds(10);

    private final String urlBase;
    private final String userAgent;
    private final String paises;
    private final HttpClient http;
    private final Duration intervaloMinimo;
    private final ObjectMapper json = new ObjectMapper();

    private final Object turno = new Object();
    private long ultimaConsultaNanos;
    private boolean huboConsulta;

    public GeocodificadorNominatim(String urlBase, String userAgent) {
        this(urlBase, userAgent, "ar",
            HttpClient.newBuilder().connectTimeout(ESPERA).build(), UNA_POR_SEGUNDO);
    }

    // paises: codigos ISO separados por coma para acotar la busqueda (null o vacio, todos).
    public GeocodificadorNominatim(String urlBase,
                                   String userAgent,
                                   String paises,
                                   HttpClient http,
                                   Duration intervaloMinimo) {
        if (userAgent == null || userAgent.isBlank()) {
            throw new IllegalArgumentException("Nominatim exige un User-Agent propio.");
        }
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        this.userAgent = userAgent;
        this.paises = paises;
        this.http = http;
        this.intervaloMinimo = intervaloMinimo;
    }

    @Override
    public Optional<Coordenadas> geocodificar(String direccion) {
        if (direccion == null || direccion.isBlank()) {
            return Optional.empty();
        }
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(urlDe(direccion)))
            .timeout(ESPERA)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .header("Accept-Language", "es")
            .GET()
            .build();

        HttpResponse<String> respuesta;
        synchronized (turno) {
            esperarTurno();
            try {
                respuesta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                throw new IntegracionExternaException("Nominatim no respondio.", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IntegracionExternaException("Se interrumpio la consulta a Nominatim.", e);
            } finally {
                ultimaConsultaNanos = System.nanoTime();
                huboConsulta = true;
            }
        }

        if (respuesta.statusCode() != 200) {
            throw new IntegracionExternaException(
                "Nominatim respondio " + respuesta.statusCode() + ".", null);
        }
        return coordenadasDe(respuesta.body());
    }

    private String urlDe(String direccion) {
        StringBuilder url = new StringBuilder(urlBase)
            .append("/search?format=jsonv2&limit=1&q=")
            .append(URLEncoder.encode(direccion.trim(), StandardCharsets.UTF_8));
        if (paises != null && !paises.isBlank()) {
            url.append("&countrycodes=").append(URLEncoder.encode(paises, StandardCharsets.UTF_8));
        }
        return url.toString();
    }

    private void esperarTurno() {
        if (!huboConsulta) {
            return;
        }
        long faltan = intervaloMinimo.toNanos() - (System.nanoTime() - ultimaConsultaNanos);
        if (faltan <= 0) {
            return;
        }
        try {
            Thread.sleep(faltan / 1_000_000, (int) (faltan % 1_000_000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IntegracionExternaException("Se interrumpio la espera entre consultas.", e);
        }
    }

    // Respuesta: [{"lat": "-34.63", "lon": "-58.36", ...}] o [] si no encontro nada.
    private Optional<Coordenadas> coordenadasDe(String cuerpo) {
        try {
            JsonNode resultados = json.readTree(cuerpo);
            if (resultados == null || !resultados.isArray() || resultados.isEmpty()) {
                return Optional.empty();
            }
            JsonNode primero = resultados.get(0);
            return Optional.of(new Coordenadas(
                Double.parseDouble(primero.path("lat").asText()),
                Double.parseDouble(primero.path("lon").asText())));
        } catch (IOException | RuntimeException e) {
            throw new IntegracionExternaException("Nominatim devolvio una respuesta invalida.", e);
        }
    }
}
