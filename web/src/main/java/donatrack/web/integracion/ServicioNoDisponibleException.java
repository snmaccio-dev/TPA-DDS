package donatrack.web.integracion;

public class ServicioNoDisponibleException extends IntegracionException {

  public ServicioNoDisponibleException(String servicio, String detalle) {
    super("El servicio de " + servicio + " " + detalle + ".");
  }

  public ServicioNoDisponibleException(String servicio, String detalle, Throwable causa) {
    super("El servicio de " + servicio + " " + detalle + ".", causa);
  }
}
