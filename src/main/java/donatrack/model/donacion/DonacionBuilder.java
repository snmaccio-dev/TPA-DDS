package donatrack.model.donacion;

import donatrack.model.catalogo.Subcategoria;

import java.util.ArrayList;
import java.util.List;

// Builder — facilita la construccion de Donacion con lista de bienes
public class DonacionBuilder {

    private Subcategoria subcategoria;
    private final List<Bien> bienes = new ArrayList<>();

    public DonacionBuilder conSubcategoria(Subcategoria subcategoria) {
        this.subcategoria = subcategoria;
        return this;
    }

    public DonacionBuilder agregarBien(Bien bien) {
        bienes.add(bien);
        return this;
    }

    public DonacionBuilder agregarBienes(List<Bien> nuevosBienes) {
        bienes.addAll(nuevosBienes);
        return this;
    }

    public Donacion build() {
        if (subcategoria == null) {
            throw new IllegalStateException("La subcategoria es obligatoria para construir una Donacion.");
        }
        if (bienes.isEmpty()) {
            throw new IllegalStateException("La donacion debe tener al menos un bien.");
        }
        return new Donacion(bienes, subcategoria);
    }
}
