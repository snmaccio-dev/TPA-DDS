package donatrack.web.templates;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.rendering.FileRenderer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

public class JavalinHandlebars implements FileRenderer {

    Handlebars handlebars = new Handlebars();
    @NotNull
    @Override
    public String render(@NotNull String path, @NotNull Map<String, ?> model, @NotNull Context context) {
        Template template = null;
        try {
            template = handlebars.compile("templates/" + path.replace(".hbs",""));
            return template.apply(model);
        } catch (IOException e) {
            e.printStackTrace();
            context.status(HttpStatus.NOT_FOUND);
            return "No se encuentra la página indicada...";
        }
    }
}
