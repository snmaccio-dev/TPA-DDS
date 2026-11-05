package donatrack.donaciones.controller.dto;

import donatrack.donaciones.seguridad.RolDeAcceso;
import donatrack.donaciones.seguridad.UsuarioActual;

import java.util.List;

public record UsuarioActualDTO(
    String keycloakId,
    String email,
    List<String> roles,
    Long donanteId,
    Long beneficiariaId,
    String nombre
) {

    public static UsuarioActualDTO desde(UsuarioActual usuario) {
        return new UsuarioActualDTO(
            usuario.keycloakId(),
            usuario.email(),
            usuario.roles().stream().map(RolDeAcceso::name).sorted().toList(),
            usuario.donante().map(d -> d.getId()).orElse(null),
            usuario.beneficiaria().map(b -> b.getId()).orElse(null),
            usuario.donante().map(d -> d.getPersona().getNombreDisplay())
                .or(() -> usuario.beneficiaria().map(b -> b.getPersona().getNombreDisplay()))
                .orElse(usuario.email())
        );
    }
}