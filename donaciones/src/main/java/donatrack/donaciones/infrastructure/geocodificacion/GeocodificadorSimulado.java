package donatrack.donaciones.infrastructure.geocodificacion;

import donatrack.donaciones.domain.geocodificacion.Geocodificador;
import donatrack.donaciones.domain.persona.Coordenadas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// Geocodificador sin red, para los tests y para correr el sistema sin acceso a Internet
// (GEOCODIFICADOR=simulado). Conoce solo las direcciones que se le cargan; las demas no
// las encuentra. Con fallar() simula que el proveedor no responde. Registra las
// direcciones consultadas para que los tests puedan verificarlas.
public class GeocodificadorSimulado implements Geocodificador {

    private final Map<String, Coordenadas> conocidas = new ConcurrentHashMap<>();
    private final List<String> consultadas = new ArrayList<>();
    private volatile boolean falla;

    public GeocodificadorSimulado conocer(String direccion, double latitud, double longitud) {
        conocidas.put(clave(direccion), new Coordenadas(latitud, longitud));
        return this;
    }

    public GeocodificadorSimulado fallar(boolean falla) {
        this.falla = falla;
        return this;
    }

    @Override
    public Optional<Coordenadas> geocodificar(String direccion) {
        synchronized (consultadas) {
            consultadas.add(direccion);
        }
        if (falla) {
            throw new IllegalStateException("Geocodificador simulado sin servicio.");
        }
        return Optional.ofNullable(conocidas.get(clave(direccion)));
    }

    public List<String> getConsultadas() {
        synchronized (consultadas) {
            return List.copyOf(consultadas);
        }
    }

    private static String clave(String direccion) {
        return direccion == null ? "" : direccion.trim().toLowerCase();
    }
}
