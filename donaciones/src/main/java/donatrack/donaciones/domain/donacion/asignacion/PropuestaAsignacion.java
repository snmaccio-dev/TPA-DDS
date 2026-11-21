package donatrack.donaciones.domain.donacion.asignacion;

import donatrack.donaciones.domain.persona.Beneficiaria;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@Entity
@Table(name = "Propuesta_Asignacion")
public class PropuestaAsignacion {

    //Clave primaria propia para la tabla de propuestas
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "propuesta_id")
    private Long id;

    @Column(name = "donacion_id")
    private long donacionId;

    // Resultado de los algoritmos: cada fila es una entidad, de que algoritmo sale (o
    // AMBOS si es coincidencia) y su posicion en ese ranking. Se reutiliza la tabla de la
    // version anterior (mismas columnas propuesta_id, beneficiaria_id y orden, mas
    // algoritmo y posicion), asi hbm2ddl=update la migra sin perder las filas.
    @ElementCollection
    @CollectionTable(
        name = "candidatas_por_propuesta",
        joinColumns = @JoinColumn(name = "propuesta_id")
    )
    @OrderColumn(name = "orden")
    private List<CandidataPropuesta> candidatas;

    @Column(name = "fecha_generacion")
    private LocalDateTime fechaGeneracion;

    @Column(name = "hubo_coincidencias")
    private boolean huboCoincidencias;

    protected PropuestaAsignacion() {
    }

    private PropuestaAsignacion(long donacionId,
                                List<CandidataPropuesta> candidatas,
                                LocalDateTime fechaGeneracion,
                                boolean huboCoincidencias) {
        this.donacionId = donacionId;
        // Evitamos List.copyOf() en constructores JPA para que Hibernate pueda manejar sus propias colecciones
        this.candidatas = new ArrayList<>(candidatas);
        this.fechaGeneracion = fechaGeneracion;
        this.huboCoincidencias = huboCoincidencias;
    }

    // Arma la propuesta con los rankings de los dos algoritmos. Si hay entidades en
    // ambos, quedan solo esas coincidencias (en el orden del ranking de compatibilidad y
    // numeradas 1..n). Si no hay ninguna, se guardan las dos ejecuciones completas, cada
    // candidata con su algoritmo y su posicion en ese ranking.
    public static PropuestaAsignacion desdeRankings(long donacionId,
                                                    List<Beneficiaria> rankingCompatibilidad,
                                                    List<Beneficiaria> rankingPrioridad,
                                                    LocalDateTime fechaGeneracion) {
        List<Beneficiaria> coincidencias = rankingCompatibilidad.stream()
            .filter(b -> rankingPrioridad.stream()
                .anyMatch(otra -> CandidataPropuesta.mismaBeneficiaria(b, otra)))
            .toList();

        if (!coincidencias.isEmpty()) {
            return new PropuestaAsignacion(donacionId,
                numeradas(coincidencias, OrigenCandidata.AMBOS), fechaGeneracion, true);
        }

        List<CandidataPropuesta> ambasEjecuciones = new ArrayList<>(
            numeradas(rankingCompatibilidad, OrigenCandidata.COMPATIBILIDAD_SEMANTICA));
        ambasEjecuciones.addAll(
            numeradas(rankingPrioridad, OrigenCandidata.PRIORIDAD_SUBATENDIDOS));

        return new PropuestaAsignacion(donacionId, ambasEjecuciones, fechaGeneracion, false);
    }

    private static List<CandidataPropuesta> numeradas(List<Beneficiaria> ranking,
                                                      OrigenCandidata origen) {
        return IntStream.range(0, ranking.size())
            .mapToObj(i -> new CandidataPropuesta(ranking.get(i), origen, i + 1))
            .toList();
    }

    public long getDonacionId() {
        return donacionId;
    }

    // Saca a la beneficiaria de la lista de candidatas (si estaba). Pensado
    // para cuando se borra una entidad: la propuesta sigue existiendo con
    // las demas candidatas en vez de quedar huerfana. Las demas conservan la
    // posicion que les dio el algoritmo.
    public boolean removerCandidata(Beneficiaria beneficiaria) {
        return candidatas.removeIf(candidata -> candidata.esDe(beneficiaria));
    }

    public boolean esCandidata(Beneficiaria beneficiaria) {
        return candidatas.stream().anyMatch(candidata -> candidata.esDe(beneficiaria));
    }

    // Todas las entidades propuestas, sin repetir: las coincidencias, o el ranking de
    // compatibilidad seguido del de prioridad.
    public List<Beneficiaria> getCandidatas() {
        return candidatas.stream().map(CandidataPropuesta::getBeneficiaria).toList();
    }

    // Los rankings tal como hay que mostrarlos: uno solo (AMBOS) si hubo coincidencias,
    // o uno por algoritmo si no. Cada lista viene ordenada por posicion.
    public List<OrigenCandidata> getOrigenes() {
        return huboCoincidencias
            ? List.of(OrigenCandidata.AMBOS)
            : List.of(OrigenCandidata.COMPATIBILIDAD_SEMANTICA,
                      OrigenCandidata.PRIORIDAD_SUBATENDIDOS);
    }

    public List<CandidataPropuesta> candidatasDe(OrigenCandidata origen) {
        return candidatas.stream()
            .filter(candidata -> candidata.getOrigen() == origen)
            .sorted(Comparator.comparing(CandidataPropuesta::getPosicion))
            .toList();
    }

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    public boolean huboCoincidencias() {
        return huboCoincidencias;
    }
}
