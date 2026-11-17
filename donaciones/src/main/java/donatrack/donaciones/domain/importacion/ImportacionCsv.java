package donatrack.donaciones.domain.importacion;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "importacion_csv")
public class ImportacionCsv {

  public static final int MAXIMO_ERRORES_GUARDADOS = 1000;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "importacion_id")
  private Long id;

  @Column(name = "nombre_archivo")
  private String nombreArchivo;

  @Column(name = "fecha_inicio")
  private LocalDateTime fechaInicio;

  @Column(name = "fecha_fin")
  private LocalDateTime fechaFin;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado")
  private EstadoImportacion estado;

  @Column(name = "motivo_fallo", length = ErrorDeImportacion.LARGO_MAXIMO_MENSAJE)
  private String motivoFallo;

  @Column(name = "filas_leidas")
  private int filasLeidas;

  @Column(name = "creados")
  private int creados;

  @Column(name = "actualizados")
  private int actualizados;

  @Column(name = "cuentas_creadas")
  private int cuentasCreadas;

  @Column(name = "cuentas_vinculadas")
  private int cuentasVinculadas;

  @Column(name = "filas_con_error")
  private int filasConError;

  @ElementCollection
  @CollectionTable(
      name = "importacion_csv_error",
      joinColumns = @JoinColumn(name = "importacion_id")
  )
  @OrderColumn(name = "orden")
  private List<ErrorDeImportacion> errores = new ArrayList<>();

  protected ImportacionCsv() {
  }

  public ImportacionCsv(String nombreArchivo) {
    this.nombreArchivo = nombreArchivo;
    this.fechaInicio = LocalDateTime.now();
    this.estado = EstadoImportacion.EN_CURSO;
  }

  public void registrarAvance(int filasLeidas,
                              int creados,
                              int actualizados,
                              int cuentasCreadas,
                              int cuentasVinculadas,
                              List<ErrorDeImportacion> nuevosErrores) {
    this.filasLeidas += filasLeidas;
    this.creados += creados;
    this.actualizados += actualizados;
    this.cuentasCreadas += cuentasCreadas;
    this.cuentasVinculadas += cuentasVinculadas;
    this.filasConError += nuevosErrores.size();
    for (ErrorDeImportacion error : nuevosErrores) {
      if (errores.size() >= MAXIMO_ERRORES_GUARDADOS) {
        break;
      }
      errores.add(error);
    }
  }

  public void finalizar() {
    this.estado = EstadoImportacion.FINALIZADA;
    this.fechaFin = LocalDateTime.now();
  }

  public void fallar(String motivo) {
    this.estado = EstadoImportacion.FALLIDA;
    this.fechaFin = LocalDateTime.now();
    String texto = motivo == null || motivo.isBlank() ? "Error sin detalle." : motivo;
    this.motivoFallo = texto.length() > ErrorDeImportacion.LARGO_MAXIMO_MENSAJE
        ? texto.substring(0, ErrorDeImportacion.LARGO_MAXIMO_MENSAJE)
        : texto;
  }

  public int getErroresNoGuardados() {
    return filasConError - errores.size();
  }

  public Long getId() {
    return id;
  }

  public String getNombreArchivo() {
    return nombreArchivo;
  }

  public LocalDateTime getFechaInicio() {
    return fechaInicio;
  }

  public LocalDateTime getFechaFin() {
    return fechaFin;
  }

  public EstadoImportacion getEstado() {
    return estado;
  }

  public String getMotivoFallo() {
    return motivoFallo;
  }

  public int getFilasLeidas() {
    return filasLeidas;
  }

  public int getCreados() {
    return creados;
  }

  public int getActualizados() {
    return actualizados;
  }

  public int getCuentasCreadas() {
    return cuentasCreadas;
  }

  public int getCuentasVinculadas() {
    return cuentasVinculadas;
  }

  public int getFilasConError() {
    return filasConError;
  }

  public List<ErrorDeImportacion> getErrores() {
    return List.copyOf(errores);
  }
}
