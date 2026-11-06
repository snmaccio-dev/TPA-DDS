package donatrack.donaciones.service;

import donatrack.donaciones.domain.notificacion.EstadoEnvio;
import donatrack.donaciones.domain.notificacion.Notificacion;
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
            // GestorNotificaciones ya no propaga fallas de envio: nos devuelve
            // una Notificacion ENVIADA o FALLIDA y nosotros decidimos.
            Notificacion notificacion =
                gestorNotificaciones.notificarAusenciaPlataforma(donante);
            if (notificacion.getEstadoEnvio() == EstadoEnvio.ENVIADA) {
                // Solo registramos el aviso cuando salio bien, para que un
                // fallo del canal no lo consuma y el donante vuelva a intentarse.
                withTransaction(donante::registrarAvisoDeInactividad);
                avisados++;
            } else {
                fallidos.add(donante.getPersona().getDocumento() + ": "
                    + (notificacion.getDetalleError() == null
                        ? "no se pudo enviar la notificacion"
                        : notificacion.getDetalleError()));
            }
        }
        return new ResultadoCorrida(avisados, fallidos);
    }
}
