package donatrack.importacion;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Template Method — define el esqueleto del proceso de importacion CSV
public abstract class ImportadorCSV<T> {

    // Template method: flujo fijo, procesarFila() es el paso variable
    public final List<T> importar(String rutaArchivo) {
        List<T> resultado = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            boolean primeraLinea = true;
            while ((linea = reader.readLine()) != null) {
                if (primeraLinea) {
                    primeraLinea = false;
                    continue; // saltar cabecera
                }
                if (linea.isBlank()) continue;
                String[] campos = linea.split(",", -1);
                try {
                    T entidad = procesarFila(campos);
                    if (entidad != null) {
                        resultado.add(entidad);
                    }
                } catch (Exception e) {
                    System.out.println("[CSV] Error en fila: " + linea + " → " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el archivo CSV: " + rutaArchivo, e);
        }
        return resultado;
    }

    protected abstract T procesarFila(String[] campos);
}
