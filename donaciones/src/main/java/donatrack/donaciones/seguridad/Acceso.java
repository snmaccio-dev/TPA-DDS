package donatrack.donaciones.seguridad;

import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.necesidad.Necesidad;
import donatrack.donaciones.domain.notificacion.Notificacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import io.javalin.http.ForbiddenResponse;

import java.util.Objects;
import java.util.Optional;

// Chequeos puros de propiedad: reciben el UsuarioActual y el recurso, y
// deciden si puede verlo/modificarlo. Centralizados aca para no repetirlos
// en cada handler. Lanzan ForbiddenResponse (403) y nunca filtran si
// existe o no el recurso (eso lo maneja el controller con 404).
public final class Acceso {

    private Acceso() {}

    public static void exigirPuedeVerDonacion(UsuarioActual usuario, Donacion donacion) {
        if (usuario.esAdministrador() || usuario.esServicioLogistica()) {
            return;
        }
        if (usuario.esDonante()) {
            Donante propio = perfilDonanteObligatorio(usuario);
            if (Objects.equals(idDe(donacion.getDonante()), idDe(propio))) {
                return;
            }
        }
        if (usuario.esBeneficiaria()) {
            Beneficiaria propia = perfilBeneficiariaObligatorio(usuario);
            Beneficiaria destinataria = donacion.getDestinatarioAsignado();
            if (destinataria != null
                && Objects.equals(idDe(destinataria), idDe(propia))) {
                return;
            }
        }
        throw new ForbiddenResponse("No podes acceder a esta donacion.");
    }

    public static void exigirPuedeVerNotificacion(UsuarioActual usuario, Notificacion notificacion) {
        // Las notificaciones para administradores son compartidas: cualquier
        // admin puede verlas y, si marca una como leida, queda leida para
        // todos. Las demas solo las ve el duenio del perfil asociado,
        // comparando por id del perfil (no por persona, porque ahora cada
        // perfil es un destinatario independiente).
        if (notificacion.esParaAdministradores()) {
            if (usuario.esAdministrador()) {
                return;
            }
            throw new ForbiddenResponse("No podes acceder a esta notificacion.");
        }
        Donante donanteNoti = notificacion.getDonante();
        if (donanteNoti != null && usuario.esDonante()) {
            Donante propio = perfilDonanteObligatorio(usuario);
            if (Objects.equals(idDe(donanteNoti), idDe(propio))) {
                return;
            }
        }
        Beneficiaria benefNoti = notificacion.getBeneficiaria();
        if (benefNoti != null && usuario.esBeneficiaria()) {
            Beneficiaria propia = perfilBeneficiariaObligatorio(usuario);
            if (Objects.equals(idDe(benefNoti), idDe(propia))) {
                return;
            }
        }
        throw new ForbiddenResponse("No podes acceder a esta notificacion.");
    }

    public static void exigirPuedeVerNecesidad(UsuarioActual usuario, Necesidad necesidad) {
        if (usuario.esAdministrador()) {
            return;
        }
        if (usuario.esBeneficiaria()) {
            Beneficiaria propia = perfilBeneficiariaObligatorio(usuario);
            if (Objects.equals(idDe(necesidad.getEntidad()), idDe(propia))) {
                return;
            }
        }
        throw new ForbiddenResponse("No podes acceder a esta necesidad.");
    }

    public static Donante perfilDonanteObligatorio(UsuarioActual usuario) {
        return usuario.donante().orElseThrow(() ->
            new ForbiddenResponse("La cuenta no tiene un perfil asociado."));
    }

    public static Beneficiaria perfilBeneficiariaObligatorio(UsuarioActual usuario) {
        return usuario.beneficiaria().orElseThrow(() ->
            new ForbiddenResponse("La cuenta no tiene un perfil asociado."));
    }

    public static Optional<Donante> perfilDonante(UsuarioActual usuario) {
        return usuario.donante();
    }

    public static Optional<Beneficiaria> perfilBeneficiaria(UsuarioActual usuario) {
        return usuario.beneficiaria();
    }

    private static Long idDe(Donante donante) {
        return donante == null ? null : donante.getId();
    }

    private static Long idDe(Beneficiaria beneficiaria) {
        return beneficiaria == null ? null : beneficiaria.getId();
    }
}
