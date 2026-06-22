package servicio;

import donatrack.model.donacion.Bien;
import donatrack.model.donacion.Donacion;
import donatrack.model.persona.Persona;
import donatrack.notificacion.NotificadorDonacionObserver;
import donatrack.repositorio.RepositorioDonaciones;

import java.util.List;

public class ServicioDonaciones {

    private final ServicioSegmentacion segmentacion = new ServicioSegmentacion();
    private final ServicioPersonas.ServicioNotificaciones servicioNotificaciones = new ServicioPersonas.ServicioNotificaciones();
    private final RepositorioDonaciones repositorio = RepositorioDonaciones.getInstance();

    public List<Donacion> todas() {
        return repositorio.todas();
    }
}
