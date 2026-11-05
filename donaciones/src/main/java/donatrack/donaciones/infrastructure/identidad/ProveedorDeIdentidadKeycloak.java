package donatrack.donaciones.infrastructure.identidad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.seguridad.IntegracionConIdentidadException;
import donatrack.donaciones.seguridad.ProveedorDeIdentidad;
import donatrack.donaciones.seguridad.RolDeAcceso;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

// Adaptador contra la Admin REST API de Keycloak. Usa java.net.http.HttpClient
// y Jackson para no agregar keycloak-admin-client. Las contrasenas nunca se
// guardan ni se loguean: se reenvian a Keycloak en una unica llamada y la
// variable se descarta.
public class ProveedorDeIdentidadKeycloak implements ProveedorDeIdentidad {

    private static final Duration ESPERA = Duration.ofSeconds(10);
    private static final Duration MARGEN_RENOVACION = Duration.ofSeconds(30);

    private final HttpClient http;
    private final ObjectMapper json;
    private final String urlBase;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    private volatile String tokenEnCache;
    private volatile Instant vencimientoToken = Instant.EPOCH;

    public ProveedorDeIdentidadKeycloak(String urlBase, String realm,
                                        String clientId, String clientSecret) {
        this(urlBase, realm, clientId, clientSecret,
            HttpClient.newBuilder().connectTimeout(ESPERA).build(),
            new ObjectMapper());
    }

    // Constructor pensado para los tests: inyecta el HttpClient y el mapper.
    public ProveedorDeIdentidadKeycloak(String urlBase, String realm,
                                        String clientId, String clientSecret,
                                        HttpClient http, ObjectMapper json) {
        if (urlBase == null || urlBase.isBlank()) {
            throw new IllegalArgumentException("La URL de Keycloak es obligatoria.");
        }
        if (realm == null || realm.isBlank()) {
            throw new IllegalArgumentException("El realm de Keycloak es obligatorio.");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("El client id de Keycloak es obligatorio.");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("El client secret de Keycloak es obligatorio.");
        }
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.http = http;
        this.json = json;
    }

    @Override
    public String crearUsuario(String email, String contrasena, RolDeAcceso rol) {
        validarEmail(email);
        if (contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException("La contrasena es obligatoria.");
        }
        return altaInterna(email, contrasena, rol, false);
    }

    @Override
    public String crearUsuarioSinContrasena(String email, RolDeAcceso rol) {
        validarEmail(email);
        String id = altaInterna(email, null, rol, true);
        enviarMailDeContrasena(id);
        return id;
    }

    @Override
    public void eliminarUsuario(String idExterno) {
        if (idExterno == null || idExterno.isBlank()) {
            return;
        }
        HttpResponse<String> respuesta = enviar(
            HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/admin/realms/" + realm + "/users/" + idExterno))
                .header("Authorization", "Bearer " + obtenerToken())
                .timeout(ESPERA)
                .DELETE()
                .build(),
            "DELETE /users"
        );
        int codigo = respuesta.statusCode();
        if (codigo == 404) {
            return;
        }
        if (!fueExitosa(codigo)) {
            throw new IntegracionConIdentidadException(
                "Keycloak respondio " + codigo + " al eliminar el usuario " + idExterno + "."
            );
        }
    }

    private String altaInterna(String email, String contrasena, RolDeAcceso rol, boolean sinContrasena) {
        ObjectNode body = json.createObjectNode();
        body.put("username", email);
        body.put("email", email);
        body.put("enabled", true);
        if (!sinContrasena) {
            ArrayNode credentials = body.putArray("credentials");
            ObjectNode credencial = credentials.addObject();
            credencial.put("type", "password");
            credencial.put("value", contrasena);
            credencial.put("temporary", false);
        }

        HttpResponse<String> respuesta = enviar(
            HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/admin/realms/" + realm + "/users"))
                .header("Authorization", "Bearer " + obtenerToken())
                .header("Content-Type", "application/json")
                .timeout(ESPERA)
                .POST(HttpRequest.BodyPublishers.ofString(serializar(body)))
                .build(),
            "POST /users"
        );

        int codigo = respuesta.statusCode();
        if (codigo == 409) {
            throw new ConflictoDeNegocioException("Ya existe una cuenta con ese correo.");
        }
        if (!fueExitosa(codigo)) {
            throw new IntegracionConIdentidadException(
                "Keycloak respondio " + codigo + " al crear el usuario."
            );
        }

        String idExterno = idDesdeLocation(respuesta);

        try {
            asignarRol(idExterno, rol);
        } catch (RuntimeException e) {
            // Rollback: si no pudimos asignarle el rol, la cuenta queda invalida.
            // Si tampoco podemos borrarla, aflojamos con el error original y
            // dejamos la limpieza para una revision manual.
            try {
                eliminarUsuario(idExterno);
            } catch (RuntimeException ignorado) {
                // dejamos propagar el error original
            }
            throw e;
        }

        return idExterno;
    }

    private void asignarRol(String idExterno, RolDeAcceso rol) {
        String nombreRol = rol.name();

        HttpResponse<String> respuestaRol = enviar(
            HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/admin/realms/" + realm + "/roles/" + nombreRol))
                .header("Authorization", "Bearer " + obtenerToken())
                .header("Accept", "application/json")
                .timeout(ESPERA)
                .GET()
                .build(),
            "GET /roles/" + nombreRol
        );

        if (!fueExitosa(respuestaRol.statusCode())) {
            throw new IntegracionConIdentidadException(
                "Keycloak respondio " + respuestaRol.statusCode()
                    + " al buscar el rol " + nombreRol + "."
            );
        }

        JsonNode rolNode = parsear(respuestaRol.body());
        ArrayNode cuerpo = json.createArrayNode();
        cuerpo.add(rolNode);

        HttpResponse<String> respuestaMapping = enviar(
            HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/admin/realms/" + realm
                    + "/users/" + idExterno + "/role-mappings/realm"))
                .header("Authorization", "Bearer " + obtenerToken())
                .header("Content-Type", "application/json")
                .timeout(ESPERA)
                .POST(HttpRequest.BodyPublishers.ofString(serializar(cuerpo)))
                .build(),
            "POST /role-mappings/realm"
        );

        if (!fueExitosa(respuestaMapping.statusCode())) {
            throw new IntegracionConIdentidadException(
                "Keycloak respondio " + respuestaMapping.statusCode()
                    + " al asignar el rol " + nombreRol + "."
            );
        }
    }

    private void enviarMailDeContrasena(String idExterno) {
        ArrayNode acciones = json.createArrayNode();
        acciones.add("UPDATE_PASSWORD");

        try {
            HttpResponse<String> respuesta = enviar(
                HttpRequest.newBuilder()
                    .uri(URI.create(urlBase + "/admin/realms/" + realm
                        + "/users/" + idExterno + "/execute-actions-email"))
                    .header("Authorization", "Bearer " + obtenerToken())
                    .header("Content-Type", "application/json")
                    .timeout(ESPERA)
                    .PUT(HttpRequest.BodyPublishers.ofString(serializar(acciones)))
                    .build(),
                "PUT /execute-actions-email"
            );
            if (!fueExitosa(respuesta.statusCode())) {
                System.err.println("[KEYCLOAK] No se pudo enviar el mail de UPDATE_PASSWORD al usuario "
                    + idExterno + " (status " + respuesta.statusCode() + "). "
                    + "La cuenta queda creada; el usuario puede pedir reestablecer la contrasena.");
            }
        } catch (RuntimeException e) {
            System.err.println("[KEYCLOAK] Error al enviar el mail de UPDATE_PASSWORD al usuario "
                + idExterno + ": " + e.getMessage());
        }
    }

    private String idDesdeLocation(HttpResponse<String> respuesta) {
        Optional<String> location = respuesta.headers().firstValue("Location");
        if (location.isEmpty()) {
            throw new IntegracionConIdentidadException(
                "Keycloak no devolvio el header Location al crear el usuario."
            );
        }
        String url = location.get();
        int barra = url.lastIndexOf('/');
        if (barra < 0 || barra == url.length() - 1) {
            throw new IntegracionConIdentidadException(
                "El header Location devuelto por Keycloak no contiene el id: " + url
            );
        }
        return url.substring(barra + 1);
    }

    private String obtenerToken() {
        String cacheado = this.tokenEnCache;
        Instant vence = this.vencimientoToken;
        if (cacheado != null && Instant.now().isBefore(vence)) {
            return cacheado;
        }
        return refrescarToken();
    }

    private synchronized String refrescarToken() {
        if (this.tokenEnCache != null && Instant.now().isBefore(this.vencimientoToken)) {
            return this.tokenEnCache;
        }

        String cuerpo = "grant_type=client_credentials"
            + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
            + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);

        HttpResponse<String> respuesta = enviar(
            HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/realms/" + realm + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(ESPERA)
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build(),
            "POST /token"
        );

        if (!fueExitosa(respuesta.statusCode())) {
            throw new IntegracionConIdentidadException(
                "Keycloak respondio " + respuesta.statusCode() + " al pedir el token de servicio."
            );
        }

        JsonNode respuestaJson = parsear(respuesta.body());
        String accessToken = respuestaJson.path("access_token").asText(null);
        long expiresIn = respuestaJson.path("expires_in").asLong(0);
        if (accessToken == null || accessToken.isBlank()) {
            throw new IntegracionConIdentidadException(
                "Keycloak devolvio una respuesta sin access_token."
            );
        }
        this.tokenEnCache = accessToken;
        this.vencimientoToken = Instant.now().plusSeconds(Math.max(1, expiresIn))
            .minus(MARGEN_RENOVACION);
        return accessToken;
    }

    private HttpResponse<String> enviar(HttpRequest pedido, String accion) {
        try {
            return http.send(pedido, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IntegracionConIdentidadException(
                "Se interrumpio la llamada a Keycloak (" + accion + ").", e);
        } catch (Exception e) {
            throw new IntegracionConIdentidadException(
                "No se pudo contactar a Keycloak (" + accion + ").", e);
        }
    }

    private String serializar(JsonNode nodo) {
        try {
            return json.writeValueAsString(nodo);
        } catch (Exception e) {
            throw new IntegracionConIdentidadException("Error al serializar el cuerpo para Keycloak.", e);
        }
    }

    private JsonNode parsear(String cuerpo) {
        try {
            return json.readTree(cuerpo);
        } catch (Exception e) {
            throw new IntegracionConIdentidadException("Error al parsear la respuesta de Keycloak.", e);
        }
    }

    private static void validarEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio.");
        }
    }

    private static boolean fueExitosa(int codigo) {
        return codigo >= 200 && codigo < 300;
    }
}