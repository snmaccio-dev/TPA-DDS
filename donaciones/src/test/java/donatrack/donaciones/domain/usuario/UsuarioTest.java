package donatrack.donaciones.domain.usuario;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UsuarioTest {

  @Test
  public void deDonanteGuardaElVinculoConElDonante() {
    Donante donante = new Donante(new PersonaHumana("Ana", "Perez", 30, "111", Genero.FEMENINO));
    Usuario usuario = Usuario.deDonante("kc-123", donante);

    assertEquals("kc-123", usuario.getKeycloakId());
    assertSame(donante, usuario.getDonante());
    assertNull(usuario.getBeneficiaria());
  }

  @Test
  public void deBeneficiariaGuardaElVinculoConLaBeneficiaria() {
    Beneficiaria beneficiaria = new Beneficiaria(
        new PersonaJuridica("30-11111111-1", "Comedor X", TipoOrganizacion.ONG, "Comedor")
    );
    Usuario usuario = Usuario.deBeneficiaria("kc-abc", beneficiaria);

    assertEquals("kc-abc", usuario.getKeycloakId());
    assertSame(beneficiaria, usuario.getBeneficiaria());
    assertNull(usuario.getDonante());
  }

  @Test
  public void deDonanteRechazaKeycloakIdVacio() {
    Donante donante = new Donante(new PersonaHumana("Ana", "Perez", 30, "111", Genero.FEMENINO));
    assertThrows(DatosInvalidosException.class, () -> Usuario.deDonante("", donante));
    assertThrows(DatosInvalidosException.class, () -> Usuario.deDonante(null, donante));
    assertThrows(DatosInvalidosException.class, () -> Usuario.deDonante("   ", donante));
  }

  @Test
  public void deDonanteRechazaDonanteNull() {
    assertThrows(DatosInvalidosException.class, () -> Usuario.deDonante("kc-123", null));
  }

  @Test
  public void deBeneficiariaRechazaKeycloakIdVacioYPerfilNull() {
    Beneficiaria beneficiaria = new Beneficiaria(
        new PersonaJuridica("30-11111111-2", "Escuela", TipoOrganizacion.INSTITUCION, "Educacion")
    );
    assertThrows(DatosInvalidosException.class, () -> Usuario.deBeneficiaria("", beneficiaria));
    assertThrows(DatosInvalidosException.class, () -> Usuario.deBeneficiaria(null, beneficiaria));
    assertThrows(DatosInvalidosException.class, () -> Usuario.deBeneficiaria("kc-xyz", null));
  }
}