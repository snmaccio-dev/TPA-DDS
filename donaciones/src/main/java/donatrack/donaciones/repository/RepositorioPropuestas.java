package donatrack.donaciones.repository;

import donatrack.donaciones.domain.donacion.asignacion.PropuestaAsignacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;
import java.util.Optional;

public class RepositorioPropuestas implements WithSimplePersistenceUnit {

    public void guardar(PropuestaAsignacion propuesta) {
        persist(propuesta);
    }

    public Optional<PropuestaAsignacion> buscarPorDonacion(long donacionId) {
        return createQuery(
            "select p from PropuestaAsignacion p where p.donacionId = :donacionId",
            PropuestaAsignacion.class)
            .setParameter("donacionId", donacionId)
            .getResultList()
            .stream()
            .findFirst();
    }

    public List<PropuestaAsignacion> todas() {
        return createQuery("select p from PropuestaAsignacion p", PropuestaAsignacion.class)
            .getResultList();
    }

    public void eliminar(long donacionId) {
        buscarPorDonacion(donacionId).ifPresent(this::remove);
    }

    // Saca a la beneficiaria de la lista de candidatas de todas las propuestas
    // que la tengan. Devuelve cuantas propuestas quedaron afectadas. La
    // mutacion de la coleccion @ManyToMany la hace Hibernate al commitear
    // la transaccion actual (no usamos delete nativo para no romper el
    // @OrderColumn).
    public int quitarComoCandidata(Beneficiaria beneficiaria) {
        if (beneficiaria == null || beneficiaria.getId() == null) {
            return 0;
        }
        List<PropuestaAsignacion> conEsaCandidata = createQuery(
            "select p from PropuestaAsignacion p join p.candidatas c where c = :b",
            PropuestaAsignacion.class)
            .setParameter("b", beneficiaria)
            .getResultList();
        int afectadas = 0;
        for (PropuestaAsignacion p : conEsaCandidata) {
            if (p.removerCandidata(beneficiaria)) {
                afectadas++;
            }
        }
        return afectadas;
    }
}

