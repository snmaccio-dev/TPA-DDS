package donatrack.model.donacion;

import donatrack.model.catalogo.Subcategoria;
import donatrack.model.donacion.estado.EstadoDonacion;
import donatrack.model.donacion.estado.EnDeposito;
import donatrack.notificacion.DonacionObserver;
import donatrack.model.donacion.CambioEstado;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

public class Donacion {

    private List<Bien> bienes;
    private EstadoDonacion estado;
    private Subcategoria subcategoria;
    private List<CambioEstado> historialEstados = new ArrayList<>();

    // Observer — lista de observadores del ciclo de vida
    private final List<DonacionObserver> observers = new ArrayList<>();

    public Donacion(List<Bien> bienes, Subcategoria subcategoria) {
        this.bienes = new ArrayList<>(bienes);
        this.subcategoria = subcategoria;
        this.estado = new EnDeposito();
    }

    // === Delegación al State ===

    public void asignar() {
        estado.asignar(this);
    }

    public void planificarRuta() {
        estado.planificarRuta(this);
    }

    public void iniciarTraslado() {
        estado.iniciarTraslado(this);
    }

    public void confirmarEntrega() {
        estado.confirmarEntrega(this);
    }

    public void fallarEntrega(String justificacion) {
        estado.fallarEntrega(this, justificacion);
    }

    public void vencer() {
        estado.vencer(this);
    }

    // Llamado por los estados concretos para cambiar el estado y notificar
    public void cambiarEstado(EstadoDonacion nuevoEstado) {
        String anterior = this.estado.getNombre();
        this.estado = nuevoEstado;
        notificarObservers(anterior, nuevoEstado.getNombre());
    }

    // === Observer ===

    public void agregarObserver(DonacionObserver observer) {
        observers.add(observer);
    }

    public void removerObserver(DonacionObserver observer) {
        observers.remove(observer);
    }

    private void notificarObservers(String estadoAnterior, String estadoNuevo) {
        for (DonacionObserver o : observers) {
            o.onCambioEstado(this, estadoAnterior, estadoNuevo);
        }
    }

    // === Getters ===

    public List<Bien> getBienes() {
        return bienes;
    }

    public EstadoDonacion getEstado() {
        return estado;
    }

    public Subcategoria getSubcategoria() {
        return subcategoria;
    }

    @Override
    public String toString() {
        return "Donacion[subcategoria=" + subcategoria + ", estado=" + estado.getNombre()
                + ", bienes=" + bienes.size() + "]";
    }

    public void cambiarEstado(EstadoDonacion nuevoEstado) {

      String anterior = this.estado.getNombre();
      String nuevo = nuevoEstado.getNombre();

      this.estado = nuevoEstado;

      historialEstados.add(new CambioEstado(anterior, nuevo));

      notificarObservers(anterior, nuevo);
    }

    public List<CambioEstado> getHistorialEstados() {
      return historialEstados;
    }
}
