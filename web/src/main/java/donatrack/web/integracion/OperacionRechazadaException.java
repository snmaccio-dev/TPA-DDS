package donatrack.web.integracion;

public class OperacionRechazadaException extends IntegracionException {

  // Status HTTP recibido del servicio (ej. 403, 404, 422). Un valor <= 0 indica que la
  // excepcion se construyo desde la web (validacion local) sin un codigo asociado.
  private final int statusHttp;

  public OperacionRechazadaException(String mensaje) {
    this(mensaje, 0);
  }

  public OperacionRechazadaException(String mensaje, int statusHttp) {
    super(mensaje);
    this.statusHttp = statusHttp;
  }

  public int statusHttp() {
    return statusHttp;
  }

  public boolean esProhibido() {
    return statusHttp == 403;
  }
}
