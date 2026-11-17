package donatrack.donaciones.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public final class Enumerados {

    private Enumerados() {}

    public static <E extends Enum<E>> E desde(Class<E> tipo, String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DatosInvalidosException(
                "El valor '" + valor + "' no es valido para " + campo + ". Valores validos: "
                    + Arrays.stream(tipo.getEnumConstants())
                        .map(Enum::name)
                        .collect(Collectors.joining(", "))
                    + ".");
        }
    }
}
