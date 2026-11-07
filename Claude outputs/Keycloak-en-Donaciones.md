# Cómo maneja Keycloak el servicio de Donaciones

Keycloak se encarga de dos cosas:

1. **Autenticar cada request:** quien llama manda un JWT y Donaciones lo valida.
2. **Administrar cuentas:** Donaciones crea y borra usuarios en Keycloak usando su Admin API.

Donaciones **no guarda contraseñas ni roles**. Lo único que guarda en su base es la relación entre el id de la cuenta en Keycloak (`sub`) y el perfil local, que puede ser un donante o una beneficiaria.

---

## 0. `keycloak/donatrack-realm.json` (configuración de Keycloak)
- Realm `donatrack`. El access token dura 30 minutos (`accessTokenLifespan: 1800`).
- Roles de realm: `DONANTE`, `BENEFICIARIA`, `ADMINISTRADOR`, `CHOFER` y `SERVICIO_LOGISTICA`.
- Clientes:
  - `donatrack-web`: es público y lo usa la web para el login.
  - `donaciones-backend`: es confidencial y tiene una *service account* con `manage-users` y `view-realm`. Gracias a eso Donaciones puede crear y borrar usuarios.
  - `logistica-backend`: es confidencial y su service account tiene el rol `SERVICIO_LOGISTICA`. Con ese rol Logística llama a los endpoints de eventos de Donaciones.

## 1. Arranque: `DonacionesApp.java`
- Lee las variables de entorno `KEYCLOAK_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_CLIENT_SECRET` y `KEYCLOAK_ISSUER` (esta última es opcional).
  - **Si falta `KEYCLOAK_CLIENT_SECRET`, el servicio no arranca** y tira `IllegalStateException`.
  - `KEYCLOAK_ISSUER` existe para Docker: ahí la URL interna (`keycloak:8080`) no coincide con el `iss` del token (`localhost:8180`).
- Crea tres objetos:
  - `ProveedorDeIdentidadKeycloak`, para crear y borrar cuentas.
  - `VerificadorDeTokens`, para validar los JWT.
  - `FiltroDeAcceso`, que usa el verificador y `RepositorioUsuarios`.
- Le pasa el proveedor a `GestorRegistro`, `GestorPersonas`, `GestorEntidadesBeneficiarias` y `AdministradoresController`.
- Registra el filtro con `filtro.registrarEn(app)` **antes** que las rutas.
- Mapea los errores a códigos HTTP:
  - `IntegracionConIdentidadException` → **502** (no se pudo hablar con Keycloak).
  - `UnauthorizedResponse` y `ForbiddenResponse` del filtro → **401** y **403**, con cuerpo `ErrorDTO`.
  - `ConflictoDeNegocioException`, por ejemplo un email repetido en Keycloak → **409**.

---

## 2. Qué pasa en cada request (en orden)

### 2.1 `seguridad/RolDeAcceso.java`
- Enum con `PUBLICO`, `DONANTE`, `BENEFICIARIA`, `ADMINISTRADOR`, `CHOFER` y `SERVICIO_LOGISTICA`. Implementa `RouteRole` de Javalin.
- **Cada ruta declara qué roles la pueden usar**, por ejemplo: `app.get("/donantes", ..., RolDeAcceso.ADMINISTRADOR)`.
- `desdeNombre()` traduce los nombres de rol que vienen en el token. Si llega uno que no está en el enum (`offline_access`, `default-roles-donatrack`), lo ignora.

### 2.2 `seguridad/FiltroDeAcceso.java` (corre antes de cada ruta, con `beforeMatched`)
Hace estas validaciones, en este orden:
1. Si la ruta **no declaró roles** → **403**. Así ninguna ruta queda pública por olvido.
2. Si la ruta es `PUBLICO` (solo `/registro/*`) → deja pasar sin pedir token.
3. Llama a `VerificadorDeTokens.verificar(header Authorization)`.
4. Si la cuenta tiene **más de un perfil** entre DONANTE, BENEFICIARIA y ADMINISTRADOR → **403**. Cada perfil necesita su propia cuenta.
5. Si **ninguno de los roles del token** está entre los roles de la ruta → **403**.
6. Busca la fila local con `RepositorioUsuarios.buscarPorKeycloakId(sub)` y arma un `UsuarioActual`, que guarda en `ctx.attribute("usuarioActual")`.
- Los handlers lo leen con `FiltroDeAcceso.usuarioDe(ctx)`. Si no está → 403.

### 2.3 `seguridad/VerificadorDeTokens.java`
- Valida contra el JWKS de Keycloak (`{url}/realms/{realm}/protocol/openid-connect/certs`). Usa la librería Nimbus.
- Validaciones:
  - El header tiene que empezar con `Bearer ` y el token no puede estar vacío → si falla, **401**.
  - **La firma tiene que ser RS256** con una clave del JWKS.
  - El **`iss`** tiene que ser el issuer esperado.
  - Tienen que estar los claims obligatorios `sub`, `exp` e `iss`.
  - **`exp`** no puede estar vencido → si lo está, **401** con "token vencido".
  - **No valida `aud`**, porque Keycloak no lo completa por defecto.
- Del token saca el `sub` (keycloakId), el `email` y los roles de `realm_access.roles`, y devuelve un `TokenVerificado`.
- Cualquier otra falla → **401** con "Token invalido".
- Tiene un segundo constructor para los tests, que recibe un `JWKSource` y no hace llamadas de red.

### 2.4 `domain/usuario/Usuario.java` y `repository/RepositorioUsuarios.java`
- Entidad `usuario`. Su PK es `keycloak_id` y tiene dos `@OneToOne`, a `donante` y a `beneficiaria`, los dos `unique`. Así un perfil tiene como máximo una cuenta.
- Se crea con `Usuario.deDonante(id, donante)` o `Usuario.deBeneficiaria(id, beneficiaria)`. Valida que el id no esté vacío y que el perfil no sea null.
- **Los administradores y los choferes no tienen fila local:** pasan el filtro solo por el rol que trae el token.

### 2.5 `seguridad/UsuarioActual.java`
- Es un record con `keycloakId`, `email`, `roles`, `Optional<Donante>` y `Optional<Beneficiaria>`.
- Tiene los helpers `esAdministrador()`, `esDonante()`, `esBeneficiaria()` y `esServicioLogistica()`.

### 2.6 `seguridad/Acceso.java` (chequeos de propiedad, después del filtro)
El filtro solo controla los roles. Esta clase controla que **el recurso sea del usuario** y, si no lo es, responde **403**.
- `exigirPuedeVerDonacion`:
  - Admin y Logística pueden ver todas.
  - Un donante solo ve las suyas.
  - Una beneficiaria solo ve las que tiene asignadas.
- `exigirPuedeVerNotificacion`:
  - Las notificaciones de administradores las ve cualquier admin.
  - Las demás las ve solo el dueño del perfil.
- `exigirPuedeVerNecesidad`: el admin ve todas y la beneficiaria solo las suyas.
- `perfilDonanteObligatorio` y `perfilBeneficiariaObligatorio`: si la cuenta tiene el rol pero no tiene perfil local → 403.

### 2.7 Controllers (usan los roles, `UsuarioActual` y `Acceso`)
- **`DonacionesController`**
  - `GET /donaciones` filtra según quién llama: admin y Logística ven todo, un donante solo lo suyo y una beneficiaria solo lo suyo.
  - `GET /donaciones/{id}`, `/comprobante` e `/historial` llaman a `exigirPuedeVerDonacion`.
  - Los endpoints `/donaciones/eventos/*` son solo para `SERVICIO_LOGISTICA` (Logística llama con el token de su service account).
- **`NecesidadesController`**
  - Una beneficiaria crea necesidades para su propia entidad, que sale del token. Un admin tiene que indicar el CUIT.
  - Ver, editar y borrar pasan por `exigirPuedeVerNecesidad`.
- **`NotificacionesController`** y **`service/ConsultaDeNotificaciones`**
  - `/mias` decide qué notificaciones devolver según el `UsuarioActual`. Si es admin, devuelve las de administradores.
  - Marcar una como leída pasa por `exigirPuedeVerNotificacion`.
- **`UsuariosController`**: `GET /usuarios/yo` devuelve un `UsuarioActualDTO` con id, email, roles, ids de perfil y nombre. La web lo usa para saber quién está logueado.
- **`RegistroController`**: `/registro/donantes` y `/registro/beneficiarias` son `PUBLICO` y no piden token.
- **Solo `ADMINISTRADOR`:** `AdministradoresController`, `DonantesController`, `AsignacionesController` y las altas y bajas de `EntidadesBeneficiariasController`.

---

## 3. Creación y borrado de cuentas en Keycloak

### 3.1 `seguridad/ProveedorDeIdentidad.java` (interfaz, patrón Adapter/Puerto)
- Define tres operaciones: `crearUsuario(email, contraseña, rol)`, `crearUsuarioSinContrasena(email, rol)` y `eliminarUsuario(id)`.
- Los services dependen de esta interfaz, no de Keycloak directamente.

### 3.2 `infrastructure/identidad/ProveedorDeIdentidadKeycloak.java` (implementación)
- **Token de servicio:** pide un token por `client_credentials` con el client id y el secret, y lo guarda en caché hasta 30 segundos antes de que venza. Se renueva en un método `synchronized`.
- **Alta** (`POST /admin/realms/{realm}/users`):
  - Envía username y email (son el mismo valor), `enabled` y, si corresponde, una contraseña no temporal.
  - Si Keycloak responde 409 → `ConflictoDeNegocioException` ("Ya existe una cuenta con ese correo").
  - El id de la cuenta nueva lo saca del header `Location`.
- **Asignación de rol:** primero `GET /roles/{ROL}` y después `POST /users/{id}/role-mappings/realm`. **Si falla, borra la cuenta recién creada** como rollback.
- **Alta sin contraseña:** después de crear la cuenta, le pide a Keycloak que mande el mail `UPDATE_PASSWORD` (`PUT /execute-actions-email`). Si el mail falla, solo lo loguea y la cuenta queda creada.
- **Baja:** `DELETE /users/{id}`. Si Keycloak responde 404, lo ignora.
- Cualquier falla de red, de parseo o un 5xx → `IntegracionConIdentidadException`, que se responde como 502.
- Las contraseñas nunca se guardan ni se loguean.

### 3.3 `seguridad/PoliticaDeContrasenas.java`
- Exige un **mínimo de 8 caracteres**. Si no se cumple, lanza `DatosInvalidosException` (400) antes de llamar a Keycloak. Después Keycloak aplica su propia política.

### 3.4 `seguridad/IntegracionConIdentidadException.java`
- Representa un error al comunicarse con Keycloak y se responde con 502. Nunca incluye contraseñas en el mensaje.

### 3.5 Services que crean o borran cuentas
Todos siguen la misma secuencia: **primero crean la cuenta en Keycloak, después guardan en la base y, si eso falla, borran la cuenta en Keycloak (rollback manual)**.

- **`service/GestorRegistro`**: registro público.
  1. Valida el email, la contraseña (`PoliticaDeContrasenas`) y que la persona no exista → 409.
  2. Llama a `proveedor.crearUsuario(email, contraseña, DONANTE o BENEFICIARIA)`.
  3. En una transacción guarda la persona, el donante o la beneficiaria y el `Usuario(keycloakId, perfil)`.
  4. Si la transacción falla → `proveedor.eliminarUsuario(keycloakId)`.
- **`service/GestorPersonas`**: el admin da de alta un donante desde el depósito.
  - Usa `crearUsuarioSinContrasena`, así Keycloak le manda el mail al donante para que elija su contraseña.
  - Si la persona ya existe, llama a `asegurarCuenta`: si ya tiene usuario lo reutiliza, y si no, crea uno.
  - **Baja:** si el donante tiene donaciones → 409. Si no, en una transacción borra las notificaciones, el usuario, el donante y la persona. **Después del commit** borra la cuenta en Keycloak. Si eso falla, solo lo loguea: la cuenta queda sin perfil y el filtro la rechaza.
- **`service/GestorEntidadesBeneficiarias`**: igual que `GestorPersonas`, pero para beneficiarias. Crea la cuenta sin contraseña con el email del primer representante y la borra en Keycloak al eliminar la entidad.
- **`controller/AdministradoresController`**: `POST /administradores`, solo para admin.
  - Valida el email y la contraseña y llama a `crearUsuario(..., ADMINISTRADOR)`.
  - **No guarda nada en la base.**
  - La contraseña no es temporal: si lo fuera, Keycloak bloquearía el login con "Account is not fully set up".

---

## 4. Lo que queda pendiente (TODOs en el código)
- **`ImportadorCSVPersonas`:** el CSV crea donantes en la base, pero **no les crea cuenta en Keycloak**, así que no pueden loguearse.
- **`GestorNotificaciones.notificarAdministradores`:** solo guarda la notificación en la base. Para mandarla por mail habría que pedirle a Keycloak la lista de admins, y eso está fuera de alcance.

## Resumen del flujo de una request
```
Request con "Authorization: Bearer <JWT>"
  → FiltroDeAcceso: ¿la ruta declaró roles? ¿es PUBLICO?
  → VerificadorDeTokens: firma RS256 (JWKS), iss, exp → sub, email, roles
  → FiltroDeAcceso: ¿un solo perfil? ¿tiene un rol permitido? → busca el Usuario local → UsuarioActual
  → Controller: Acceso.exigirPuedeVer... (¿el recurso es suyo?)
  → Service
Errores: 401 (token), 403 (rol o propiedad), 502 (Keycloak caído), 409 (email repetido)
```
