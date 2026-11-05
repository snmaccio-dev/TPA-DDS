package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.RegistroBeneficiariaRequest;
import donatrack.donaciones.controller.dto.RegistroDonanteRequest;
import donatrack.donaciones.controller.dto.RepresentanteRequest;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.Representante;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.seguridad.RolDeAcceso;
import donatrack.donaciones.service.DatosEntidad;
import donatrack.donaciones.service.GestorRegistro;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.List;

public class RegistroController {

    private final GestorRegistro gestor;

    public RegistroController(GestorRegistro gestor) {
        this.gestor = gestor;
    }

    public void registrarRutas(Javalin app) {
        app.post("/registro/donantes", this::registrarDonante, RolDeAcceso.PUBLICO);
        app.post("/registro/beneficiarias", this::registrarBeneficiaria, RolDeAcceso.PUBLICO);
    }

    private void registrarDonante(Context ctx) {
        RegistroDonanteRequest request = ctx.bodyAsClass(RegistroDonanteRequest.class);
        Donante donante = gestor.registrarDonante(
            request.aPersona(),
            request.email(),
            request.contrasena()
        );
        if (request.direccion() != null && !request.direccion().isBlank()) {
            donante.getPersona().setDireccion(request.direccion());
        }
        ctx.status(201).json(new RegistroDonanteResponse(donante.getId()));
    }

    private void registrarBeneficiaria(Context ctx) {
        RegistroBeneficiariaRequest request = ctx.bodyAsClass(RegistroBeneficiariaRequest.class);
        PersonaJuridica juridica = new PersonaJuridica(
            request.cuit(),
            request.razonSocial(),
            request.tipoOrganizacion() == null ? null : TipoOrganizacion.valueOf(request.tipoOrganizacion()),
            request.rubro()
        );
        DatosEntidad datos = new DatosEntidad(
            request.direccion(),
            request.telefono(),
            mapRepresentantes(request.representantes())
        );
        Beneficiaria beneficiaria = gestor.registrarBeneficiaria(
            juridica,
            datos,
            request.email(),
            request.contrasena()
        );
        ctx.status(201).json(new RegistroBeneficiariaResponse(beneficiaria.getId()));
    }

    private static List<Representante> mapRepresentantes(List<RepresentanteRequest> representantes) {
        if (representantes == null) {
            return List.of();
        }
        return representantes.stream().map(RepresentanteRequest::aRepresentante).toList();
    }

    public record RegistroDonanteResponse(Long donanteId) {}

    public record RegistroBeneficiariaResponse(Long beneficiariaId) {}
}