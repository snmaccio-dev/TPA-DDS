package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.DatosInvalidosException;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Objects;

// Ubicacion geografica (WGS84, grados decimales) de la direccion de una entidad. Es un
// valor: no cambia, se reemplaza. En la base son dos columnas opcionales; si las dos son
// null, Hibernate deja el embebido en null.
@Embeddable
public class Coordenadas {

    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    protected Coordenadas() {
    }

    public Coordenadas(double latitud, double longitud) {
        if (Double.isNaN(latitud) || latitud < -90 || latitud > 90) {
            throw new DatosInvalidosException("La latitud tiene que estar entre -90 y 90.");
        }
        if (Double.isNaN(longitud) || longitud < -180 || longitud > 180) {
            throw new DatosInvalidosException("La longitud tiene que estar entre -180 y 180.");
        }
        this.latitud = latitud;
        this.longitud = longitud;
    }

    // Para datos opcionales que llegan de un request: sin ninguna de las dos devuelve null;
    // con una sola, es un error (no hay media ubicacion).
    public static Coordenadas opcionales(Double latitud, Double longitud) {
        if (latitud == null && longitud == null) {
            return null;
        }
        if (latitud == null || longitud == null) {
            throw new DatosInvalidosException(
                "La latitud y la longitud se indican juntas: falta una de las dos.");
        }
        return new Coordenadas(latitud, longitud);
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Coordenadas coordenadas)) {
            return false;
        }
        return Objects.equals(latitud, coordenadas.latitud)
            && Objects.equals(longitud, coordenadas.longitud);
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitud, longitud);
    }

    @Override
    public String toString() {
        return latitud + "," + longitud;
    }
}
