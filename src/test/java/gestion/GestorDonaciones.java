package gestion;

import donatrack.model.donacion.Donacion;
import donatrack.notificacion.Notificador;
import donatrack.notificacion.NotificadorWhatsApp;
import donatrack.repositorio.RepositorioDonaciones;

import java.util.List;

public class GestorDonaciones {

    private final SegmentadorDonaciones segmentacion = new SegmentadorDonaciones();
    private final Notificador notificador = new NotificadorWhatsApp();
    private final RepositorioDonaciones repositorio = RepositorioDonaciones.getInstance();

    public List<Donacion> todas() {
        return repositorio.todas();
    }
}
