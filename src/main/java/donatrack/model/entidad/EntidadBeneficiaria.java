package donatrack.model.entidad;

import donatrack.model.necesidad.Necesidad;
import donatrack.model.persona.Persona;
import donatrack.model.donacion.Donacion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EntidadBeneficiaria extends Persona {

    private String razonSocial;
    private List<Necesidad> necesidades = new ArrayList<>();
    private List<Donacion> donacionesRecibidas = new ArrayList<>();

    public EntidadBeneficiaria(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public void registrarNecesidad(Necesidad necesidad) {
        necesidades.add(necesidad);
    }

    public void registrarDonacionRecibida(Donacion donacion) {
        donacionesRecibidas.add(donacion);
    }

    public long cantidadDonacionesUltimoTrimestre() {
        LocalDate limite = LocalDate.now().minusMonths(3);

        return donacionesRecibidas.stream()
            .filter(d -> d.getFechaEntrega() != null)
            .filter(d -> !d.getFechaEntrega().isBefore(limite))
            .count();
    }

    public List<Necesidad> getNecesidades() {
        return necesidades;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public List<Donacion> getDonacionesRecibidas() {
        return donacionesRecibidas;
    }

    @Override
    public String getNombreDisplay() {
        return razonSocial;
    }

    public void recibirDonacion(Donacion donacion) {
        donacionesRecibidas.add(donacion);
        donacion.registrarEntrega();
    }

    public long getCantidadDonacionesUltimoTrimestre() {
        LocalDate haceTresMeses = LocalDate.now().minusMonths(3);

        return donacionesRecibidas.stream()
            .filter(d -> d.getFechaEntrega() != null)
            .filter(d -> !d.getFechaEntrega().isBefore(haceTresMeses))
            .count();
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }
}
