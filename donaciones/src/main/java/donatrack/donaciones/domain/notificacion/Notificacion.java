package donatrack.donaciones.domain.notificacion;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import java.time.LocalDateTime;

// Notificacion almacenada. Apunta al perfil (donante o beneficiaria), no a
// la persona: una persona que es donante y beneficiaria tiene listas
// separadas. Siempre hay exactamente uno cargado entre donante, beneficiaria
// y paraAdministradores, y el constructor privado lo valida.
@Entity
@Table(name = "notificacion")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notificacion_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "donante_id")
    private Donante donante;

    @ManyToOne
    @JoinColumn(name = "beneficiaria_id")
    private Beneficiaria beneficiaria;

    @Column(name = "para_administradores", nullable = false)
    private boolean paraAdministradores;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoNotificacion tipo;

    @Column(name = "mensaje", length = 1000, nullable = false)
    private String mensaje;

    @ManyToOne
    @JoinColumn(name = "donacion_id")
    private Donacion donacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal")
    private TipoContacto canal;

    @Column(name = "destino")
    private String destino;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_envio", nullable = false)
    private EstadoEnvio estadoEnvio;

    @Column(name = "detalle_error", length = 1000)
    private String detalleError;

    @Column(name = "fecha_hora_creacion", nullable = false)
    private LocalDateTime fechaHoraCreacion;

    @Column(name = "fecha_hora_lectura")
    private LocalDateTime fechaHoraLectura;

    protected Notificacion() {
    }

    private Notificacion(Donante donante,
                         Beneficiaria beneficiaria,
                         boolean paraAdministradores,
                         TipoNotificacion tipo,
                         String mensaje,
                         Donacion donacion) {
        if (tipo == null) {
            throw new DatosInvalidosException("El tipo de notificacion es obligatorio.");
        }
        if (mensaje == null || mensaje.isBlank()) {
            throw new DatosInvalidosException("El mensaje de la notificacion es obligatorio.");
        }
        int cargados = (donante != null ? 1 : 0)
            + (beneficiaria != null ? 1 : 0)
            + (paraAdministradores ? 1 : 0);
        if (cargados != 1) {
            throw new DatosInvalidosException(
                "La notificacion tiene que apuntar a un unico destinatario (donante, beneficiaria o administradores)."
            );
        }
        this.donante = donante;
        this.beneficiaria = beneficiaria;
        this.paraAdministradores = paraAdministradores;
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.donacion = donacion;
        this.fechaHoraCreacion = LocalDateTime.now();
        this.estadoEnvio = EstadoEnvio.NO_APLICA;
    }

    public static Notificacion paraDonante(Donante donante,
                                           TipoNotificacion tipo,
                                           String mensaje,
                                           Donacion donacion) {
        if (donante == null) {
            throw new DatosInvalidosException("El donante destinatario es obligatorio.");
        }
        return new Notificacion(donante, null, false, tipo, mensaje, donacion);
    }

    public static Notificacion paraBeneficiaria(Beneficiaria beneficiaria,
                                                TipoNotificacion tipo,
                                                String mensaje,
                                                Donacion donacion) {
        if (beneficiaria == null) {
            throw new DatosInvalidosException("La beneficiaria destinataria es obligatoria.");
        }
        return new Notificacion(null, beneficiaria, false, tipo, mensaje, donacion);
    }

    public static Notificacion paraAdministradores(TipoNotificacion tipo,
                                                   String mensaje,
                                                   Donacion donacion) {
        return new Notificacion(null, null, true, tipo, mensaje, donacion);
    }

    public void marcarEnviada(TipoContacto canal, String destino) {
        this.estadoEnvio = EstadoEnvio.ENVIADA;
        this.canal = canal;
        this.destino = destino;
        this.detalleError = null;
    }

    public void marcarFallida(String detalle) {
        this.estadoEnvio = EstadoEnvio.FALLIDA;
        this.detalleError = detalle;
    }

    // Idempotente: si ya estaba leida no modifica la fecha, para no "revivir"
    // una notificacion ya resuelta ni hacer updates innecesarios.
    public void marcarLeida() {
        if (fechaHoraLectura == null) {
            fechaHoraLectura = LocalDateTime.now();
        }
    }

    public boolean estaLeida() {
        return fechaHoraLectura != null;
    }

    public Long getId() {
        return id;
    }

    public Donante getDonante() {
        return donante;
    }

    public Beneficiaria getBeneficiaria() {
        return beneficiaria;
    }

    public boolean esParaAdministradores() {
        return paraAdministradores;
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public Donacion getDonacion() {
        return donacion;
    }

    public TipoContacto getCanal() {
        return canal;
    }

    public String getDestino() {
        return destino;
    }

    public EstadoEnvio getEstadoEnvio() {
        return estadoEnvio;
    }

    public String getDetalleError() {
        return detalleError;
    }

    public LocalDateTime getFechaHoraCreacion() {
        return fechaHoraCreacion;
    }

    public LocalDateTime getFechaHoraLectura() {
        return fechaHoraLectura;
    }
}
