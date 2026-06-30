package gestion;

import donatrack.model.contacto.MedioContacto;
import donatrack.model.contacto.TipoContacto;
import donatrack.model.persona.Persona;
import donatrack.model.usuario.Usuario;
import donatrack.notificacion.*;
import donatrack.repositorio.RepositorioPersonas;

import java.util.Optional;
import java.util.UUID;

public class GestorPersonas {

    private final RepositorioPersonas repositorio = RepositorioPersonas.getInstance();
    private final ServicioNotificaciones servicioNotificaciones = new ServicioNotificaciones();

    public void registrar(String email, Persona persona) {
        Optional<Persona> existente = repositorio.buscarPorEmail(email);
        if (existente.isPresent()) {
            actualizarDatos(existente.get(), persona);
            System.out.println("[REGISTRO] Persona actualizada: " + email);
        } else {
            String contrasena = generarContrasena();
            persona.setUsuario(new Usuario(email, contrasena));
            persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, email));
            repositorio.guardar(email, persona);
            servicioNotificaciones.notificarPorMedio(email, TipoContacto.EMAIL,
                    "Bienvenido a DonaTrack. Su usuario: " + email + " | Contrasena: " + contrasena);
            System.out.println("[REGISTRO] Persona creada: " + email);
        }
    }

    private void actualizarDatos(Persona existente, Persona nueva) {
        if (nueva.getDireccion() != null) {
            existente.setDireccion(nueva.getDireccion());
        }
    }

    private String generarContrasena() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

  public static class ServicioNotificaciones {

      public void notificar(Persona destinatario, String mensaje) {
          MedioContacto medio = destinatario.getContactoPredeterminado();
          if (medio == null) {
              System.out.println("[NOTIFICACION] Sin medio de contacto para " + destinatario.getNombreDisplay());
              return;
          }
          Notificador notificador = resolverNotificador(medio.getTipo());
          notificador.notificar(medio.getValor(), mensaje);
      }

      public void notificarPorMedio(String valor, TipoContacto tipo, String mensaje) {
          Notificador notificador = resolverNotificador(tipo);
          notificador.notificar(valor, mensaje);
      }

      private Notificador resolverNotificador(TipoContacto tipo) {
          return switch (tipo) {
              case EMAIL    -> new NotificadorEmail();
              case TELEFONO -> new NotificadorSMS();
              case WHATSAPP -> new NotificadorWhatsApp();
          };
      }
  }
}
