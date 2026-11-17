package donatrack.donaciones.service;

import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.repository.RepositorioPersonas;

import java.util.Optional;

public class PersonasService {

    private final RepositorioPersonas repositorio;

    public PersonasService(RepositorioPersonas repositorio) {
        this.repositorio = repositorio;
    }

    public Optional<PersonaJuridica> buscarJuridicaPorCuit(String cuit) {
        return repositorio.buscarJuridicaPorCuit(cuit);
    }
}
