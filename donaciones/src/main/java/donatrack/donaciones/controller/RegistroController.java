package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.RegistroBeneficiariaRequest;
import donatrack.donaciones.controller.dto.RegistroDonanteRequest;
import donatrack.donaciones.controller.dto.RepresentanteRequest;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.Representante;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.service.DatosEntidad;
import donatrack.donaciones.service.DonantesService;
import donatrack.donaciones.service.EntidadesBeneficiariasService;
import io.javalin.http.Context;

import java.util.List;

public class RegistroController {

    private final DonantesService donantesService;
    private final EntidadesBeneficiariasService entidadesService;

    public RegistroController(DonantesService donantesService,
                              EntidadesBeneficiariasService entidadesService) {
        this.donantesService = donantesService;
        this.entidadesService = entidadesService;
    }

    public void registrarDonante(Context ctx) {
        RegistroDonanteRequest request = ctx.bodyAsClass(RegistroDonanteRequest.class);
        Persona persona = request.aPersona();
        if (request.direccion() != null && !request.direccion().isBlank()) {
            persona.setDireccion(request.direccion());
        }
        Donante donante = donantesService.registrarPublico(
            persona,
            request.email(),
            request.contrasena()
        );
        ctx.status(201).json(new RegistroDonanteResponse(donante.getId()));
    }

    public void registrarBeneficiaria(Context ctx) {
        RegistroBeneficiariaRequest request = ctx.bodyAsClass(RegistroBeneficiariaRequest.class);
        PersonaJuridica juridica = PersonaJuridica.nueva(
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
        Beneficiaria beneficiaria = entidadesService.registrarPublica(
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
