package donatrack.donaciones;

import java.util.concurrent.atomic.AtomicLong;

// Generadores compartidos por todos los tests que persisten datos.
// Un unico AtomicLong seedeado con System.nanoTime() alcanza para que los
// valores no choquen ni entre clases del mismo run ni entre corridas, aunque
// la base en memoria se comparta. Los tests puramente unitarios no lo
// necesitan.
public final class DatosDeTest {

    private static final AtomicLong CONTADOR = new AtomicLong(System.nanoTime());

    private DatosDeTest() {}

    public static String documentoUnico() {
        // 8 digitos empezando en 9, fuera del rango de DNI de los CSV de ejemplo.
        long n = CONTADOR.incrementAndGet();
        return "9" + String.format("%07d", Math.floorMod(n, 10_000_000L));
    }

    public static String cuitUnico() {
        // 11 digitos empezando en 33, fuera del prefijo 30 de los CSV de ejemplo.
        long n = CONTADOR.incrementAndGet();
        return "33" + String.format("%09d", Math.floorMod(n, 1_000_000_000L));
    }

    public static String emailUnico(String prefijo) {
        return prefijo + "-" + CONTADOR.incrementAndGet() + "@test.local";
    }

    public static String keycloakIdUnico() {
        return "kc-" + CONTADOR.incrementAndGet();
    }

    public static String textoUnico(String prefijo) {
        return prefijo + "-" + CONTADOR.incrementAndGet();
    }
}
