package donatrack.donaciones.domain.donacion;

import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.donacion.estado.EstadoDonacion;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.notificacion.DonacionObserver;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Donacion {

    private final long id;
    private static long proximoId = 1;
    private List<Bien> bienes;
    private EstadoDonacion estado;
    private String descripcion;
    private Donante donante;
    private Beneficiaria destinatarioAsignado;
    private DatosEntrega datosEntrega;
    private List<String> fotos = new ArrayList<>();
    private List<CambioEstado> historialEstados = new ArrayList<>();

    // Observer — lista de observadores del ciclo de vida
    private final List<DonacionObserver> observers = new ArrayList<>();

    public Donacion(List<Bien> bienes,
                    Donante donante,
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
        this.estado = EstadoDonacion.EN_DEPOSITO;
    }

    // === Transiciones ===

    public void confirmarDestino(Beneficiaria destinatario) {
        exigirEstado("confirmar el destino", EstadoDonacion.EN_DEPOSITO);
        if (destinatario == null) {
            throw new IllegalArgumentException(
                "Debe especificarse un destinatario para confirmar el destino."
            );
        }
        this.destinatarioAsignado = destinatario;
        cambiarEstado(EstadoDonacion.ASIGNACION_REALIZADA);
    }

    public void marcarListaParaEntregar() {
        exigirEstado("marcar lista para entregar", EstadoDonacion.ASIGNACION_REALIZADA);
        cambiarEstado(EstadoDonacion.LISTA_PARA_ENTREGAR);
    }

    public void marcarEnTraslado() {
        exigirEstado("marcar en traslado", EstadoDonacion.LISTA_PARA_ENTREGAR);
        cambiarEstado(EstadoDonacion.EN_TRASLADO);
    }

    public void confirmarRecepcion(List<String> fotos) {
        exigirEstado("confirmar la recepcion", EstadoDonacion.EN_TRASLADO);
        if (datosEntrega == null) {
            datosEntrega = new DatosEntrega(LocalDateTime.now(), null);
        } else {
            datosEntrega.setFechaHora(LocalDateTime.now());
        }
        this.fotos = new ArrayList<>(fotos);
        cambiarEstado(EstadoDonacion.ENTREGADA);
        destinatarioAsignado.registrarDonacionRecibida(this);
    }

    public void marcarEntregaFallida(String motivo) {
        exigirEstado("marcar entrega fallida", EstadoDonacion.EN_TRASLADO);
        cambiarEstado(EstadoDonacion.ENTREGA_FALLIDA, motivo);
    }

    public void marcarEnDeposito() {
        exigirEstado("marcar en deposito", EstadoDonacion.ENTREGA_FALLIDA);
        this.destinatarioAsignado = null;
        this.datosEntrega = null;
        cambiarEstado(EstadoDonacion.EN_DEPOSITO);
    }

    public void vencer() {
        if (estado == EstadoDonacion.ENTREGADA || estado == EstadoDonacion.VENCIDA) {
            throw new IllegalStateException(
                "No se puede vencer desde el estado " + estado.getNombre() + "."
            );
        }
        cambiarEstado(EstadoDonacion.VENCIDA);
    }

    private void exigirEstado(String accion, EstadoDonacion... permitidos) {
        for (EstadoDonacion permitido : permitidos) {
            if (estado == permitido) return;
        }
        throw new IllegalStateException(
            "No se puede " + accion + " desde el estado " + estado.getNombre() + "."
        );
    }

    public Comprobante generarComprobante() {
        if (estado != EstadoDonacion.ENTREGADA) {
            throw new IllegalStateException("Solo se genera comprobante de una donacion entregada.");
        }
        return new Comprobante(
            id,
            datosEntrega.getFechaHora(),
            datosEntrega.getPatenteCamion(),
            destinatarioAsignado.getPersona().getRazonSocial(),
            donante.getPersona().getNombreDisplay(),
            descripcion
        );
    }

    // === Factory ===

    public static Donacion crear(List<Bien> bienes,
                                 Donante donante,
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

    public Donante getDonante() {
        return donante;
    }

    public void setDonante(Donante donante) {
        this.donante = donante;
    }

    public Beneficiaria getDestinatarioAsignado() {
        return destinatarioAsignado;
    }

    public DatosEntrega getDatosEntrega() {
        return datosEntrega;
    }

    public void asignarDatosEntrega(DatosEntrega datosEntrega) {
        this.datosEntrega = datosEntrega;
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

    private void cambiarEstado(EstadoDonacion nuevoEstado) {
        cambiarEstado(nuevoEstado, null);
    }

    private void cambiarEstado(EstadoDonacion nuevoEstado, String motivo) {
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
