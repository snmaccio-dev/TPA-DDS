package donatrack.donaciones.domain.importacion;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class ErrorDeImportacion {

  public static final int LARGO_MAXIMO_MENSAJE = 1000;

  @Column(name = "numero_fila")
  private int numeroFila;

  @Column(name = "mensaje", length = LARGO_MAXIMO_MENSAJE)
  private String mensaje;

  protected ErrorDeImportacion() {
  }

  public ErrorDeImportacion(int numeroFila, String mensaje) {
    this.numeroFila = numeroFila;
    String texto = mensaje == null || mensaje.isBlank() ? "Error sin detalle." : mensaje;
    this.mensaje = texto.length() > LARGO_MAXIMO_MENSAJE
        ? texto.substring(0, LARGO_MAXIMO_MENSAJE)
        : texto;
  }

  public int getNumeroFila() {
    return numeroFila;
  }

  public String getMensaje() {
    return mensaje;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ErrorDeImportacion otro)) {
      return false;
    }
    return numeroFila == otro.numeroFila && Objects.equals(mensaje, otro.mensaje);
  }

  @Override
  public int hashCode() {
    return Objects.hash(numeroFila, mensaje);
  }
}
