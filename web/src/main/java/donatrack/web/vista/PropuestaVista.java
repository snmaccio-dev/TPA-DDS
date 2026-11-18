package donatrack.web.vista;

import java.util.List;

public record PropuestaVista(
    long donacionId,
    String fechaGeneracion,
    boolean huboCoincidencias,
    List<Candidata> candidatas,
    String descripcionDonacion,
    String donanteNombre
) {

  public record Candidata(long id, String razonSocial, String cuit) {
    public long getId() { return id; }
    public String getRazonSocial() { return razonSocial; }
    public String getCuit() { return cuit; }
  }

  public PropuestaVista {
    candidatas = candidatas == null ? List.of() : List.copyOf(candidatas);
  }

  public long getDonacionId() { return donacionId; }
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

  public PropuestaVista conDatos(
      String descripcion,
      String nombreDonante,
      List<Candidata> beneficiariasCompatibles
  ) {
    return new PropuestaVista(
        donacionId,
        fechaGeneracion,
        huboCoincidencias,
        beneficiariasCompatibles,
        descripcion,
        nombreDonante
    );
  }
}
