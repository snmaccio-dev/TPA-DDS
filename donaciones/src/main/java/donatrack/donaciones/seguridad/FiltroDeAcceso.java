package donatrack.donaciones.seguridad;

import donatrack.donaciones.domain.usuario.Usuario;
import donatrack.donaciones.repository.RepositorioUsuarios;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.security.RouteRole;

import java.util.Optional;
import java.util.Set;

// Filtro central de seguridad: valida token, arma UsuarioActual y lo guarda
// en el contexto. Si la ruta no declaro roles, la rechaza con 403 para que
// nunca quede una ruta publica por olvido.
public class FiltroDeAcceso {

    private static final String CLAVE_USUARIO = "usuarioActual";

    private final VerificadorDeTokens verificador;
    private final RepositorioUsuarios repositorioUsuarios;

    public FiltroDeAcceso(VerificadorDeTokens verificador, RepositorioUsuarios repositorioUsuarios) {
        this.verificador = verificador;
        this.repositorioUsuarios = repositorioUsuarios;
    }

    public void registrarEn(Javalin app) {
        app.beforeMatched(this::aplicar);
    }

    public void aplicar(Context ctx) {
        Set<? extends RouteRole> roles = ctx.routeRoles();
        if (roles == null || roles.isEmpty()) {
            System.err.println("[SEGURIDAD] Ruta sin roles declarados: " + ctx.method() + " " + ctx.path());
            throw new ForbiddenResponse("La ruta no declaro roles de acceso.");
        }

        if (roles.contains(RolDeAcceso.PUBLICO)) {
            return;
        }

        VerificadorDeTokens.TokenVerificado token =
            verificador.verificar(ctx.header("Authorization"));

        // Cada cuenta tiene que mapear a un unico perfil. CHOFER y
        // SERVICIO_LOGISTICA no son perfiles de la base, asi que no entran
        // en la cuenta.
        long perfiles = token.roles().stream()
            .filter(r -> r == RolDeAcceso.DONANTE
                || r == RolDeAcceso.BENEFICIARIA
                || r == RolDeAcceso.ADMINISTRADOR)
            .count();
        if (perfiles > 1) {
            throw new ForbiddenResponse(
                "La cuenta tiene mas de un perfil asignado. Cada perfil debe tener su propia cuenta.");
        }

        boolean tieneAlgunRolPermitido = token.roles().stream().anyMatch(roles::contains);
        if (!tieneAlgunRolPermitido) {
            throw new ForbiddenResponse(
                "La cuenta no tiene los roles necesarios para esta operacion.");
        }

        Optional<Usuario> local = repositorioUsuarios.buscarPorKeycloakId(token.keycloakId());
        UsuarioActual actual = new UsuarioActual(
            token.keycloakId(),
            token.email(),
            Set.copyOf(token.roles()),
            local.map(Usuario::getDonante).filter(d -> d != null),
            local.map(Usuario::getBeneficiaria).filter(b -> b != null)
        );
        ctx.attribute(CLAVE_USUARIO, actual);
    }

    // Helper estatico para leer el UsuarioActual desde los handlers.
    public static UsuarioActual usuarioDe(Context ctx) {
        UsuarioActual actual = ctx.attribute(CLAVE_USUARIO);
        if (actual == null) {
            throw new ForbiddenResponse("No hay usuario autenticado en el contexto.");
        }
        return actual;
    }
}