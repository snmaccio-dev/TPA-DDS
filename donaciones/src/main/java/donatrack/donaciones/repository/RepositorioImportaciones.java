package donatrack.donaciones.repository;

import donatrack.donaciones.domain.importacion.ImportacionCsv;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;
import java.util.Optional;

public class RepositorioImportaciones implements WithSimplePersistenceUnit {

    public void guardar(ImportacionCsv importacion) {
        persist(importacion);
    }

    public Optional<ImportacionCsv> buscarPorId(long id) {
        return Optional.ofNullable(find(ImportacionCsv.class, id));
    }

    public List<ImportacionCsv> ultimas(int cantidad) {
        return createQuery(
            "select i from ImportacionCsv i order by i.fechaInicio desc, i.id desc",
            ImportacionCsv.class)
            .setMaxResults(cantidad)
            .getResultList();
    }
}
