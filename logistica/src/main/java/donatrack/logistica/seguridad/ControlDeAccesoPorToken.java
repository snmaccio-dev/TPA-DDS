package donatrack.logistica.seguridad;

import donatrack.logistica.contrato.PerfilDeUsuario;
import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.integracion.DirectorioDeUsuarios;
import donatrack.logistica.domain.ruta.RutaReparto;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;

import java.util.Objects;

public class ControlDeAccesoPorToken implements ControlDeAcceso {

  private static final String CLAVE_PERFIL = "perfilDelUsuario";

  private final DirectorioDeUsuarios directorio;

  public ControlDeAccesoPorToken(DirectorioDeUsuarios directorio) {
    Objects.requireNonNull(directorio, "Debe indicarse el directorio de usuarios.");
    this.directorio = directorio;
  }

  @Override
  public void exigirAccesoAEntrega(Context ctx, Entrega entrega) {
    UsuarioActual usuario = FiltroDeAcceso.usuarioDe(ctx);

    if (usuario.esAdministrador() || usuario.esServicioLogistica() || usuario.esChofer()) {
      return;
    }

    if (usuario.esBeneficiaria()
        && perfil(ctx).esLaBeneficiaria(entrega.getBeneficiariaId())) {
      return;
    }

    if (usuario.esDonante() && esSuDonacion(entrega, perfil(ctx))) {
      return;
    }

    throw new ForbiddenResponse("No podes acceder a esta entrega.");
  }

  @Override
  public void exigirAccesoARuta(Context ctx, RutaReparto ruta) {
    UsuarioActual usuario = FiltroDeAcceso.usuarioDe(ctx);

    if (usuario.esAdministrador() || usuario.esServicioLogistica() || usuario.esChofer()) {
      return;
    }

    if (usuario.esBeneficiaria() && tieneEntregaDe(ruta, perfil(ctx))) {
      return;
    }

    if (usuario.esDonante() && tieneDonacionDe(ruta, perfil(ctx))) {
      return;
    }

    throw new ForbiddenResponse("No podes acceder a esta ruta.");
  }

  @Override
  public AlcanceDeMonitoreo alcanceDeMonitoreo(Context ctx, AlcanceDeMonitoreo pedido) {
    UsuarioActual usuario = FiltroDeAcceso.usuarioDe(ctx);

    if (usuario.esAdministrador()) {
      return pedido;
    }

    if (usuario.esBeneficiaria()) {
      PerfilDeUsuario perfil = perfil(ctx);

      if (!perfil.tienePerfilDeBeneficiaria()) {
        throw new ForbiddenResponse("La cuenta no tiene un perfil de entidad beneficiaria.");
      }
      if (pedido.esDeBeneficiaria() && !perfil.esLaBeneficiaria(pedido.beneficiariaId())) {
        throw new ForbiddenResponse("No podes ver el seguimiento de otra entidad.");
      }

      return AlcanceDeMonitoreo.deBeneficiaria(perfil.beneficiariaId());
    }

    if (usuario.esDonante()) {
      PerfilDeUsuario perfil = perfil(ctx);

      if (!perfil.tienePerfilDeDonante()) {
        throw new ForbiddenResponse("La cuenta no tiene un perfil de persona donante.");
      }
      if (pedido.esDeDonante() && !perfil.esElDonante(pedido.donanteId())) {
        throw new ForbiddenResponse("No podes ver el seguimiento de otra persona donante.");
      }

      return AlcanceDeMonitoreo.deDonante(perfil.donanteId());
    }

    throw new ForbiddenResponse("No podes acceder al seguimiento solicitado.");
  }

  private boolean tieneEntregaDe(RutaReparto ruta, PerfilDeUsuario perfil) {
    return ruta.getEntregas().stream()
        .anyMatch(entrega -> perfil.esLaBeneficiaria(entrega.getBeneficiariaId()));
  }

  private boolean tieneDonacionDe(RutaReparto ruta, PerfilDeUsuario perfil) {
    return ruta.getEntregas().stream().anyMatch(entrega -> esSuDonacion(entrega, perfil));
  }

  private boolean esSuDonacion(Entrega entrega, PerfilDeUsuario perfil) {
    return entrega.getDonanteId() != null && perfil.esElDonante(entrega.getDonanteId());
  }

  private PerfilDeUsuario perfil(Context ctx) {
    PerfilDeUsuario cacheado = ctx.attribute(CLAVE_PERFIL);

    if (cacheado != null) {
      return cacheado;
    }

    PerfilDeUsuario consultado = directorio.perfilDe(ctx.header("Authorization"));
    ctx.attribute(CLAVE_PERFIL, consultado);

    return consultado;
  }
}
