package donatrack.model.logistica;

import java.time.LocalDateTime;

public record Comprobante(
    long donacionId,
    LocalDateTime fecha,
    String patenteCamion,
    String razonSocialBeneficiaria,
    String nombreDonante,
    String descripcionDonacion
) {}
