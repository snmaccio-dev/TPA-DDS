package donatrack.donaciones.service;

import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.notificacion.Notificador;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.usuario.Usuario;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioPersonas;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GestorPersonas implements WithSimplePersistenceUnit {

    private final RepositorioPersonas repositorio =
        RepositorioPersonas.getInstance();

    private final RepositorioDonantes repositorioDonantes;
    private final Notificador notificador;

    public GestorPersonas(Notificador notificador, RepositorioDonantes repositorioDonantes) {
        this.notificador = notificador;
        this.repositorioDonantes = repositorioDonantes;
    }

    public void registrar(Persona persona, String email) {
        String[] emailBienvenida = new String[1];

        withTransaction(() -> {
            Optional<Persona> existente = repositorio.buscarPorDocumento(persona.getDocumento());

            if (existente.isPresent()) {
                Persona actual = existente.get();
                if (!actual.tieneContacto(TipoContacto.EMAIL, email)) {
                    actual.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, email));
                }
                if (!actual.tieneRol(Donante.class)) {
                    Donante donante = new Donante(actual);
                    repositorioDonantes.guardar(donante);
                }
                System.out.println("[REGISTRO] Persona ya existente: " + persona.getDocumento());
                return;
            }

            String contrasena = generarContrasena();
            persona.setUsuario(new Usuario(email, contrasena));
            persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, email));
            Donante donante = new Donante(persona);
            repositorio.guardar(persona);
            repositorioDonantes.guardar(donante);

            emailBienvenida[0] = "Bienvenido a DonaTrack. Su usuario: " + email
                + " | Contrasena: " + contrasena;

            System.out.println("[REGISTRO] Persona creada: " + persona.getDocumento());
        });

        if (emailBienvenida[0] != null) {
            notificador.notificar(email, emailBienvenida[0]);
        }
    }

    public Persona buscarPorDocumento(String documento) {
        return repositorio.buscarPorDocumento(documento)
            .orElseThrow(() ->
                new RecursoInexistenteException(
                    "No existe una persona con documento " + documento
                ));
    }

    public Optional<Persona> buscarPorEmail(String email) {
        return repositorio.buscarPorEmail(email);
    }

    public List<Persona> todos() {
        return repositorio.todos();
    }

    public void eliminar(String documento) {
        withTransaction(() ->
            repositorio.buscarPorDocumento(documento).ifPresent(repositorio::eliminar)
        );
    }

    private String generarContrasena() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
