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

public class BandejaDeNotificacionesService implements WithSimplePersistenceUnit {

    private final RepositorioNotificaciones repositorio;

    public BandejaDeNotificacionesService(RepositorioNotificaciones repositorio) {
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
