package donatrack.donaciones.domain.donacion;

import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.catalogo.Unidades;
import donatrack.donaciones.domain.donacion.estado.EstadoDonacion;
import donatrack.donaciones.domain.donacion.estado.EstadoEntregaFallida;
import donatrack.donaciones.domain.donacion.estado.EstadosPosiblesDonacion;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.notificacion.DonacionObserver;
import javax.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Donacion")
public class Donacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "donacion_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "subcategoria_id")
    private Subcategoria subcategoria;

    // Una donación tiene muchos bienes
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "donacion_id") 
    private List<Bien> bienes = new ArrayList<>();
    
    // Relación con el estado actual
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "estado_actual_id")
    private EstadoDonacion estado;

    @Column(name = "descripcion")
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "donante_id")
    private Donante donante;

    @ManyToOne
    @JoinColumn(name = "destinatario_id")
    private Beneficiaria destinatarioAsignado;

    // Asumiendo que DatosEntrega es un @Embeddable (atributos que se suman a esta misma tabla)
    @Embedded 
    private DatosEntrega datosEntrega;

    // Historial de estados que tuvo la donación
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "donacion_id")
    private List<EstadoDonacion> historialEstados = new ArrayList<>();

    // Observer — lista de observadores del ciclo de vida
    // @Transient le dice a JPA que ignore esto, que no intente guardarlo en PostgreSQL
    @Transient
    private final List<DonacionObserver> observers = new ArrayList<>();

    protected Donacion() {
    }
    
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
        this.bienes = new ArrayList<>(bienes);
        this.subcategoria = this.bienes.get(0).getSubcategoria();
        this.donante = donante;
        this.descripcion = descripcion;
        this.estado = new EstadoDonacion(EstadosPosiblesDonacion.EN_DEPOSITO);
        this.historialEstados.add(this.estado);
    }

    // === Transiciones ===

    public void confirmarDestino(Beneficiaria destinatario) {
        exigirEstado("confirmar el destino", EstadosPosiblesDonacion.EN_DEPOSITO);
        if (destinatario == null) {
            throw new IllegalArgumentException(
                "Debe especificarse un destinatario para confirmar el destino."
            );
        }
        this.destinatarioAsignado = destinatario;
        cambiarEstado(new EstadoDonacion(EstadosPosiblesDonacion.ASIGNACION_REALIZADA));
    }

    public void marcarListaParaEntregar() {
        exigirEstado("marcar lista para entregar", EstadosPosiblesDonacion.ASIGNACION_REALIZADA);
        cambiarEstado(new EstadoDonacion(EstadosPosiblesDonacion.LISTA_PARA_ENTREGAR));
    }

    public void marcarEnTraslado() {
        exigirEstado("marcar en traslado", EstadosPosiblesDonacion.LISTA_PARA_ENTREGAR);
        cambiarEstado(new EstadoDonacion(EstadosPosiblesDonacion.EN_TRASLADO));
    }

    public void marcarEntregada(LocalDateTime fechaHora, String patenteCamion) {
        exigirEstado("marcar entregada", EstadosPosiblesDonacion.EN_TRASLADO);
        this.datosEntrega = new DatosEntrega(fechaHora, patenteCamion);
        cambiarEstado(new EstadoDonacion(EstadosPosiblesDonacion.ENTREGADA));
        destinatarioAsignado.registrarDonacionRecibida(this);
    }

    public void marcarEntregaFallida(String motivo) {
        exigirEstado("marcar entrega fallida", EstadosPosiblesDonacion.EN_TRASLADO);
        cambiarEstado(new EstadoEntregaFallida(motivo));
    }

    public void marcarEnDeposito() {
        exigirEstado("marcar en deposito", EstadosPosiblesDonacion.ENTREGA_FALLIDA);
        this.destinatarioAsignado = null;
        this.datosEntrega = null;
        cambiarEstado(new EstadoDonacion(EstadosPosiblesDonacion.EN_DEPOSITO));
    }

    public void marcarVencida() {
        EstadosPosiblesDonacion actual = estado.getEstado();
        if (actual == EstadosPosiblesDonacion.ENTREGADA || actual == EstadosPosiblesDonacion.VENCIDA) {
            throw new IllegalStateException(
                "No se puede vencer desde el estado " + estado.getNombre() + "."
            );
        }
        cambiarEstado(new EstadoDonacion(EstadosPosiblesDonacion.VENCIDA));
    }

    private void exigirEstado(String accion, EstadosPosiblesDonacion... permitidos) {
        for (EstadosPosiblesDonacion permitido : permitidos) {
            if (estado.getEstado() == permitido) return;
        }
        throw new IllegalStateException(
            "No se puede " + accion + " desde el estado " + estado.getNombre() + "."
        );
    }

    public Comprobante generarComprobante() {
        if (estado.getEstado() != EstadosPosiblesDonacion.ENTREGADA) {
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

    public LocalDateTime getFechaHoraEntrega() {
        return datosEntrega == null ? null : datosEntrega.getFechaHora();
    }

    public void asignarDatosEntrega(DatosEntrega datosEntrega) {
        this.datosEntrega = datosEntrega;
    }

    public Subcategoria getSubcategoria() {
        return subcategoria;
    }

    public List<Bien> getBienes() {
        return List.copyOf(bienes);
    }

    public double getCantidadTotal() {
        return bienes.stream().mapToDouble(Bien::getCantidad).sum();
    }

    public Unidades getUnidades() {
        return getSubcategoria().getUnidades();
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
        String anterior = this.estado.getNombre();
        String nuevo = nuevoEstado.getNombre();

        this.estado = nuevoEstado;
        historialEstados.add(nuevoEstado);
        notificarObservers(anterior, nuevo);
    }

    public List<EstadoDonacion> getHistorialEstados() {
        return List.copyOf(historialEstados);
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
