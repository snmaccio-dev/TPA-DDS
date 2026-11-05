package donatrack.web;

import donatrack.web.integracion.IntegracionException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Modelo {

  private final Map<String, Object> datos = new HashMap<>();
  private final Map<String, String> activa = new LinkedHashMap<>();

  private Modelo(String titulo) {
    datos.put("titulo", titulo);
    datos.put("activa", activa);
  }

  public static Modelo de(String titulo) {
    return new Modelo(titulo);
  }

  public Modelo seccion(String seccion) {
    datos.put("seccion", seccion);
    return this;
  }

  public Modelo conMapa() {
    datos.put("usaMapa", true);
    return this;
  }

  public Modelo activa(String clave) {
    activa.put(clave, "active");
    return this;
  }

  public Modelo con(String clave, Object valor) {
    datos.put(clave, valor);
    return this;
  }

  public <T> Modelo conDatosDe(String clave, T valorPorDefecto, Supplier<T> consulta) {
    try {
      return con(clave, consulta.get());
    } catch (IntegracionException e) {
      return con(clave, valorPorDefecto).con("errorDeServicio", e.getMessage());
    }
  }

  public Modelo comoDonante() {
    datos.put("esDonante", true);
    return usuario("Usuario de prueba", "US", "DONANTE",
        "Persona donante", "/donante-dashboard");
  }

  public Modelo comoBeneficiaria() {
    datos.put("esBeneficiaria", true);
    return usuario("Usuario de prueba", "US", "BENEFICIARIO",
        "Entidad beneficiaria", "/beneficiario-dashboard");
  }

  public Modelo comoAdmin() {
    datos.put("esAdmin", true);
    return usuario("Administrador", "AD", "ADMIN",
        "Administración", "/admin-dashboard");
  }

  private Modelo usuario(String nombre, String iniciales, String rolTexto,
                         String rolDescripcion, String inicio) {
    Map<String, Object> usuario = new HashMap<>();
    usuario.put("nombre", nombre);
    usuario.put("iniciales", iniciales);
    usuario.put("rolTexto", rolTexto);
    usuario.put("rolDescripcion", rolDescripcion);
    usuario.put("inicio", inicio);
    datos.put("usuario", usuario);
    return this;
  }

  public Map<String, Object> mapa() {
    return datos;
  }
}
