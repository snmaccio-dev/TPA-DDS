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
        // 8 digitos, para que entre en el formato de DNI.
        long n = CONTADOR.incrementAndGet();
        return String.format("%08d", Math.floorMod(n, 100_000_000L));
    }

    public static String cuitUnico() {
        // 11 digitos, empezando en 30 para que parezca CUIT de persona juridica.
        long n = CONTADOR.incrementAndGet();
        return "30" + String.format("%09d", Math.floorMod(n, 1_000_000_000L));
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
