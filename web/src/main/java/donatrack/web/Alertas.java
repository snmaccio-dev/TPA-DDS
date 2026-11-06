package donatrack.web;

import io.javalin.http.Context;

import java.util.Map;

// Puente entre los handlers (que pueden hacer redirect) y el componente unico de alerta.
// Guardamos el aviso como un atributo de sesion de un solo uso: el renderer lo lee,
// lo inyecta en el modelo y lo borra.
public final class Alertas {

  public static final String CLAVE_FLASH = "alerta";
  public static final String TIPO_ERROR = "error";
  public static final String TIPO_ADVERTENCIA = "advertencia";

  public static final String SERVICIO_NO_DISPONIBLE =
      "El servicio no está disponible, intentá de nuevo en unos minutos.";

  private Alertas() {
  }

  public static void flashError(Context ctx, String texto) {
    flash(ctx, TIPO_ERROR, texto);
  }

  public static void flashAdvertencia(Context ctx, String texto) {
    flash(ctx, TIPO_ADVERTENCIA, texto);
  }

  public static void flash(Context ctx, String tipo, String texto) {
    if (ctx == null || texto == null || texto.isBlank()) return;
    ctx.sessionAttribute(CLAVE_FLASH, Map.of("tipo", tipo, "texto", texto));
  }

  @SuppressWarnings("unchecked")
  public static Map<String, String> consumir(Context ctx) {
    if (ctx == null) return null;
    Object valor = ctx.sessionAttribute(CLAVE_FLASH);
    if (valor == null) return null;
    ctx.sessionAttribute(CLAVE_FLASH, null);
    if (valor instanceof Map<?, ?> m) {
      return (Map<String, String>) m;
    }
    return null;
  }
}
