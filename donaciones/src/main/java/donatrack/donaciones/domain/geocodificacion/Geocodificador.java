package donatrack.donaciones.domain.geocodificacion;

import donatrack.donaciones.domain.persona.Coordenadas;

import java.util.Optional;

// Traduce una direccion postal a coordenadas. Vacio si el proveedor no encontro la
// direccion; una RuntimeException si no se le pudo consultar (red, timeout, respuesta
// invalida). Quien lo usa decide que hacer en cada caso.
public interface Geocodificador {

    Optional<Coordenadas> geocodificar(String direccion);
}
