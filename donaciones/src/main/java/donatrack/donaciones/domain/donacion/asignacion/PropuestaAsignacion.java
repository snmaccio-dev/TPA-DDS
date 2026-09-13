package donatrack.donaciones.domain.donacion.asignacion;

import donatrack.donaciones.domain.persona.Beneficiaria;

import java.time.LocalDateTime;
import java.util.List;

public class PropuestaAsignacion {

    private final long donacionId;
    private final List<Beneficiaria> candidatas;
    private final LocalDateTime fechaGeneracion;
    private final boolean huboCoincidencias;

    public PropuestaAsignacion(long donacionId,
                               List<Beneficiaria> candidatas,
                               LocalDateTime fechaGeneracion,
                               boolean huboCoincidencias) {
        this.donacionId = donacionId;
        this.candidatas = List.copyOf(candidatas);
        this.fechaGeneracion = fechaGeneracion;
        this.huboCoincidencias = huboCoincidencias;
    }

    public long getDonacionId() {
        return donacionId;
    }

    public List<Beneficiaria> getCandidatas() {
        return List.copyOf(candidatas);
    }

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    public boolean huboCoincidencias() {
        return huboCoincidencias;
    }
}