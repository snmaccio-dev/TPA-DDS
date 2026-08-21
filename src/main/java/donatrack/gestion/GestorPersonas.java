package donatrack.gestion;

import donatrack.model.contacto.MedioContacto;
import donatrack.model.contacto.TipoContacto;
import donatrack.model.persona.Donante;
import donatrack.model.persona.Persona;
import donatrack.model.usuario.Usuario;
import donatrack.notificacion.NotificadorSMS;
import donatrack.notificacion.NotificadorEmail;
import donatrack.notificacion.*;
import donatrack.repositorio.RepositorioPersonas;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GestorPersonas {

    private final RepositorioPersonas repositorio =
        RepositorioPersonas.getInstance();

    private final Notificador notificador =
        new NotificadorEmail();

    public void registrar(String email, Persona persona) {
        Optional<Persona> existente =
            repositorio.buscarPorEmail(email);

        if (existente.isPresent()) {
            actualizarDatos(existente.get(), persona);
            System.out.println(
                "[REGISTRO] Persona actualizada: " + email
            );
        } else {
            String contrasena = generarContrasena();

            persona.setUsuario(
                new Usuario(email, contrasena)
            );

            persona.agregarMedioContacto(
                new MedioContacto(
                    TipoContacto.EMAIL,
                    email
                )
            );

            new Donante(persona);

            repositorio.guardar(email, persona);

            notificador.notificar(
                email,
                "Bienvenido a DonaTrack. Su usuario: "
                    + email
                    + " | Contrasena: "
                    + contrasena
            );

            System.out.println(
                "[REGISTRO] Persona creada: " + email
            );
        }
    }

    public Persona buscar(String email) {
        return repositorio.buscarPorEmail(email)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "No existe una persona con email "
                        + email
                ));
    }

    public List<Persona> todos() {
        return repositorio.todos();
    }

    public void eliminar(String email) {
        repositorio.eliminar(email);
    }

    private void actualizarDatos(
        Persona existente,
        Persona nueva
    ) {
        if (nueva.getDireccion() != null) {
            existente.setDireccion(
                nueva.getDireccion()
            );
        }
    }

    private String generarContrasena() {
        return UUID.randomUUID()
            .toString()
            .substring(0, 8);
    }
}
