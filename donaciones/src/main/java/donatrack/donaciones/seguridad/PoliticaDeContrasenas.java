package donatrack.donaciones.seguridad;

import donatrack.donaciones.domain.DatosInvalidosException;

// Politica minima de contrasenas de Donaciones. Keycloak aplica su propia
// politica despues; esta sirve para fallar rapido antes de llamar a Keycloak.
public final class PoliticaDeContrasenas {

    public static final int LARGO_MINIMO = 8;

    private PoliticaDeContrasenas() {}

    public static void validar(String contrasena) {
        if (contrasena == null || contrasena.length() < LARGO_MINIMO) {
            throw new DatosInvalidosException(
                "La contrasena debe tener al menos " + LARGO_MINIMO + " caracteres."
            );
        }
    }
}