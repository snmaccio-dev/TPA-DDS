package donatrack.donaciones.domain.usuario;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

// Representa el vinculo entre una cuenta de Keycloak y un perfil local
// (donante o beneficiaria). Donaciones no persiste ni credenciales ni roles:
// eso lo tiene Keycloak. Varios Usuarios pueden apuntar al mismo perfil
// cuando una persona juridica tiene varios representantes con cuenta propia.
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @Column(name = "keycloak_id")
    private String keycloakId;

    @ManyToOne
    @JoinColumn(name = "donante_id")
    private Donante donante;

    @ManyToOne
    @JoinColumn(name = "beneficiaria_id")
    private Beneficiaria beneficiaria;

    protected Usuario() {
    }

    private Usuario(String keycloakId, Donante donante, Beneficiaria beneficiaria) {
        this.keycloakId = keycloakId;
        this.donante = donante;
        this.beneficiaria = beneficiaria;
    }

    public static Usuario deDonante(String keycloakId, Donante donante) {
        validarKeycloakId(keycloakId);
        if (donante == null) {
            throw new DatosInvalidosException("El usuario debe estar asociado a un donante.");
        }
        return new Usuario(keycloakId, donante, null);
    }

    public static Usuario deBeneficiaria(String keycloakId, Beneficiaria beneficiaria) {
        validarKeycloakId(keycloakId);
        if (beneficiaria == null) {
            throw new DatosInvalidosException("El usuario debe estar asociado a una beneficiaria.");
        }
        return new Usuario(keycloakId, null, beneficiaria);
    }

    private static void validarKeycloakId(String keycloakId) {
        if (keycloakId == null || keycloakId.isBlank()) {
            throw new DatosInvalidosException("El identificador de Keycloak es obligatorio.");
        }
    }

    public String getKeycloakId() {
        return keycloakId;
    }

    public Donante getDonante() {
        return donante;
    }

    public Beneficiaria getBeneficiaria() {
        return beneficiaria;
    }
}
