package donatrack.donaciones.service;

import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.repository.RepositorioDonantes;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GestorDonantes implements WithSimplePersistenceUnit {

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

    public ResultadoCorrida avisarInactivos() {
        int avisados = 0;
        List<String> fallidos = new ArrayList<>();
        for (Donante donante : inactivos()) {
            try {
                gestorNotificaciones.notificarAusenciaPlataforma(donante.getPersona());
                // Recien cuando la notificacion salio bien registramos el aviso, asi un
                // fallo del canal no consume el aviso y el donante vuelve a intentarse.
                withTransaction(donante::registrarAvisoDeInactividad);
                avisados++;
            } catch (RuntimeException e) {
                fallidos.add(donante.getPersona().getDocumento() + ": " + e.getMessage());
            }
        }
        return new ResultadoCorrida(avisados, fallidos);
    }
}
