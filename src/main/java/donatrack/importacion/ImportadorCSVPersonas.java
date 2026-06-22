package donatrack.importacion;

import donatrack.model.contacto.TipoContacto;
import donatrack.model.persona.Persona;
import donatrack.repositorio.RepositorioPersonas;

public class ImportadorCSVPersonas extends ImportadorCSV<Persona> {

    private final RepositorioPersonas repositorio = RepositorioPersonas.getInstance();
    private final ServicioNotificaciones servicioNotificaciones = new ServicioNotificaciones();
    private int creados = 0;
    private int actualizados = 0;

    @Override
    protected Persona procesarFila(String[] campos) {
        Persona persona = PersonaFactory.crear(campos);
        String email = campos[4].trim();

        if (repositorio.existe(email)) {
            // actualizar datos
            repositorio.guardar(email, persona);
            actualizados++;
        } else {
            // crear y enviar credenciales
            repositorio.guardar(email, persona);
            servicioNotificaciones.notificarPorMedio(email, TipoContacto.EMAIL,
                    "Bienvenido a DonaTrack. Su email de acceso es: " + email);
            creados++;
        }
        return persona;
    }

    public void importarConResumen(String rutaArchivo) {
        creados = 0;
        actualizados = 0;
        importar(rutaArchivo);
        System.out.println("[CSV] Importacion finalizada. Creados: " + creados
                + " | Actualizados: " + actualizados
                + " | Total en repositorio: " + repositorio.cantidad());
    }
}
