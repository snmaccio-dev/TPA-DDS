package donatrack.donaciones.service;

import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.importacion.ErrorDeImportacion;
import donatrack.donaciones.domain.importacion.ImportacionCsv;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.infrastructure.importacion.FilaDonanteCsv;
import donatrack.donaciones.infrastructure.importacion.ImportacionCsvException;
import donatrack.donaciones.infrastructure.importacion.LectorCsvDonantes;
import donatrack.donaciones.infrastructure.importacion.LineaCsv;
import donatrack.donaciones.infrastructure.importacion.LineaInvalida;
import donatrack.donaciones.infrastructure.importacion.PersonaFactory;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioImportaciones;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioUsuarios;
import donatrack.donaciones.seguridad.RolDeAcceso;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import java.util.HashSet;
import java.util.Set;

public class ImportacionDonantesService implements WithSimplePersistenceUnit {

  public static final int TAMANIO_TANDA = 200;
  public static final int CANTIDAD_ULTIMAS = 20;

  private static final Logger LOG = LoggerFactory.getLogger(ImportacionDonantesService.class);

  private final LectorCsvDonantes lector;
  private final RepositorioImportaciones repositorioImportaciones;
  private final RepositorioPersonas repositorioPersonas;
  private final RepositorioDonantes repositorioDonantes;
  private final RepositorioUsuarios repositorioUsuarios;
  private final UsuariosService usuariosService;
  private final Executor executor;
  private final boolean enviarMails;

  public ImportacionDonantesService(LectorCsvDonantes lector,
                                    RepositorioImportaciones repositorioImportaciones,
                                    RepositorioPersonas repositorioPersonas,
                                    RepositorioDonantes repositorioDonantes,
                                    RepositorioUsuarios repositorioUsuarios,
                                    UsuariosService usuariosService,
                                    Executor executor,
                                    boolean enviarMails) {
    this.lector = lector;
    this.repositorioImportaciones = repositorioImportaciones;
    this.repositorioPersonas = repositorioPersonas;
    this.repositorioDonantes = repositorioDonantes;
    this.repositorioUsuarios = repositorioUsuarios;
    this.usuariosService = usuariosService;
    this.executor = executor;
    this.enviarMails = enviarMails;
  }

  public long iniciar(String nombreArchivo, InputStream contenido) {
    Path temporal = copiarATemporal(contenido);
    ImportacionCsv importacion = new ImportacionCsv(nombreArchivo);
    try {
      withTransaction(() -> repositorioImportaciones.guardar(importacion));
    } catch (RuntimeException e) {
      borrar(temporal);
      throw e;
    }
    long id = importacion.getId();
    executor.execute(() -> procesarYBorrar(id, temporal));
    return id;
  }

  public ImportacionCsv buscar(long id) {
    entityManager().clear();
    return repositorioImportaciones.buscarPorId(id)
        .orElseThrow(() -> new RecursoInexistenteException(
            "No existe la importacion con id " + id));
  }

  public List<ImportacionCsv> ultimas() {
    entityManager().clear();
    return repositorioImportaciones.ultimas(CANTIDAD_ULTIMAS);
  }

  private void procesarYBorrar(long id, Path archivo) {
    try {
      procesar(id, archivo);
    } finally {
      borrar(archivo);
      entityManager().clear();
    }
  }

  private void procesar(long id, Path archivo) {
    Avance pendiente = new Avance();
    List<FilaDonanteCsv> tanda = new ArrayList<>();
    try (Stream<LineaCsv> lineas = lector.leer(archivo)) {
      Iterator<LineaCsv> iterador = lineas.iterator();
      while (iterador.hasNext()) {
        LineaCsv linea = iterador.next();
        pendiente.filasLeidas++;
        if (linea instanceof LineaInvalida invalida) {
          pendiente.error(invalida.numero(), invalida.mensaje());
        } else {
          tanda.add((FilaDonanteCsv) linea);
        }
        if (tanda.size() == TAMANIO_TANDA) {
          pendiente.sumar(procesarTanda(tanda));
          registrarAvance(id, pendiente);
          pendiente = new Avance();
          tanda = new ArrayList<>();
        }
      }
      pendiente.sumar(procesarTanda(tanda));
      registrarAvance(id, pendiente);
      withTransaction(() -> importacion(id).finalizar());
    } catch (RuntimeException e) {
      LOG.warn("La importacion {} fallo: {}", id, mensajeDe(e));
      entityManager().clear();
      registrarFallo(id, mensajeDe(e));
    }
  }

  private Avance procesarTanda(List<FilaDonanteCsv> filas) {
    if (filas.isEmpty()) {
      return new Avance();
    }
    try {
      return intentarTanda(filas);
    } catch (RuntimeException e) {
      entityManager().clear();
      if (filas.size() == 1) {
        Avance conError = new Avance();
        conError.error(filas.get(0).numero(), mensajeDe(e));
        return conError;
      }
      Avance total = new Avance();
      for (FilaDonanteCsv fila : filas) {
        total.sumar(procesarTanda(List.of(fila)));
      }
      return total;
    } finally {
      entityManager().clear();
    }
  }

  /*
  private Avance intentarTanda(List<FilaDonanteCsv> filas) {
    Avance avance = new Avance();
    Tanda tanda = new Tanda();
    for (FilaDonanteCsv fila : filas) {
      try {
        prepararFila(fila, tanda, avance);
      } catch (RuntimeException e) {
        avance.error(fila.numero(), mensajeDe(e));
      }
    }
    try {
      withTransaction(() -> tanda.destinos.forEach(this::persistir));
    } catch (RuntimeException e) {
      tanda.destinos.stream()
          .filter(destino -> destino.cuentaCreada)
          .forEach(destino -> usuariosService.compensar(destino.keycloakId));
      throw e;
    }
    return avance;
  }

  */

    private Avance intentarTanda(List<FilaDonanteCsv> filas) {
      Avance avance = new Avance();
      Tanda tanda = new Tanda();

      IndiceExistentes indice = buscarExistentes(filas);

      for (FilaDonanteCsv fila : filas) {
          try {
              prepararFila(fila, tanda, avance, indice);
          } catch (RuntimeException e) {
              avance.error(fila.numero(), mensajeDe(e));
          }
      }

      try {
          withTransaction(() -> tanda.destinos.forEach(this::persistir));
      } catch (RuntimeException e) {
          tanda.destinos.stream()
              .filter(destino -> destino.cuentaCreada)
              .forEach(destino -> usuariosService.compensar(destino.keycloakId));
          throw e;
      }

      return avance;
  }

  // Nuevo metodo 

  private IndiceExistentes buscarExistentes(List<FilaDonanteCsv> filas) {
    Set<String> emails = new HashSet<>();
    Set<String> documentos = new HashSet<>();
    Set<String> cuis = new HashSet<>();

    for (FilaDonanteCsv fila : filas) {
        emails.add(fila.email());
        documentos.add(fila.documento());
        cuis.add(PersonaJuridica.normalizarCuit(fila.documento()));
    }

    Map<String, Persona> porEmail = new HashMap<>();
    Map<String, PersonaHumana> porDocumento = new HashMap<>();
    Map<String, PersonaJuridica> porCuit = new HashMap<>();

    for (Persona persona : repositorioPersonas.buscarPorEmails(emails)) {
        for (String email : emails) {
            if (persona.tieneContacto(TipoContacto.EMAIL, email)) {
                porEmail.putIfAbsent(email, persona);
            }
        }
    }

    for (PersonaHumana persona :
            repositorioPersonas.buscarHumanasPorDocumentos(documentos)) {
        porDocumento.putIfAbsent(persona.getDocumento(), persona);
    }

    for (PersonaJuridica persona :
            repositorioPersonas.buscarJuridicasPorCuits(cuis)) {
        porCuit.putIfAbsent(persona.getCuit(), persona);
    }

    return new IndiceExistentes(porEmail, porDocumento, porCuit);
}

  /*
  private void prepararFila(FilaDonanteCsv fila, Tanda tanda, Avance avance) {
    Coincidencia coincidencia = buscarCoincidencia(fila, tanda);

    if (coincidencia == null) {
      Destino destino = new Destino(PersonaFactory.crear(fila), true, null);
      obtenerCuenta(destino, fila.email());
      tanda.agregar(destino, fila);
      avance.creados++;
      avance.contarCuenta(destino);
      return;
    }

    validar(coincidencia, fila);

    if (coincidencia.destino() != null) {
      aplicar(coincidencia.destino().persona, fila);
      tanda.indexar(coincidencia.destino(), fila);
      avance.actualizados++;
      return;
    }

    Persona existente = coincidencia.persona();
    Donante donante = repositorioDonantes.buscarPorPersona(existente).orElse(null);
    Destino destino = new Destino(existente, false, donante);
    if (donante == null || !repositorioUsuarios.existeParaDonante(donante)) {
      obtenerCuenta(destino, fila.email());
    }
    aplicar(existente, fila);
    tanda.agregar(destino, fila);
    avance.actualizados++;
    avance.contarCuenta(destino);
  }
  */

    private void prepararFila(FilaDonanteCsv fila, 
                              Tanda tanda, 
                              Avance avance, 
                              IndiceExistentes indice
                              ) {
    Coincidencia coincidencia = buscarCoincidencia(fila, tanda, indice);

    if (coincidencia == null) {
      Destino destino = new Destino(PersonaFactory.crear(fila), true, null);
      obtenerCuenta(destino, fila.email());
      tanda.agregar(destino, fila);
      avance.creados++;
      avance.contarCuenta(destino);
      return;
    }

    validar(coincidencia, fila);

    if (coincidencia.destino() != null) {
      aplicar(coincidencia.destino().persona, fila);
      tanda.indexar(coincidencia.destino(), fila);
      avance.actualizados++;
      return;
    }

    Persona existente = coincidencia.persona();
    Donante donante = repositorioDonantes.buscarPorPersona(existente).orElse(null);
    Destino destino = new Destino(existente, false, donante);
    if (donante == null || !repositorioUsuarios.existeParaDonante(donante)) {
      obtenerCuenta(destino, fila.email());
    }
    aplicar(existente, fila);
    tanda.agregar(destino, fila);
    avance.actualizados++;
    avance.contarCuenta(destino);
  }

/*
  private Coincidencia buscarCoincidencia(FilaDonanteCsv fila, Tanda tanda) {
    Destino enTanda = tanda.porEmail.get(fila.email());
    if (enTanda != null) {
      return new Coincidencia(enTanda.persona, enTanda, true);
    }
    Optional<Persona> porEmail = repositorioPersonas.buscarPorEmail(fila.email());
    if (porEmail.isPresent()) {
      return new Coincidencia(porEmail.get(), tanda.porPersona.get(porEmail.get()), true);
    }
    enTanda = tanda.porDocumento.get(fila.documentoNormalizado());
    if (enTanda != null) {
      return new Coincidencia(enTanda.persona, enTanda, false);
    }
    return repositorioPersonas.buscarPorDocumento(fila.documento())
        .map(persona -> new Coincidencia(persona, tanda.porPersona.get(persona), false))
        .orElse(null);
  }
*/

  private Coincidencia buscarCoincidencia(
          FilaDonanteCsv fila,
          Tanda tanda,
          IndiceExistentes indice) {

      Destino enTanda = tanda.porEmail.get(fila.email());

      if (enTanda != null) {
          return new Coincidencia(enTanda.persona, enTanda, true);
      }

      Persona porEmail = indice.porEmail().get(fila.email());

      if (porEmail != null) {
          return new Coincidencia(
              porEmail,
              tanda.porPersona.get(porEmail),
              true
          );
      }

      enTanda = tanda.porDocumento.get(fila.documentoNormalizado());

      if (enTanda != null) {
          return new Coincidencia(enTanda.persona, enTanda, false);
      }

      Persona porDocumento = indice.porDocumento().get(fila.documento());

      if (porDocumento != null) {
          return new Coincidencia(
              porDocumento,
              tanda.porPersona.get(porDocumento),
              false
          );
      }

      PersonaJuridica porCuit = indice.porCuit().get(
          PersonaJuridica.normalizarCuit(fila.documento())
      );

      if (porCuit != null) {
          return new Coincidencia(
              porCuit,
              tanda.porPersona.get(porCuit),
              false
          );
      }

      return null;
  }

  private void validar(Coincidencia coincidencia, FilaDonanteCsv fila) {
    Persona persona = coincidencia.persona();
    if (coincidencia.porEmail() && !fila.documentoNormalizado().equals(persona.getDocumento())) {
      throw new ConflictoDeNegocioException(
          "El email " + fila.email() + " ya está registrado para otro documento.");
    }
    boolean mismoTipo = fila.tipoPersona() == FilaDonanteCsv.TipoPersona.HUMANA
        ? persona instanceof PersonaHumana
        : persona instanceof PersonaJuridica;
    if (!mismoTipo) {
      throw new ConflictoDeNegocioException(
          "La persona con documento " + fila.documento()
              + " ya existe y no es de tipo " + fila.tipoPersona() + ".");
    }
  }

  // La regla (vincular si existe, crear si no) vive en UsuariosService y la comparten
  // las altas del admin. Solo las cuentas creadas aca se compensan si falla la tanda.
  private void obtenerCuenta(Destino destino, String email) {
    UsuariosService.CuentaObtenida cuenta =
        usuariosService.obtenerCuenta(email, RolDeAcceso.DONANTE, enviarMails);
    destino.keycloakId = cuenta.keycloakId();
    destino.cuentaCreada = cuenta.creada();
    destino.cuentaVinculada = !cuenta.creada();
  }

  private static void aplicar(Persona persona, FilaDonanteCsv fila) {
    if (persona instanceof PersonaHumana humana) {
      PersonaFactory.NombreYApellido nombre = PersonaFactory.separarNombre(fila.nombre());
      humana.actualizarNombreValidado(nombre.nombre(), nombre.apellido());
    } else if (persona instanceof PersonaJuridica juridica) {
      juridica.actualizarRazonSocialValidada(fila.nombre());
    }
    if (fila.tieneTelefono()) {
      persona.reemplazarContacto(TipoContacto.TELEFONO, fila.telefono());
    }
    if (!persona.tieneContacto(TipoContacto.EMAIL, fila.email())) {
      persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, fila.email()));
    }
  }

  private void persistir(Destino destino) {
    if (destino.nueva) {
      repositorioPersonas.guardar(destino.persona);
    }
    if (destino.donante == null) {
      destino.donante = new Donante(destino.persona);
      repositorioDonantes.guardar(destino.donante);
    }
    if (destino.keycloakId != null) {
      usuariosService.vincular(destino.keycloakId, destino.donante);
    }
  }

  private void registrarAvance(long id, Avance avance) {
    List<ErrorDeImportacion> errores = new ArrayList<>(avance.errores);
    errores.sort(Comparator.comparingInt(ErrorDeImportacion::getNumeroFila));
    withTransaction(() -> importacion(id).registrarAvance(
        avance.filasLeidas,
        avance.creados,
        avance.actualizados,
        avance.cuentasCreadas,
        avance.cuentasVinculadas,
        errores));
    entityManager().clear();
  }

  private void registrarFallo(long id, String motivo) {
    try {
      withTransaction(() -> importacion(id).fallar(motivo));
    } catch (RuntimeException e) {
      LOG.warn("No se pudo marcar como fallida la importacion {}: {}", id, mensajeDe(e));
    }
  }

  private ImportacionCsv importacion(long id) {
    return repositorioImportaciones.buscarPorId(id)
        .orElseThrow(() -> new RecursoInexistenteException(
            "No existe la importacion con id " + id));
  }

  private static Path copiarATemporal(InputStream contenido) {
    try (InputStream entrada = contenido) {
      Path temporal = Files.createTempFile("importacion-donantes-", ".csv");
      Files.copy(entrada, temporal, StandardCopyOption.REPLACE_EXISTING);
      return temporal;
    } catch (IOException e) {
      throw new ImportacionCsvException("No se pudo guardar el archivo recibido.", e);
    }
  }

  private static void borrar(Path archivo) {
    try {
      Files.deleteIfExists(archivo);
    } catch (IOException e) {
      LOG.warn("No se pudo borrar el archivo temporal {}: {}", archivo, e.getMessage());
    }
  }

  private static String mensajeDe(Throwable e) {
    Throwable raiz = e;
    while (raiz.getCause() != null && raiz.getCause() != raiz) {
      raiz = raiz.getCause();
    }
    String mensaje = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    if (raiz != e && raiz.getMessage() != null && !mensaje.contains(raiz.getMessage())) {
      return mensaje + " (" + raiz.getMessage() + ")";
    }
    return mensaje;
  }

  private record Coincidencia(Persona persona, Destino destino, boolean porEmail) {
  }

  private record IndiceExistentes(
    Map<String, Persona> porEmail,
    Map<String, PersonaHumana> porDocumento,
    Map<String, PersonaJuridica> porCuit
  ) {
  }

  private static final class Destino {
    private final Persona persona;
    private final boolean nueva;
    private Donante donante;
    private String keycloakId;
    private boolean cuentaCreada;
    private boolean cuentaVinculada;

    private Destino(Persona persona, boolean nueva, Donante donante) {
      this.persona = persona;
      this.nueva = nueva;
      this.donante = donante;
    }
  }

  private static final class Tanda {
    private final List<Destino> destinos = new ArrayList<>();
    private final Map<String, Destino> porEmail = new HashMap<>();
    private final Map<String, Destino> porDocumento = new HashMap<>();
    private final Map<Persona, Destino> porPersona = new IdentityHashMap<>();

    private void agregar(Destino destino, FilaDonanteCsv fila) {
      destinos.add(destino);
      porPersona.put(destino.persona, destino);
      indexar(destino, fila);
    }

    private void indexar(Destino destino, FilaDonanteCsv fila) {
      porEmail.put(fila.email(), destino);
      porDocumento.put(destino.persona.getDocumento(), destino);
    }
  }

  private static final class Avance {
    private int filasLeidas;
    private int creados;
    private int actualizados;
    private int cuentasCreadas;
    private int cuentasVinculadas;
    private final List<ErrorDeImportacion> errores = new ArrayList<>();

    private void error(int numeroFila, String mensaje) {
      errores.add(new ErrorDeImportacion(numeroFila, mensaje));
    }

    private void contarCuenta(Destino destino) {
      if (destino.cuentaCreada) {
        cuentasCreadas++;
      }
      if (destino.cuentaVinculada) {
        cuentasVinculadas++;
      }
    }

    private void sumar(Avance otro) {
      filasLeidas += otro.filasLeidas;
      creados += otro.creados;
      actualizados += otro.actualizados;
      cuentasCreadas += otro.cuentasCreadas;
      cuentasVinculadas += otro.cuentasVinculadas;
      errores.addAll(otro.errores);
    }
  }
}
