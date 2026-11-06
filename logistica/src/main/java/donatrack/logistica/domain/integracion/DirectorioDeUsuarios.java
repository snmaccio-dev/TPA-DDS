package donatrack.logistica.domain.integracion;

import donatrack.logistica.contrato.PerfilDeUsuario;

public interface DirectorioDeUsuarios {

  PerfilDeUsuario perfilDe(String headerAutorizacion);
}
