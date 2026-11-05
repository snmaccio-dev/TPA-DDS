package donatrack.donaciones.seguridad;

// Puerto que esconde al proveedor de identidad externo (en produccion, Keycloak).
// Las excepciones de dominio (ConflictoDeNegocioException para 409, etc.) se
// propagan con sus mensajes propios. Cualquier otro error se encapsula en
// IntegracionConIdentidadException, que el handler global mapea a 502.
public interface ProveedorDeIdentidad {

    // Crea la cuenta con contrasena ya seteada (registro publico). Devuelve el
    // id externo del usuario (el "sub" del JWT en Keycloak).
    String crearUsuario(String email, String contrasena, RolDeAcceso rol);

    // Crea la cuenta habilitada sin contrasena y le pide al proveedor que
    // envie el correo para que el usuario la configure (alta por un admin).
    String crearUsuarioSinContrasena(String email, RolDeAcceso rol);

    // Elimina la cuenta. Si ya no existe, no hay nada que hacer.
    void eliminarUsuario(String idExterno);
}