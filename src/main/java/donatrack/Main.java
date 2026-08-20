package donatrack;

import donatrack.gestion.SegmentadorDonaciones;
import donatrack.importacion.ImportadorCSVPersonas;
import donatrack.model.catalogo.Categoria;
import donatrack.model.catalogo.Subcategoria;
import donatrack.model.contacto.MedioContacto;
import donatrack.model.contacto.TipoContacto;
import donatrack.model.donacion.Bien;
import donatrack.model.donacion.CondicionBien;
import donatrack.model.donacion.Donacion;
import donatrack.model.donacion.Unidades;
import donatrack.model.donacion.estado.EntregaFallida;
import donatrack.model.entidad.EntidadBeneficiaria;
import donatrack.notificacion.Notificador;
import donatrack.notificacion.NotificadorDonacionObserver;
import donatrack.notificacion.NotificadorWhatsApp;
import donatrack.notificacion.NotificadorEmail;
import donatrack.notificacion.NotificadorSMS;
import donatrack.model.necesidad.NecesidadRecurrente;
import donatrack.model.persona.Genero;
import donatrack.model.persona.PersonaHumana;
import donatrack.model.persona.PersonaJuridica;
import donatrack.model.persona.TipoOrganizacion;


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

        Categoria mobiliario = new Categoria("Mobiliario");
        Categoria alimentos  = new Categoria("Alimentos");
        Subcategoria sillas  = new Subcategoria("Sillas",              mobiliario);
        Subcategoria mesas   = new Subcategoria("Mesas",               mobiliario);
        Subcategoria fideos  = new Subcategoria("Fideos secos",        alimentos);
        Subcategoria tomates = new Subcategoria("Tomate en tetrapak",  alimentos);

        List<Bien> bienes = List.of(
                new Bien("Silla oficina usada",    sillas,  Unidades.UNIDADES, CondicionBien.NUEVO),
                new Bien("Silla oficina usada",    sillas,  Unidades.UNIDADES, CondicionBien.NUEVO),
                new Bien("Mesa rectangular usada", mesas,   Unidades.UNIDADES, CondicionBien.NUEVO),
                new Bien("Fideos 500g",            fideos,  Unidades.KILOGRAMOS, CondicionBien.NUEVO),
                new Bien("Tetrapak tomate",        tomates, Unidades.UNIDADES, CondicionBien.NUEVO)
        );

        // Segmentar donaciones
        SegmentadorDonaciones segmentador = new SegmentadorDonaciones();
        // agregar la lista de bienes arriba a arcos
        List<Donacion> donaciones = segmentador.segmentar(bienes, arcos);

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

        Categoria vestimenta = new Categoria("Vestimenta");
        Subcategoria ropa = new Subcategoria("Camperas de abrigo", vestimenta);
        Bien campera = new Bien("Campera talle M nueva", ropa, Unidades.UNIDADES, CondicionBien.USADO);
        Donacion donacion = new Donacion(List.of(campera), ropa);

        // Registrar el observer de la donación
        donacion.agregarObserver(
            new NotificadorDonacionObserver(donante, new NotificadorWhatsApp())
        );

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
        Notificador email = new NotificadorEmail();
        Notificador sms = new NotificadorSMS();
        Notificador whatsapp = new NotificadorWhatsApp();
        email.notificar("usuario@mail.com", "Prueba de notificacion por EMAIL");
        sms.notificar("+54 11 1234-5678", "Prueba de notificacion por SMS");
        whatsapp.notificar("+54 11 9876-5432", "Prueba de notificacion por WhatsApp");
        System.out.println();
    }

    static void demo6_entidadBeneficiaria() {
        System.out.println("--- [6] Entidades beneficiarias y necesidades ---");
        Categoria mobiliario = new Categoria("Mobiliario");
        Categoria alimentos  = new Categoria("Alimentos");
        Subcategoria bancos = new Subcategoria("Bancos escolares", mobiliario);
        Subcategoria fideos = new Subcategoria("Fideos secos",     alimentos);

        EntidadBeneficiaria escuela = new EntidadBeneficiaria("Escuela Rural N10");
        escuela.setDireccion("Ruta 3 km 42, Provincia de Buenos Aires");
        escuela.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "escuela10@edu.ar"));

        escuela.registrarNecesidad(new NecesidadRecurrente("Reposicion tras inundacion", 30, bancos));

        EntidadBeneficiaria comedor = new EntidadBeneficiaria("Escobar Sonrisas");
        comedor.registrarNecesidad(new NecesidadRecurrente("Consumo semanal habitual", 100, fideos));

        System.out.println("Necesidades de " + escuela.getNombreDisplay() + ": " + escuela.getNecesidades().size());
        System.out.println("Necesidades de " + comedor.getNombreDisplay() + ": " + comedor.getNecesidades().size());
    }
}
