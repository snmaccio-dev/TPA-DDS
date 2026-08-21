package donatrack.model.entidad;

import donatrack.model.necesidad.Necesidad;
import donatrack.model.persona.Persona;
import donatrack.model.persona.PersonaHumana;
import donatrack.model.donacion.Donacion;
import donatrack.model.logistica.Entrega;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EntidadBeneficiaria extends Persona {

    private String razonSocial;
    private List<Necesidad> necesidades = new ArrayList<>();
    private List<PersonaHumana> representantes = new ArrayList<>();
    private List<Donacion> donacionesRecibidas = new ArrayList<>();

    public EntidadBeneficiaria(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public void registrarNecesidad(Necesidad necesidad) {
        necesidades.add(necesidad);
    }

    public void agregarRepresentante(PersonaHumana representante) {
        representantes.add(representante);
    }

    public List<PersonaHumana> getRepresentantes() {
        return representantes;
    }

    public void registrarDonacionRecibida(Donacion donacion) {
        donacionesRecibidas.add(donacion);
    }

    public long getCantidadDonacionesUltimoTrimestre() {
        LocalDate limite = LocalDate.now().minusMonths(3);

        return donacionesRecibidas.stream()
            .map(Donacion::getEntrega)
            .filter(entrega -> entrega != null && entrega.getFechaEntrega() != null)
            .filter(entrega -> !entrega.getFechaEntrega().isBefore(limite))
            .count();
    }

    public List<Necesidad> getNecesidades() {
        return necesidades;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public List<Donacion> getDonacionesRecibidas() {
        return donacionesRecibidas;
    }

    @Override
    public String getNombreDisplay() {
        return razonSocial;
    }
}
