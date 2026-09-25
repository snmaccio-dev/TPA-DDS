package donatrack.donaciones.domain.donacion;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import java.time.LocalDateTime;

@Embeddable
public class DatosEntrega {

    @Column(name = "fecha_hora_entrega")
    private LocalDateTime fechaHora;

    @Column(name = "patente_camion")
    private String patenteCamion;

    protected DatosEntrega() {
    }

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
