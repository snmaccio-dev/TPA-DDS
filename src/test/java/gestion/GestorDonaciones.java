package gestion;

import donatrack.model.donacion.Donacion;
import donatrack.repositorio.RepositorioDonaciones;

import java.util.List;

public class GestorDonaciones {

    private final SegmentadorDonaciones segmentacion = new SegmentadorDonaciones();
    private final GestorPersonas.ServicioNotificaciones servicioNotificaciones = new GestorPersonas.ServicioNotificaciones();
    private final RepositorioDonaciones repositorio = RepositorioDonaciones.getInstance();

    public List<Donacion> todas() {
        return repositorio.todas();
    }
}
