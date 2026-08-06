package donatrack.model.donacion;

import donatrack.model.catalogo.Subcategoria;
import donatrack.model.donacion.estado.EstadoDonacion;
import donatrack.model.donacion.estado.EnDeposito;
import donatrack.model.logistica.Camion;
import donatrack.notificacion.DonacionObserver;
import donatrack.model.donacion.CambioEstado;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Donacion {

    private List<Bien> bienes;
    private EstadoDonacion estado;
    private Subcategoria subcategoria;
    private List<CambioEstado> historialEstados = new ArrayList<>();
    private LocalDate fechaEntrega;
    private Camion camionEntrega;
    private List<String> fotos = new ArrayList<>();

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

    public static Donacion crear(Subcategoria subcategoria, List<Bien> bienes) {
        return new Donacion(bienes, subcategoria);
    }

    public Subcategoria getSubcategoria() {
      return subcategoria;
    }

    public void setSubcategoria(Subcategoria subcategoria) {
      this.subcategoria = subcategoria;
    }

    public List<Bien> getBienes() {
      return bienes;
    }

    // === Observer ===

    public void agregarObserver(DonacionObserver observer) {
        observers.add(observer);
    }

    public void removerObserver(DonacionObserver observer) {
        observers.remove(observer);
    }

    private void notificarObservers(String estadoAnterior, String estadoNuevo) {
        observers.forEach(o -> o.onCambioEstado(this, estadoAnterior, estadoNuevo));
    }

    // === Getters ===

    public EstadoDonacion getEstado() {
        return estado;
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

    public void registrarEntrega() {
        this.fechaEntrega = LocalDate.now();
    }

    public LocalDate getFechaEntrega() {
        return fechaEntrega;
    }

    public void registrarCamion(Camion camion) {
        this.camionEntrega = camion;
    }

    public void agregarFoto(String foto) {
        fotos.add(foto);
    }

    public Camion getCamionEntrega() {
        return camionEntrega;
    }

    public List<String> getFotos() {
        return fotos;
    }

}
