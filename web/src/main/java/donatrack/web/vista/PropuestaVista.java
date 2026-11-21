package donatrack.web.vista;

import java.util.List;

// Propuesta tal como la devuelve Donaciones: rankings trae una sola lista ("Coinciden
// ambos algoritmos") si hubo coincidencias, o una por algoritmo si no. candidatas es la
// lista plana de todas las entidades propuestas.
public record PropuestaVista(
    long donacionId,
    String fechaGeneracion,
    boolean huboCoincidencias,
    List<Ranking> rankings,
    List<Candidata> candidatas,
    String descripcionDonacion,
    String donanteNombre
) {

  public record Ranking(String algoritmo, String nombre, List<Candidata> candidatas) {

    public Ranking {
      candidatas = candidatas == null ? List.of() : List.copyOf(candidatas);
    }

    public String getNombre() { return nombre; }
    public List<Candidata> getCandidatas() { return candidatas; }
  }

  public record Candidata(long id, String razonSocial, String cuit, Integer posicion) {
    public long getId() { return id; }
    public String getRazonSocial() { return razonSocial; }
    public String getCuit() { return cuit; }
    public Integer getPosicion() { return posicion; }
  }

  public PropuestaVista {
    rankings = rankings == null ? List.of() : List.copyOf(rankings);
    candidatas = candidatas == null ? List.of() : List.copyOf(candidatas);
  }

  public long getDonacionId() { return donacionId; }
  public List<Ranking> getRankings() { return rankings; }
  public List<Candidata> getCandidatas() { return candidatas; }
  public boolean isHuboCoincidencias() { return huboCoincidencias; }
  public boolean isTieneCandidatas() { return !candidatas.isEmpty(); }

  public String getDescripcionDonacion() {
    return descripcionDonacion == null
        ? "Donación #" + donacionId
        : descripcionDonacion;
  }

  public String getDonanteNombre() {
    return donanteNombre == null || donanteNombre.isBlank()
        ? "Donante no identificado"
        : donanteNombre;
  }

  public String getFechaGeneracionTexto() {
    return Formato.fechaHoraCorta(fechaGeneracion);
  }

  // Completa los datos de la donacion (Donaciones no los manda en la propuesta). Las
  // candidatas quedan como las propusieron los algoritmos.
  public PropuestaVista conDatos(String descripcion, String nombreDonante) {
    return new PropuestaVista(
        donacionId,
        fechaGeneracion,
        huboCoincidencias,
        rankings,
        candidatas,
        descripcion,
        nombreDonante
    );
  }
}
