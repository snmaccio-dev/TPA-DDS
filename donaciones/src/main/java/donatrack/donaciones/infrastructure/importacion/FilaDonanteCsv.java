package donatrack.donaciones.infrastructure.importacion;

import donatrack.donaciones.domain.persona.PersonaJuridica;

public record FilaDonanteCsv(
    int numero,
    TipoPersona tipoPersona,
    String tipoDocumento,
    String documento,
    String nombre,
    String email,
    String telefono
) implements LineaCsv {

  public enum TipoPersona {
    HUMANA,
    JURIDICA
  }

  public String documentoNormalizado() {
    return tipoPersona == TipoPersona.JURIDICA
        ? PersonaJuridica.normalizarCuit(documento)
        : documento;
  }

  public boolean tieneTelefono() {
    return telefono != null && !telefono.isBlank();
  }
}
