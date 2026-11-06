package donatrack.donaciones.controller.dto;

// Alta de un administrador: email + contrasena que define el admin que lo
// esta dando de alta. toString oculta la contrasena para que no aparezca
// en logs ni en mensajes de excepcion (Javalin la logeara si un bodyAsClass
// falla).
public record NuevoAdministradorRequest(String email, String contrasena) {

    @Override
    public String toString() {
        return "NuevoAdministradorRequest{email=" + email + ", contrasena=***}";
    }
}
