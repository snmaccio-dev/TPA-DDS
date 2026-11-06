package donatrack.web;

import donatrack.web.integracion.IntegracionException;
import donatrack.web.vista.PerfilVista;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Modelo {

  private final Map<String, Object> datos = new HashMap<>();
  private final Map<String, String> activa = new LinkedHashMap<>();
  private final Map<String, Object> usuario = new HashMap<>();

  private Modelo(String titulo) {
    datos.put("titulo", titulo);
    datos.put("activa", activa);
    datos.put("usuario", usuario);
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

  public Modelo conUsuario(PerfilVista perfil) {
    if (perfil != null) {
      usuario.put("nombre", perfil.getNombre());
      usuario.put("iniciales", perfil.getIniciales());
      usuario.put("email", perfil.getEmail());
    }

    return this;
  }

  public Modelo comoDonante() {
    datos.put("esDonante", true);
    return rol("DONANTE", "Persona donante", "/donante-dashboard");
  }

  public Modelo comoBeneficiaria() {
    datos.put("esBeneficiaria", true);
    return rol("BENEFICIARIO", "Entidad beneficiaria", "/beneficiario-dashboard");
  }

  public Modelo comoAdmin() {
    datos.put("esAdmin", true);
    return rol("ADMIN", "Administración", "/admin-dashboard");
  }

  private Modelo rol(String rolTexto, String rolDescripcion, String inicio) {
    usuario.putIfAbsent("nombre", "Usuario de prueba");
    usuario.putIfAbsent("iniciales", "US");
    usuario.put("rolTexto", rolTexto);
    usuario.put("rolDescripcion", rolDescripcion);
    usuario.put("inicio", inicio);
    return this;
  }

  public Map<String, Object> mapa() {
    return datos;
  }
}
