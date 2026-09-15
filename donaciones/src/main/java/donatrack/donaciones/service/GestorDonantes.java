package donatrack.donaciones.service;

import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.repository.RepositorioDonantes;

import java.time.LocalDate;
import java.util.List;

public class GestorDonantes {

    public static final int DIAS_SIN_INTERACCION = 20;

    private final RepositorioDonantes repositorio;
    private final GestorNotificaciones gestorNotificaciones;

    public GestorDonantes(RepositorioDonantes repositorio,
                          GestorNotificaciones gestorNotificaciones) {
        this.repositorio = repositorio;
        this.gestorNotificaciones = gestorNotificaciones;
    }

    public List<Donante> inactivos() {
        LocalDate limite = LocalDate.now().minusDays(DIAS_SIN_INTERACCION);
        return repositorio.queRequierenAvisoDeInactividad(limite);
    }

    public int avisarInactivos() {
        int avisados = 0;
        for (Donante donante : inactivos()) {
            try {
                gestorNotificaciones.notificarAusenciaPlataforma(donante.getPersona());
                // Recien cuando la notificacion salio bien registramos el aviso, asi un
                // fallo del canal no consume el aviso y el donante vuelve a intentarse.
                donante.registrarAvisoDeInactividad();
                avisados++;
            } catch (RuntimeException e) {
                System.err.println(
                    "[AVISO-INACTIVIDAD] Fallo para "
                        + donante.getPersona().getDocumento() + ": " + e.getMessage()
                );
            }
        }
        return avisados;
    }
}