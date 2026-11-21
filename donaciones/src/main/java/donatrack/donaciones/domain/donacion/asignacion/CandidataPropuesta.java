package donatrack.donaciones.domain.donacion.asignacion;

import donatrack.donaciones.domain.persona.Beneficiaria;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

// Una fila del resultado de un algoritmo: la entidad, el algoritmo que la propuso (o
// AMBOS) y su posicion en ese ranking (1 = primera). Es un valor de la propuesta, sin
// identidad propia: vive y muere con ella.
@Embeddable
public class CandidataPropuesta {

  @ManyToOne
  @JoinColumn(name = "beneficiaria_id")
  private Beneficiaria beneficiaria;

  @Enumerated(EnumType.STRING)
  @Column(name = "algoritmo")
  private OrigenCandidata origen;

  // Integer (y no int) para que hbm2ddl=update pueda agregar la columna a una tabla
  // que ya tiene filas de la version anterior.
  @Column(name = "posicion")
  private Integer posicion;

  protected CandidataPropuesta() {
  }

  public CandidataPropuesta(Beneficiaria beneficiaria, OrigenCandidata origen, int posicion) {
    this.beneficiaria = beneficiaria;
    this.origen = origen;
    this.posicion = posicion;
  }

  public Beneficiaria getBeneficiaria() {
    return beneficiaria;
  }

  public OrigenCandidata getOrigen() {
    return origen;
  }

  public Integer getPosicion() {
    return posicion;
  }

  public boolean esDe(Beneficiaria otra) {
    return mismaBeneficiaria(beneficiaria, otra);
  }

  static boolean mismaBeneficiaria(Beneficiaria una, Beneficiaria otra) {
    if (una == otra) {
      return true;
    }
    return una != null && otra != null
        && una.getId() != null && una.getId().equals(otra.getId());
  }
}
