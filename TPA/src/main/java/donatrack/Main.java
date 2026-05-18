package donatrack;

import donatrack.importacion.ImportadorCSVPersonas;
import donatrack.model.catalogo.Subcategoria;
import donatrack.model.contacto.MedioContacto;
import donatrack.model.contacto.TipoContacto;
import donatrack.model.donacion.Bien;
import donatrack.model.donacion.Donacion;
import donatrack.model.donacion.Unidades;
import donatrack.model.donacion.estado.EntregaFallida;
import donatrack.model.entidad.EntidadBeneficiaria;
import donatrack.model.necesidad.Necesidad;
import donatrack.model.persona.Genero;
import donatrack.model.persona.PersonaHumana;
import donatrack.model.persona.PersonaJuridica;
import donatrack.model.persona.TipoOrganizacion;
import donatrack.notificacion.NotificadorDonacionObserver;
import donatrack.notificacion.ServicioNotificaciones;
import donatrack.servicio.ServicioDonaciones;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== DonaTrack — Entrega 1 ===\n");

        demo1_crearPersonas();
        demo2_segmentacionDonaciones();
        demo3_estadosDonacion();
        demo4_importacionCSV();
        demo5_notificaciones();
        demo6_entidadBeneficiaria();
    }

    static void demo1_crearPersonas() {
        System.out.println("--- [1] Registro de personas donantes ---");

        PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "12345678", Genero.FEMENINO);
        ana.setDireccion("Av. Corrientes 1234, CABA");
        ana.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "ana@mail.com"));
        ana.agregarMedioContacto(new MedioContacto(TipoContacto.WHATSAPP, "+54 11 5555-5555"));
        System.out.println("Persona humana creada: " + ana.getNombreDisplay()
                + " | Contacto predeterminado: " + ana.getContactoPredeterminado().getValor());

        PersonaJuridica arcos = new PersonaJuridica("Arcos Plateados S.A.", TipoOrganizacion.EMPRESA, "Construccion");
        arcos.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "contacto@arcos.com"));
        arcos.agregarRepresentante(ana);
        System.out.println("Persona juridica creada: " + arcos.getNombreDisplay()
                + " | Representantes: " + arcos.getRepresentantes().size());
        System.out.println();
    }

    static void demo2_segmentacionDonaciones() {
        System.out.println("--- [2] Segmentacion automatica de donaciones por subcategoria ---");

        PersonaJuridica arcos = new PersonaJuridica("Arcos Plateados", TipoOrganizacion.EMPRESA, "Mudanza");
        arcos.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "arcos@demo.com"));

        Subcategoria sillas  = new Subcategoria("Sillas");
        Subcategoria mesas   = new Subcategoria("Mesas");
        Subcategoria fideos  = new Subcategoria("Fideos secos");
        Subcategoria tomates = new Subcategoria("Tomate en tetrapak");

        List<Bien> bienes = List.of(
                new Bien("Silla oficina usada",    sillas,  Unidades.UNIDADES),
                new Bien("Silla oficina usada",    sillas,  Unidades.UNIDADES),
                new Bien("Mesa rectangular usada", mesas,   Unidades.UNIDADES),
                new Bien("Fideos 500g",            fideos,  Unidades.KILOGRAMOS),
                new Bien("Tetrapak tomate",        tomates, Unidades.UNIDADES)
        );

        ServicioDonaciones servicio = new ServicioDonaciones();
        List<Donacion> donaciones = servicio.ingresarDonacion(bienes, arcos);

        System.out.println("Bienes ingresados: " + bienes.size());
        System.out.println("Donaciones generadas: " + donaciones.size());
        donaciones.forEach(d ->
                System.out.println("  → " + d.getSubcategoria().getNombre()
                        + " [" + d.getBienes().size() + " bien(es)] estado: " + d.getEstado().getNombre())
        );
        System.out.println();
    }

    static void demo3_estadosDonacion() {
        System.out.println("--- [3] Ciclo de estados de una donacion ---");

        PersonaHumana donante = new PersonaHumana("Luis", "Garcia", 45, "87654321", Genero.MASCULINO);
        donante.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "luis@mail.com"));

        Subcategoria ropa = new Subcategoria("Camperas de abrigo");
        Bien campera = new Bien("Campera talle M nueva", ropa, Unidades.UNIDADES);
        Donacion donacion = new Donacion(List.of(campera), ropa);

        ServicioNotificaciones svc = new ServicioNotificaciones();
        donacion.agregarObserver(new NotificadorDonacionObserver(donante, svc));

        System.out.println("Estado inicial: " + donacion.getEstado().getNombre());
        donacion.asignar();
        System.out.println("Tras asignar:   " + donacion.getEstado().getNombre());
        donacion.planificarRuta();
        System.out.println("Tras planificar ruta: " + donacion.getEstado().getNombre());
        donacion.iniciarTraslado();
        System.out.println("Tras iniciar traslado: " + donacion.getEstado().getNombre());
        donacion.fallarEntrega("Tocamos timbre pero nadie respondio");
        System.out.println("Tras entrega fallida: " + donacion.getEstado().getNombre());

        // Volver al deposito desde entrega fallida
        ((EntregaFallida) donacion.getEstado()).devolverAlDeposito(donacion);
        System.out.println("Vuelve al deposito: " + donacion.getEstado().getNombre());

        // Transicion invalida — debe lanzar excepcion
        System.out.print("Transicion invalida (confirmarEntrega desde EN_DEPOSITO): ");
        try {
            donacion.confirmarEntrega();
        } catch (IllegalStateException e) {
            System.out.println("excepcion capturada correctamente → " + e.getMessage());
        }
        System.out.println();
    }

    static void demo4_importacionCSV() {
        System.out.println("--- [4] Importacion masiva CSV ---");
        String ruta = "src/main/resources/donantes_prueba.csv"; //Se puede cambiar por "donantes_prueba_2.csv" para validar el funcionamiento a mayor escala.
        ImportadorCSVPersonas importador = new ImportadorCSVPersonas();
        try {
            importador.importarConResumen(ruta);
        } catch (RuntimeException e) {
            System.out.println("[CSV] Archivo no encontrado en: " + ruta + " — " + e.getMessage());
        }
        System.out.println();
    }

    static void demo5_notificaciones() {
        System.out.println("--- [5] Notificaciones simuladas (Strategy) ---");
        ServicioNotificaciones svc = new ServicioNotificaciones();
        svc.notificarPorMedio("usuario@mail.com",   TipoContacto.EMAIL,    "Prueba de notificacion por EMAIL");
        svc.notificarPorMedio("+54 11 1234-5678",   TipoContacto.TELEFONO, "Prueba de notificacion por SMS");
        svc.notificarPorMedio("+54 11 9876-5432",   TipoContacto.WHATSAPP, "Prueba de notificacion por WhatsApp");
        System.out.println();
    }

    static void demo6_entidadBeneficiaria() {
        System.out.println("--- [6] Entidades beneficiarias y necesidades ---");
        Subcategoria bancos = new Subcategoria("Bancos escolares");
        Subcategoria fideos = new Subcategoria("Fideos secos");

        EntidadBeneficiaria escuela = new EntidadBeneficiaria("Escuela Rural N10");
        escuela.setDireccion("Ruta 3 km 42, Provincia de Buenos Aires");
        escuela.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "escuela10@edu.ar"));

        escuela.registrarNecesidades(new Necesidad("Reposicion tras inundacion", 30, bancos));

        EntidadBeneficiaria comedor = new EntidadBeneficiaria("Escobar Sonrisas");
        comedor.registrarNecesidades(new Necesidad("Consumo semanal habitual", 100, fideos));

        System.out.println("Necesidades de " + escuela.getNombreDisplay() + ": " + escuela.getNecesidades().size());
        System.out.println("Necesidades de " + comedor.getNombreDisplay() + ": " + comedor.getNecesidades().size());
    }
}
