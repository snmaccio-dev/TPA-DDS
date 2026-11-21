package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.DatosInvalidosException;

import java.util.regex.Pattern;

// Reglas de formato de los datos de donantes y entidades beneficiarias. Es el unico lugar
// donde viven: las usan el registro publico, el alta y la modificacion por administracion y
// la importacion CSV. Cada metodo valida un campo y devuelve el valor normalizado, o lanza
// DatosInvalidosException con un mensaje que dice el campo y la regla.
//
// Web repite estas reglas en el formulario de registro (atributos pattern) para avisar
// antes de enviar; si se cambian aca, hay que cambiarlas tambien en registro.html.hbs.
public final class ReglasDePersona {

    public static final int EDAD_MINIMA = 18;
    public static final int LETRAS_MINIMAS = 3;

    // Letras permitidas en nombres y apellidos: las del espanol, con tildes, dieresis y enie.
    private static final String LETRAS = "A-Za-zÁÉÍÓÚáéíóúÜüÑñ";
    private static final Pattern NOMBRE = Pattern.compile("[" + LETRAS + "' ]+");
    private static final Pattern LETRA = Pattern.compile("[" + LETRAS + "]");
    private static final Pattern LETRA_CUALQUIERA = Pattern.compile("\\p{L}");
    private static final Pattern DOCUMENTO = Pattern.compile("\\d{7,8}");
    private static final Pattern CUIT = Pattern.compile("\\d{11}|\\d{2}-\\d{8}-\\d");
    private static final Pattern TELEFONO = Pattern.compile("\\d{8,15}");
    // usuario@dominio.ext: sin espacios, una sola @ y al menos un punto despues de ella.
    private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

    private ReglasDePersona() {
    }

    // DNI: solo digitos, 7 u 8.
    public static String documento(String documento) {
        String valor = documento == null ? "" : documento.trim();
        if (!DOCUMENTO.matcher(valor).matches()) {
            throw new DatosInvalidosException(
                "El documento tiene que tener 7 u 8 dígitos, solo números (sin puntos).");
        }
        return valor;
    }

    // Nombre o apellido de una persona: letras, espacios y apostrofo, con al menos 3
    // letras. campo es como se nombra en el mensaje ("El nombre", "El apellido del
    // representante"). Recorta los extremos y colapsa los espacios repetidos.
    public static String nombre(String campo, String nombre) {
        String valor = colapsarEspacios(nombre);
        if (valor.isEmpty()) {
            throw new DatosInvalidosException(campo + " es obligatorio.");
        }
        if (!NOMBRE.matcher(valor).matches() || contar(LETRA, valor) < LETRAS_MINIMAS) {
            throw new DatosInvalidosException(campo + " tiene que tener al menos "
                + LETRAS_MINIMAS + " letras y solo puede tener letras, espacios y apóstrofo (').");
        }
        return valor;
    }

    public static int edad(Integer edad) {
        if (edad == null || edad < EDAD_MINIMA) {
            throw new DatosInvalidosException(
                "La edad tiene que ser un número entero mayor o igual a " + EDAD_MINIMA + ".");
        }
        return edad;
    }

    // Correo con formato usuario@dominio.ext. campo: "El correo", "El correo del
    // representante". Se guarda sin espacios en los extremos y en minusculas.
    public static String email(String campo, String email) {
        String valor = email == null ? "" : email.trim();
        if (valor.isEmpty()) {
            throw new DatosInvalidosException(campo + " es obligatorio.");
        }
        if (!EMAIL.matcher(valor).matches()) {
            throw new DatosInvalidosException(
                campo + " tiene que tener el formato usuario@dominio.ext, sin espacios.");
        }
        return valor.toLowerCase();
    }

    // Al menos 3 letras; puede tener numeros y signos ("Escuela N° 14", "S.A.").
    public static String razonSocial(String razonSocial) {
        String valor = colapsarEspacios(razonSocial);
        if (contar(LETRA_CUALQUIERA, valor) < LETRAS_MINIMAS) {
            throw new DatosInvalidosException(
                "La razón social tiene que tener al menos " + LETRAS_MINIMAS + " letras.");
        }
        return valor;
    }

    // 11 digitos, con o sin guiones (XX-XXXXXXXX-X). No se valida el digito verificador ni
    // el prefijo. Devuelve el CUIT sin guiones.
    public static String cuit(String cuit) {
        String valor = cuit == null ? "" : cuit.trim();
        if (!CUIT.matcher(valor).matches()) {
            throw new DatosInvalidosException(
                "El CUIT tiene que tener 11 dígitos, con o sin guiones (XX-XXXXXXXX-X).");
        }
        return valor.replace("-", "");
    }

    // Solo digitos, entre 8 y 15: nada de +, guiones ni espacios. campo: "El teléfono",
    // "El WhatsApp".
    public static String telefono(String campo, String telefono) {
        String valor = telefono == null ? "" : telefono.trim();
        if (!TELEFONO.matcher(valor).matches()) {
            throw new DatosInvalidosException(
                campo + " tiene que tener entre 8 y 15 dígitos, solo números.");
        }
        return valor;
    }

    // Representante de una entidad: nombre, apellido y correo, con las reglas de arriba.
    public static void representante(Representante representante) {
        if (representante == null) {
            throw new DatosInvalidosException("El representante es obligatorio.");
        }
        nombre("El nombre del representante", representante.getNombre());
        nombre("El apellido del representante", representante.getApellido());
        email("El correo del representante", representante.getEmail());
    }

    static String colapsarEspacios(String valor) {
        return valor == null ? "" : valor.trim().replaceAll("\\s+", " ");
    }

    private static long contar(Pattern patron, String valor) {
        return patron.matcher(valor).results().count();
    }
}
