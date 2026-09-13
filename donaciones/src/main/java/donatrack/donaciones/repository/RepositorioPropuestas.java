package donatrack.donaciones.repository;

import donatrack.donaciones.domain.donacion.asignacion.PropuestaAsignacion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RepositorioPropuestas {

    private final Map<Long, PropuestaAsignacion> propuestas = new HashMap<>();

    public void guardar(PropuestaAsignacion propuesta) {
        propuestas.put(propuesta.getDonacionId(), propuesta);
    }

    public Optional<PropuestaAsignacion> buscarPorDonacion(long donacionId) {
        return Optional.ofNullable(propuestas.get(donacionId));
    }

    public List<PropuestaAsignacion> todas() {
        return new ArrayList<>(propuestas.values());
    }

    public void eliminar(long donacionId) {
        propuestas.remove(donacionId);
    }
}