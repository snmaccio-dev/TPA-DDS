package donatrack.model.donacion;

import donatrack.model.catalogo.Subcategoria;
import donatrack.model.donacion.estado.EnDeposito;
import donatrack.model.donacion.estado.Entregada;
import donatrack.model.donacion.estado.EstadoDonacion;
import donatrack.model.logistica.Camion;
import donatrack.model.logistica.Comprobante;
import donatrack.model.persona.Beneficiaria;
import donatrack.model.persona.Persona;
import donatrack.notificacion.DonacionObserver;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Donacion {

    private final long id;
    private static long proximoId = 1;
    private List<Bien> bienes;
    private EstadoDonacion estado;
    private String descripcion;
    private Persona donante;
    private Beneficiaria destinatarioAsignado;
    private Camion camion;
    private LocalDateTime fechaHoraEntrega;
    private List<String> fotos = new ArrayList<>();
    private List<CambioEstado> historialEstados = new ArrayList<>();

    // Observer — lista de observadores del ciclo de vida
    private final List<DonacionObserver> observers = new ArrayList<>();

    public Donacion(List<Bien> bienes,
                    Persona donante,
                    String descripcion) {
        if (bienes == null || bienes.isEmpty()) {
            throw new IllegalArgumentException("La donacion debe contener al menos un bien.");
        }
        if (donante == null) {
            throw new IllegalArgumentException("La donacion debe tener un donante.");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La donacion debe tener una descripcion.");
        }
        this.id = proximoId++;
        this.bienes = new ArrayList<>(bienes);
        this.donante = donante;
        this.descripcion = descripcion;
        this.estado = new EnDeposito();
    }

    // === Transiciones ===

    public void asignar() {
        estado.asignar(this);
    }

    public void marcarListaParaEntregar() {
        estado.marcarListaParaEntregar(this);
    }

    public void marcarEnTraslado() {
        estado.marcarEnTraslado(this);
    }

    public void confirmarRecepcion(List<String> fotos) {
        estado.confirmarRecepcion(this, fotos);
    }

    public void marcarEntregaFallida(String motivo) {
        estado.marcarEntregaFallida(this, motivo);
    }

    public void marcarEnDeposito() {
        estado.marcarEnDeposito(this);
    }

    public void vencer() {
        estado.vencer(this);
    }

    // Uso interno del State al confirmar recepción — no invocar directamente.
    public void registrarRecepcion(List<String> fotos) {
        this.fechaHoraEntrega = LocalDateTime.now();
        this.fotos = new ArrayList<>(fotos);
    }

    public Comprobante generarComprobante() {
        if (!(estado instanceof Entregada)) {
            throw new IllegalStateException("Solo se genera comprobante de una donacion entregada.");
        }
        return new Comprobante(
            id,
            fechaHoraEntrega,
            camion.getPatente(),
            destinatarioAsignado.getPersona().getRazonSocial(),
            donante.getNombreDisplay(),
            descripcion
        );
    }

    // === Factory ===

    public static Donacion crear(List<Bien> bienes,
                                 Persona donante,
                                 String descripcion) {
        return new Donacion(bienes, donante, descripcion);
    }

    // === Getters / Setters de dominio ===

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Persona getDonante() {
        return donante;
    }

    public void setDonante(Persona donante) {
        this.donante = donante;
    }

    public Beneficiaria getDestinatarioAsignado() {
        return destinatarioAsignado;
    }

    public void asignarDestinatario(Beneficiaria destinatario) {
        this.destinatarioAsignado = destinatario;
    }

    public void limpiarDestinatario() {
        this.destinatarioAsignado = null;
    }

    public Camion getCamion() {
        return camion;
    }

    public void asignarCamion(Camion camion) {
        this.camion = camion;
    }

    public void limpiarCamion() {
        this.camion = null;
    }

    public LocalDateTime getFechaHoraEntrega() {
        return fechaHoraEntrega;
    }

    public List<String> getFotos() {
        return fotos;
    }

    public Subcategoria getSubcategoria() {
        return bienes.get(0).getSubcategoria();
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

    // === Estado ===

    public EstadoDonacion getEstado() {
        return estado;
    }

    public void cambiarEstado(EstadoDonacion nuevoEstado) {
        cambiarEstado(nuevoEstado, null);
    }

    public void cambiarEstado(EstadoDonacion nuevoEstado, String motivo) {
        String anterior = this.estado.getNombre();
        String nuevo = nuevoEstado.getNombre();

        this.estado = nuevoEstado;
        historialEstados.add(new CambioEstado(anterior, nuevo, motivo));
        notificarObservers(anterior, nuevo);
    }

    public List<CambioEstado> getHistorialEstados() {
        return historialEstados;
    }

    @Override
    public String toString() {
        return "Donacion[subcategoria=" + getSubcategoria()
            + ", estado=" + estado.getNombre()
            + ", bienes=" + bienes.size() + "]";
    }

    public Long getId() {
        return id;
    }
}
