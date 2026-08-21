package donatrack.model.persona;

import donatrack.model.donacion.Donacion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Donante extends Rol {

    private LocalDate ultimaInteraccion;
    private final List<Donacion> donacionesRealizadas = new ArrayList<>();

    public Donante(Persona persona) {
        super(persona);
        this.ultimaInteraccion = LocalDate.now();
    }

    public void registrarDonacion(Donacion donacion) {
        donacionesRealizadas.add(donacion);
        registrarInteraccion();
    }

    public void registrarInteraccion() {
        this.ultimaInteraccion = LocalDate.now();
    }

    public LocalDate getUltimaInteraccion() {
        return ultimaInteraccion;
    }

    public List<Donacion> getDonacionesRealizadas() {
        return donacionesRealizadas;
    }
}
