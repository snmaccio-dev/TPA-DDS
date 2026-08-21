package donatrack.model.persona;

import donatrack.model.donacion.Donacion;
import donatrack.model.necesidad.Necesidad;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Beneficiaria extends Rol {

    private static long proximoId = 1;

    private final long id;
    private final List<Necesidad> necesidades = new ArrayList<>();
    private final List<Donacion> donacionesRecibidas = new ArrayList<>();

    public Beneficiaria(PersonaJuridica persona) {
        super(persona);
        this.id = proximoId++;
    }

    @Override
    public PersonaJuridica getPersona() {
        return (PersonaJuridica) super.getPersona();
    }

    public long getId() {
        return id;
    }

    public void registrarNecesidad(Necesidad necesidad) {
        necesidades.add(necesidad);
    }

    public void registrarDonacionRecibida(Donacion donacion) {
        donacionesRecibidas.add(donacion);
    }

    public long getCantidadDonacionesUltimoTrimestre() {
        LocalDate limite = LocalDate.now().minusMonths(3);
        return donacionesRecibidas.stream()
            .filter(donacion -> donacion.getFechaHoraEntrega() != null)
            .filter(donacion -> !donacion.getFechaHoraEntrega().toLocalDate().isBefore(limite))
            .count();
    }

    public List<Necesidad> getNecesidades() {
        return necesidades;
    }

    public List<Donacion> getDonacionesRecibidas() {
        return donacionesRecibidas;
    }
}
