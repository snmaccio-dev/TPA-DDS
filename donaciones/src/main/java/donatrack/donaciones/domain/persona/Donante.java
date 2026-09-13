package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.donacion.Donacion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Donante extends Rol {

    private LocalDate ultimaInteraccion;
    private LocalDate fechaUltimoAvisoInactividad;
    private final List<Donacion> donacionesRealizadas = new ArrayList<>();

    public Donante(Persona persona) {
        super(persona);
        this.ultimaInteraccion = LocalDate.now();
    }

    public void registrarDonacion(Donacion donacion) {
        donacionesRealizadas.add(donacion);
        registrarInteraccion();
    }

    // Por ahora la unica accion que cuenta como interaccion es registrar una donacion.
    public void registrarInteraccion() {
        this.ultimaInteraccion = LocalDate.now();
    }

    public void registrarAvisoDeInactividad() {
        this.fechaUltimoAvisoInactividad = LocalDate.now();
    }

    public boolean requiereAvisoDeInactividad(LocalDate limite) {
        return ultimaInteraccion.isBefore(limite)
            && (fechaUltimoAvisoInactividad == null
                || fechaUltimoAvisoInactividad.isBefore(ultimaInteraccion));
    }

    public LocalDate getUltimaInteraccion() {
        return ultimaInteraccion;
    }

    public LocalDate getFechaUltimoAvisoInactividad() {
        return fechaUltimoAvisoInactividad;
    }

    public List<Donacion> getDonacionesRealizadas() {
        return donacionesRealizadas;
    }
}