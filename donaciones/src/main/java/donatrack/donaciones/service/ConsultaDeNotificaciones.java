package donatrack.donaciones.service;

import donatrack.donaciones.domain.notificacion.EstadoEnvio;
import donatrack.donaciones.domain.notificacion.Notificacion;
import donatrack.donaciones.domain.notificacion.TipoNotificacion;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import donatrack.donaciones.seguridad.UsuarioActual;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.http.ForbiddenResponse;

import java.time.LocalDateTime;
import java.util.List;

// Lecturas sobre el repositorio de notificaciones. Cada cuenta tiene un
// unico perfil, asi que cada metodo se resuelve con una sola llamada al
// repo y la paginacion es la del repositorio: no juntamos listas.
public class ConsultaDeNotificaciones implements WithSimplePersistenceUnit {

    private final RepositorioNotificaciones repositorio;

    public ConsultaDeNotificaciones(RepositorioNotificaciones repositorio) {
        this.repositorio = repositorio;
    }

    public List<Notificacion> propiasDe(UsuarioActual usuario,
                                        boolean soloNoLeidas,
                                        int pagina,
                                        int tamanio) {
        if (usuario.esAdministrador()) {
            return repositorio.deAdministradores(soloNoLeidas, pagina, tamanio);
        }
        if (usuario.donante().isPresent()) {
            return repositorio.deDonante(usuario.donante().get(), soloNoLeidas, pagina, tamanio);
        }
        if (usuario.beneficiaria().isPresent()) {
            return repositorio.deBeneficiaria(
                usuario.beneficiaria().get(), soloNoLeidas, pagina, tamanio);
        }
        throw new ForbiddenResponse("La cuenta no tiene un perfil asociado.");
    }

    public long cantidadNoLeidasDe(UsuarioActual usuario) {
        if (usuario.esAdministrador()) {
            return repositorio.cantidadNoLeidasAdministradores();
        }
        if (usuario.donante().isPresent()) {
            return repositorio.cantidadNoLeidasDeDonante(usuario.donante().get());
        }
        if (usuario.beneficiaria().isPresent()) {
            return repositorio.cantidadNoLeidasDeBeneficiaria(usuario.beneficiaria().get());
        }
        throw new ForbiddenResponse("La cuenta no tiene un perfil asociado.");
    }

    public List<Notificacion> todas(EstadoEnvio estadoEnvio,
                                    TipoNotificacion tipo,
                                    int pagina,
                                    int tamanio) {
        return repositorio.todas(estadoEnvio, tipo, pagina, tamanio);
    }

    public Notificacion buscarPorId(long id) {
        return repositorio.buscarPorId(id).orElseThrow(() ->
            new RecursoInexistenteException("No existe la notificacion con id " + id));
    }

    public void marcarLeida(Notificacion notificacion) {
        withTransaction(notificacion::marcarLeida);
    }

    // Marca todas las propias del usuario como leidas, en un unico update JPQL.
    // No usamos el tope de 100 del listado, porque aca el bulk es sobre todo
    // lo no leido. Para las de administradores esto afecta a todos los admins:
    // si uno las marca leidas, quedan leidas para todos.
    public void marcarTodasLeidasDe(UsuarioActual usuario) {
        LocalDateTime ahora = LocalDateTime.now();
        withTransaction(() -> {
            if (usuario.esAdministrador()) {
                repositorio.marcarLeidasDeAdministradores(ahora);
                return;
            }
            if (usuario.donante().isPresent()) {
                repositorio.marcarLeidasDeDonante(usuario.donante().get(), ahora);
                return;
            }
            if (usuario.beneficiaria().isPresent()) {
                repositorio.marcarLeidasDeBeneficiaria(usuario.beneficiaria().get(), ahora);
                return;
            }
            throw new ForbiddenResponse("La cuenta no tiene un perfil asociado.");
        });
    }
}
