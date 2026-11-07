package donatrack.web.templates;

import donatrack.web.Alertas;
import donatrack.web.Modelo;
import io.javalin.http.Context;
import io.javalin.rendering.FileRenderer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class JavalinRenderer implements FileRenderer {
    private Map<String, FileRenderer> renderers = new HashMap<>();
    public JavalinRenderer register(String extension, FileRenderer renderer) {
        renderers.put(extension, renderer);
        return this;
    }

    @Override
    public String render(@NotNull String filePath, @NotNull Map<String, ? extends Object> model, @NotNull Context context) {
        String extension = filePath.substring(filePath.lastIndexOf(".") + 1);

        // Inyectamos el flash de alerta aca, una sola vez, para no obligar a cada
        // controller a leerlo y limpiarlo despues de un redirect.
        Map<String, Object> enriquecido = new HashMap<>(model);
        Map<String, String> flash = Alertas.consumir(context);
        if (flash != null) {
            String tipo = flash.get("tipo");
            String texto = flash.get("texto");
            if (Alertas.TIPO_ERROR.equals(tipo)) {
                enriquecido.putIfAbsent(Modelo.CLAVE_ALERTA_ERROR, texto);
            } else if (Alertas.TIPO_ADVERTENCIA.equals(tipo)) {
                enriquecido.putIfAbsent(Modelo.CLAVE_ALERTA_ADVERTENCIA, texto);
            }
        }

        return renderers.get(extension).render(filePath, enriquecido, context);
    }

}
