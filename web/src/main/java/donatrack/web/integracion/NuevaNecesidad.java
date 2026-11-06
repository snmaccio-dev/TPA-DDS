package donatrack.web.integracion;

// cuitBeneficiaria viaja en null a proposito: cuando quien llama es una beneficiaria,
// Donaciones resuelve la entidad desde el token y lo ignora. Mandarlo desde el
// formulario permitiria que alguien cargue necesidades en nombre de otra entidad.
public record NuevaNecesidad(
    String cuitBeneficiaria,
    String descripcion,
    int cantidad,
    String subcategoria,
    String tipo,
    String periodo
) {
}
