package donatrack.donaciones.service;

import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.usuario.Usuario;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioUsuarios;
import donatrack.donaciones.seguridad.ProveedorDeIdentidad;
import donatrack.donaciones.seguridad.RolDeAcceso;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

// Orquesta el alta en Keycloak y la persistencia local, en ese orden, con
// rollback en Keycloak si falla la persistencia. Las contrasenas viajan por
// parametro y se reenvian al proveedor en una unica llamada: no se guardan
// ni se loguean.
public class GestorRegistro implements WithSimplePersistenceUnit {

    private static final int LARGO_MINIMO_CONTRASENA = 8;
    private static final String MENSAJE_PERSONA_DUPLICADA =
        "Ya existe una persona registrada con ese documento. "
            + "Si te dieron de alta en el deposito, revisa el correo de bienvenida.";

    private final ProveedorDeIdentidad proveedor;
    private final RepositorioPersonas repositorioPersonas;
    private final RepositorioDonantes repositorioDonantes;
    private final RepositorioEntidades repositorioEntidades;
    private final RepositorioUsuarios repositorioUsuarios;

    public GestorRegistro(ProveedorDeIdentidad proveedor,
                          RepositorioPersonas repositorioPersonas,
                          RepositorioDonantes repositorioDonantes,
                          RepositorioEntidades repositorioEntidades,
                          RepositorioUsuarios repositorioUsuarios) {
        this.proveedor = proveedor;
        this.repositorioPersonas = repositorioPersonas;
        this.repositorioDonantes = repositorioDonantes;
        this.repositorioEntidades = repositorioEntidades;
        this.repositorioUsuarios = repositorioUsuarios;
    }

    public Donante registrarDonante(Persona persona, String email, String contrasena) {
        validarEmail(email);
        validarContrasena(contrasena);
        if (persona == null) {
            throw new DatosInvalidosException("Los datos de la persona son obligatorios.");
        }

        if (repositorioPersonas.buscarPorDocumento(persona.getDocumento()).isPresent()) {
            throw new ConflictoDeNegocioException(MENSAJE_PERSONA_DUPLICADA);
        }

        String keycloakId = proveedor.crearUsuario(email, contrasena, RolDeAcceso.DONANTE);

        Donante[] donanteCreado = new Donante[1];
        try {
            withTransaction(() -> {
                persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, email));
                repositorioPersonas.guardar(persona);
                Donante donante = new Donante(persona);
                repositorioDonantes.guardar(donante);
                repositorioUsuarios.guardar(Usuario.deDonante(keycloakId, donante));
                donanteCreado[0] = donante;
            });
        } catch (RuntimeException e) {
            intentarEliminarUsuario(keycloakId);
            throw e;
        }
        return donanteCreado[0];
    }

    public Beneficiaria registrarBeneficiaria(PersonaJuridica juridica,
                                              DatosEntidad datos,
                                              String email,
                                              String contrasena) {
        validarEmail(email);
        validarContrasena(contrasena);
        if (juridica == null) {
            throw new DatosInvalidosException("Los datos de la persona juridica son obligatorios.");
        }
        if (datos == null) {
            throw new DatosInvalidosException(
                "Los datos de la beneficiaria (direccion y telefono) son obligatorios.");
        }

        if (repositorioPersonas.buscarPorDocumento(juridica.getCuit()).isPresent()) {
            throw new ConflictoDeNegocioException(MENSAJE_PERSONA_DUPLICADA);
        }

        String keycloakId = proveedor.crearUsuario(email, contrasena, RolDeAcceso.BENEFICIARIA);

        Beneficiaria[] creada = new Beneficiaria[1];
        try {
            withTransaction(() -> {
                juridica.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, email));
                datos.aplicarA(juridica);
                repositorioPersonas.guardar(juridica);
                Beneficiaria beneficiaria = new Beneficiaria(juridica);
                repositorioEntidades.guardar(beneficiaria);
                repositorioUsuarios.guardar(Usuario.deBeneficiaria(keycloakId, beneficiaria));
                creada[0] = beneficiaria;
            });
        } catch (RuntimeException e) {
            intentarEliminarUsuario(keycloakId);
            throw e;
        }
        return creada[0];
    }

    private void intentarEliminarUsuario(String keycloakId) {
        try {
            proveedor.eliminarUsuario(keycloakId);
        } catch (RuntimeException ignorado) {
            // Si Keycloak tampoco pudo limpiar, dejamos la cuenta huerfana
            // para revision manual: lo importante es propagar el error original.
            System.err.println("[REGISTRO] Rollback en Keycloak fallo para el usuario "
                + keycloakId + ": " + ignorado.getMessage());
        }
    }

    private static void validarEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new DatosInvalidosException("El email es obligatorio y debe ser valido.");
        }
    }

    private static void validarContrasena(String contrasena) {
        if (contrasena == null || contrasena.length() < LARGO_MINIMO_CONTRASENA) {
            throw new DatosInvalidosException(
                "La contrasena debe tener al menos " + LARGO_MINIMO_CONTRASENA + " caracteres."
            );
        }
    }
}