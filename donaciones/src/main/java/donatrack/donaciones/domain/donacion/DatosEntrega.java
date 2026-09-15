package donatrack.donaciones.domain.donacion;

import java.time.LocalDateTime;

public class DatosEntrega {

    private LocalDateTime fechaHora;
    private String patenteCamion;

    public DatosEntrega(LocalDateTime fechaHora, String patenteCamion) {
        this.fechaHora = fechaHora;
        this.patenteCamion = patenteCamion;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getPatenteCamion() {
        return patenteCamion;
    }

    public void setPatenteCamion(String patenteCamion) {
        this.patenteCamion = patenteCamion;
    }
}
