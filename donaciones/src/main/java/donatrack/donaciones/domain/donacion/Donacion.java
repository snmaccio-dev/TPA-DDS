package donatrack.donaciones.domain.donacion;

import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.TransicionNoPermitidaException;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    // Nuevo para que se agregue la imagen 

    @Column(name = "foto_url", length = 1000)
    private String fotoUrl;

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
        this(bienes, donante, descripcion, EstadosPosiblesDonacion.EN_DEPOSITO);
    }

    public Donacion(List<Bien> bienes,
                    Donante donante,
                    String descripcion,
                    EstadosPosiblesDonacion estadoInicial) {
        if (bienes == null || bienes.isEmpty()) {
            throw new DatosInvalidosException("La donacion debe contener al menos un bien.");
        }
        if (donante == null) {
            throw new DatosInvalidosException("La donacion debe tener un donante.");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new DatosInvalidosException("La donacion debe tener una descripcion.");
        }
        if (estadoInicial == null) {
            throw new DatosInvalidosException("La donacion debe tener un estado inicial.");
        }

        this.bienes = new ArrayList<>(bienes);
        this.subcategoria = this.bienes.get(0).getSubcategoria();
        this.donante = donante;
        this.descripcion = descripcion;
        this.estado = new EstadoDonacion(estadoInicial);
        this.historialEstados.add(this.estado);
    }

    // === Transiciones ===

    public void confirmarDestino(Beneficiaria destinatario) {
        exigirEstado("confirmar el destino", EstadosPosiblesDonacion.EN_DEPOSITO);
        if (destinatario == null) {
            throw new DatosInvalidosException(
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

    public void confirmarRecepcionEnDeposito() {
        exigirEstado(
            "confirmar la recepcion en deposito",
            EstadosPosiblesDonacion.PENDIENTE_RECEPCION
        );

        cambiarEstado(new EstadoDonacion(EstadosPosiblesDonacion.EN_DEPOSITO));
    }

    public void marcarVencida() {
        EstadosPosiblesDonacion actual = estado.getEstado();
        if (actual == EstadosPosiblesDonacion.ENTREGADA || actual == EstadosPosiblesDonacion.VENCIDA) {
            throw new TransicionNoPermitidaException(
                "No se puede vencer desde el estado " + estado.getNombre() + "."
            );
        }
        cambiarEstado(new EstadoDonacion(EstadosPosiblesDonacion.VENCIDA));
    }

    // Nuevo metodo 

    public void deshacerVencimiento() {
        if (estado.getEstado() != EstadosPosiblesDonacion.VENCIDA) {
            throw new TransicionNoPermitidaException(
                "Solo se puede deshacer el vencimiento de una donación vencida."
            );
        }

        if (historialEstados.size() < 2 ||
            historialEstados.get(historialEstados.size() - 2).getEstado()
                != EstadosPosiblesDonacion.EN_DEPOSITO) {
            throw new TransicionNoPermitidaException(
                "La donación no estaba en depósito antes de vencer."
            );
        }

        cambiarEstado(new EstadoDonacion(EstadosPosiblesDonacion.EN_DEPOSITO));
    }

    private void exigirEstado(String accion, EstadosPosiblesDonacion... permitidos) {
        for (EstadosPosiblesDonacion permitido : permitidos) {
            if (estado.getEstado() == permitido) return;
        }
        throw new TransicionNoPermitidaException(
            "No se puede " + accion + " desde el estado " + estado.getNombre() + "."
        );
    }

    public Comprobante generarComprobante() {
        if (estado.getEstado() != EstadosPosiblesDonacion.ENTREGADA) {
            throw new TransicionNoPermitidaException("Solo se genera comprobante de una donacion entregada.");
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

    // === Modificacion ===

    // Solo se editan la descripcion, la foto y la descripcion y cantidad de cada bien.
    // La subcategoria, la condicion y la fecha de vencimiento no se pueden cambiar porque
    // definen la segmentacion de la donacion: si hay que cambiarlas, se borra la donacion
    // y se vuelve a cargar. Tampoco se agregan ni se quitan bienes.
    public void modificar(String descripcion, String fotoUrl, List<DatosModificacionBien> bienes) {
        if (estado.getEstado() != EstadosPosiblesDonacion.EN_DEPOSITO) {
            throw new ConflictoDeNegocioException("Solo se puede modificar una donacion en deposito.");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new DatosInvalidosException("La donacion debe tener una descripcion.");
        }
        Map<Bien, DatosModificacionBien> cambios = emparejarBienes(bienes);
        cambios.values().forEach(d -> Bien.validarModificacion(d.descripcion(), d.cantidad()));

        this.descripcion = descripcion.trim();
        this.fotoUrl = fotoUrl == null || fotoUrl.isBlank() ? null : fotoUrl.trim();
        cambios.forEach((bien, datos) -> bien.modificar(datos.descripcion(), datos.cantidad()));
    }

    private Map<Bien, DatosModificacionBien> emparejarBienes(List<DatosModificacionBien> datos) {
        Map<Long, Bien> actuales = new LinkedHashMap<>();
        this.bienes.forEach(b -> actuales.put(b.getId(), b));

        Map<Bien, DatosModificacionBien> emparejados = new LinkedHashMap<>();
        for (DatosModificacionBien d : datos == null ? List.<DatosModificacionBien>of() : datos) {
            if (d == null || d.id() == null) {
                throw new DatosInvalidosException("Cada bien tiene que indicar su id.");
            }
            Bien bien = actuales.get(d.id());
            if (bien == null) {
                throw new DatosInvalidosException(
                    "El bien con id " + d.id() + " no pertenece a la donacion.");
            }
            if (emparejados.put(bien, d) != null) {
                throw new DatosInvalidosException("El bien con id " + d.id() + " esta repetido.");
            }
        }
        for (Map.Entry<Long, Bien> actual : actuales.entrySet()) {
            if (!emparejados.containsKey(actual.getValue())) {
                throw new DatosInvalidosException(
                    "Falta el bien con id " + actual.getKey()
                        + ". Hay que enviar todos los bienes de la donacion.");
            }
        }
        return emparejados;
    }

    // === Factory ===

    public static Donacion crear(List<Bien> bienes,
                                 Donante donante,
                                 String descripcion) {
        return new Donacion(bienes, donante, descripcion);
    }

    public static Donacion crear(List<Bien> bienes,
                                Donante donante,
                                String descripcion,
                                EstadosPosiblesDonacion estadoInicial) {
        return new Donacion(bienes, donante, descripcion, estadoInicial);
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

    // Idempotente por tipo: si ya hay un observer de la misma clase, no lo
    // vuelve a agregar. Los observers son @Transient, asi que se registran
    // cada vez que la Donacion se carga de la base; con el EM pudiendo
    // devolver la misma instancia mas de una vez dentro del mismo request,
    // sin esta guarda terminariamos notificando duplicado.
    public void agregarObserver(DonacionObserver observer) {
        if (observer == null) {
            return;
        }
        for (DonacionObserver existente : observers) {
            if (existente.getClass().equals(observer.getClass())) {
                return;
            }
        }
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

    // Nuevos metodos 

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void asignarFoto(String fotoUrl) {
        if (fotoUrl == null || fotoUrl.isBlank()) {
            throw new DatosInvalidosException(
                "La URL de la fotografía no puede estar vacía."
            );
        }
        this.fotoUrl = fotoUrl;
    }
}
