package donatrack.donaciones.repository;

import donatrack.donaciones.domain.persona.Donante;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RepositorioDonantes {

    private final Map<String, Donante> porDocumento = new HashMap<>();

    public void guardar(Donante donante) {
        porDocumento.put(donante.getPersona().getDocumento(), donante);
    }

    public Optional<Donante> buscarPorDocumento(String documento) {
        return Optional.ofNullable(porDocumento.get(documento));
    }

    public List<Donante> todos() {
        return new ArrayList<>(porDocumento.values());
    }

    public List<Donante> queRequierenAvisoDeInactividad(LocalDate limite) {
        return porDocumento.values().stream()
            .filter(donante -> donante.requiereAvisoDeInactividad(limite))
            .toList();
    }
}