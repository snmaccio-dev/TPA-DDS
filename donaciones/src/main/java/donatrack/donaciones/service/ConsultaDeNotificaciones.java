package donatrack.donaciones.service;

import donatrack.donaciones.domain.notificacion.EstadoEnvio;
import donatrack.donaciones.domain.notificacion.Notificacion;
import donatrack.donaciones.domain.notificacion.TipoNotificacion;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import donatrack.donaciones.seguridad.UsuarioActual;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// Lecturas sobre el repositorio de notificaciones, con la logica para
// combinar perfiles cuando el UsuarioActual tiene mas de uno (por ejemplo,
// alguien que es DONANTE y BENEFICIARIA, o DONANTE y ADMINISTRADOR).
public class ConsultaDeNotificaciones implements WithSimplePersistenceUnit {

    private final RepositorioNotificaciones repositorio;

    public ConsultaDeNotificaciones(RepositorioNotificaciones repositorio) {
        this.repositorio = repositorio;
    }

    public List<Notificacion> propiasDe(UsuarioActual usuario,
                                        boolean soloNoLeidas,
                                        int pagina,
                                        int tamanio) {
        List<Notificacion> combinadas = new ArrayList<>();
        Optional<Persona> comoDonante = usuario.donante().map(d -> d.getPersona());
        Optional<Persona> comoBeneficiaria = usuario.beneficiaria().map(b -> b.getPersona());

        comoDonante.ifPresent(persona -> combinadas.addAll(
            repositorio.deDestinatario(persona, soloNoLeidas, pagina, tamanio)));
        comoBeneficiaria
            .filter(persona -> comoDonante.isEmpty() || !persona.getId().equals(comoDonante.get().getId()))
            .ifPresent(persona -> combinadas.addAll(
                repositorio.deDestinatario(persona, soloNoLeidas, pagina, tamanio)));
        if (usuario.esAdministrador()) {
            combinadas.addAll(repositorio.deAdministradores(soloNoLeidas, pagina, tamanio));
        }

        combinadas.sort(Comparator.comparing(Notificacion::getFechaHoraCreacion).reversed());
        return combinadas;
    }

    public long cantidadNoLeidasDe(UsuarioActual usuario) {
        long total = 0;
        Optional<Persona> comoDonante = usuario.donante().map(d -> d.getPersona());
        Optional<Persona> comoBeneficiaria = usuario.beneficiaria().map(b -> b.getPersona());
        if (comoDonante.isPresent()) {
            total += repositorio.cantidadNoLeidas(comoDonante.get());
        }
        if (comoBeneficiaria.isPresent()
            && (comoDonante.isEmpty()
                || !comoBeneficiaria.get().getId().equals(comoDonante.get().getId()))) {
            total += repositorio.cantidadNoLeidas(comoBeneficiaria.get());
        }
        if (usuario.esAdministrador()) {
            total += repositorio.cantidadNoLeidasAdministradores();
        }
        return total;
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

    // Marca todas las propias del usuario como leidas. Para las de
    // administradores esto afecta a todos los admins: las lee uno, quedan
    // leidas para todos.
    public void marcarTodasLeidasDe(UsuarioActual usuario) {
        withTransaction(() -> {
            Optional<Persona> comoDonante = usuario.donante().map(d -> d.getPersona());
            Optional<Persona> comoBeneficiaria = usuario.beneficiaria().map(b -> b.getPersona());
            comoDonante.ifPresent(persona ->
                repositorio.deDestinatario(persona, true, 0, 1000)
                    .forEach(Notificacion::marcarLeida));
            comoBeneficiaria
                .filter(persona ->
                    comoDonante.isEmpty() || !persona.getId().equals(comoDonante.get().getId()))
                .ifPresent(persona ->
                    repositorio.deDestinatario(persona, true, 0, 1000)
                        .forEach(Notificacion::marcarLeida));
            if (usuario.esAdministrador()) {
                repositorio.deAdministradores(true, 0, 1000)
                    .forEach(Notificacion::marcarLeida);
            }
        });
    }
}
