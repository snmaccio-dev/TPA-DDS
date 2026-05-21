package donatrack.servicio;

import donatrack.model.donacion.Bien;
import donatrack.model.donacion.Donacion;
import donatrack.model.persona.Persona;
import donatrack.notificacion.NotificadorDonacionObserver;
import donatrack.notificacion.ServicioNotificaciones;
import donatrack.repositorio.RepositorioDonaciones;

import java.util.List;

public class ServicioDonaciones {

    private final ServicioSegmentacion segmentacion = new ServicioSegmentacion();
    private final ServicioNotificaciones servicioNotificaciones = new ServicioNotificaciones();
    private final RepositorioDonaciones repositorio = RepositorioDonaciones.getInstance();

    public List<Donacion> ingresarDonacion(List<Bien> bienes, Persona donante) {
        List<Donacion> donaciones = segmentacion.segmentar(bienes, donante);
        NotificadorDonacionObserver observer = new NotificadorDonacionObserver(donante, servicioNotificaciones);
        for (Donacion d : donaciones) {
            d.agregarObserver(observer);
        }
        return donaciones;
    }

    public List<Donacion> todas() {
        return repositorio.todas();
    }
}
