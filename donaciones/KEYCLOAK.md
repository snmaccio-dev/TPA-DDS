# Integracion con Keycloak

DonaTrack delega la autenticacion y los roles en Keycloak. Los tres modulos lo usan:

- **Donaciones** valida el token de cada request y usa la Admin REST API para crear usuarios.
- **Logistica** valida el token de cada request y pide un token de servicio para llamar a Donaciones.
- **Web** hace el login contra Keycloak y propaga el token del usuario a los dos backends.

Este documento tiene dos caminos: importar el realm ya armado (seccion 2) o configurarlo a mano
(seccion 3). El primero es el recomendado.

## 1. Levantar Keycloak

La version es la **24.0**. Hay dos formas, las dos escuchan en el puerto 8180 para dejar libre
el 8080 de Donaciones.

### 1.1 Con Docker

```bash
docker run -d --name donatrack-keycloak \
  -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:24.0 start-dev
```

### 1.2 Sin Docker, con el ZIP

Sirve si no tenes Docker instalado. Solo hace falta Java 17, que ya es requisito del proyecto.

1. Bajar `keycloak-24.0.5.zip` de https://github.com/keycloak/keycloak/releases
2. Descomprimir de modo que exista `C:\keycloak\bin\kc.bat`. **La ruta no puede tener espacios.**
3. Levantarlo:

```powershell
$env:KEYCLOAK_ADMIN = "admin"; $env:KEYCLOAK_ADMIN_PASSWORD = "admin"
C:\keycloak\bin\kc.bat start-dev --http-port=8180
```

Las comillas en las dos variables son necesarias: sin ellas PowerShell interpreta `admin`
como un comando y falla.

Consola de administracion: http://localhost:8180 (usuario `admin`, contrasena `admin`).

## 2. Importar el realm (camino recomendado)

El repo trae el realm exportado en **`keycloak/donatrack-realm.json`**, con los cinco roles,
los tres clients y los service accounts con sus permisos ya asignados.

### 2.1 Con el ZIP

Copiar el archivo a `C:\keycloak\data\import\` y arrancar con:

```powershell
C:\keycloak\bin\kc.bat start-dev --http-port=8180 --import-realm
```

### 2.2 Con Docker

Montar el archivo en `/opt/keycloak/data/import/` y agregar `--import-realm` al comando.

### 2.3 Despues del import, si o si

**Los client secrets del export estan enmascarados** con el texto `CAMBIAR-EN-CREDENTIALS`.
Cada uno tiene que generar los suyos:

- Clients > `donaciones-backend` > Credentials > **Regenerate** > copiar el valor
- Clients > `logistica-backend` > Credentials > **Regenerate** > copiar el valor

Son dos secrets distintos y van en variables distintas (ver seccion 5).

**El export no trae usuarios humanos**, solo los service accounts. Hay que crear los propios
(ver seccion 4).

**Subir la vida del access token.** Realm settings > Sessions > *Access Token Lifespan* a
**30 minutos**. Por defecto son 5, y el cliente web todavia no implementa refresh token, asi
que la sesion se cae enseguida.

## 3. Configurar el realm a mano

Solo hace falta si no se usa el export. Si lo importaste, salteate esta seccion entera.

- Crear un realm llamado **`donatrack`**.
- En *Realm settings > Login*, activar **Email as username** (asi el `username` en Keycloak
  queda igual al `email` que mandan los microservicios).
- En *Realm settings > Email*, cargar el SMTP. Hace falta para las altas que Donaciones
  hace **sin contrasena**: los donantes cargados en el deposito (`POST /donantes`) y las
  beneficiarias dadas de alta por un administrador (`POST /entidades/*`). En esos casos
  Keycloak le manda al usuario el correo de `UPDATE_PASSWORD` para que la configure.
  Los administradores (`POST /administradores`) reciben la contrasena directamente de
  quien los da de alta, asi que para ese flujo el SMTP no es necesario.
  Para desarrollo sirve [MailHog](https://github.com/mailhog/MailHog) en `localhost:1025`.

### 3.1 Roles de realm

Crear estos **5 roles** (Realm roles > Create role). Los nombres tienen que coincidir
exactamente, en mayusculas, con el enum `RolDeAcceso` de los dos backends:

| Nombre | Lo tiene... |
|---|---|
| `DONANTE` | personas y organizaciones que donan |
| `BENEFICIARIA` | entidades receptoras |
| `ADMINISTRADOR` | staff interno |
| `CHOFER` | chofer de Logistica |
| `SERVICIO_LOGISTICA` | service account del client `logistica-backend` |

`PUBLICO` **no** se crea: existe solo en el enum de cada modulo, para marcar las rutas
abiertas, y nunca viaja en un token.

### 3.2 `donaciones-backend` (service account para la Admin REST API)

- *Client authentication*: **ON**. *Service accounts roles*: **ON**. El resto de los flows, OFF.
- En *Service accounts roles > Assign role*, cambiar el filtro a **Filter by clients** y asignar
  `realm-management > manage-users` y `realm-management > view-realm`. Son los que Donaciones
  necesita para crear usuarios, asignarles roles, mandar el mail de UPDATE_PASSWORD y borrarlos
  en el rollback.
- En *Credentials* copiar el **Client secret**.

### 3.3 `logistica-backend` (service account para llamar a Donaciones)

- *Client authentication*: **ON**. *Service accounts roles*: **ON**.
- En *Service accounts roles > Assign role*, con el filtro en **Filter by realm roles**, asignar
  `SERVICIO_LOGISTICA`.
- En *Credentials* copiar el **Client secret**.

### 3.4 `donatrack-web` (login del front)

- *Client authentication*: **OFF**. Es el front y es publico; usa PKCE en lugar de secret.
- Habilitar **Standard flow** (authorization code).
- *Valid redirect URIs*: `http://localhost:8082/*`
- *Valid post logout redirect URIs*: `http://localhost:8082/*`
- *Web origins*: `http://localhost:8082`

El 8082 es el puerto del modulo `web`, que es una app Javalin server-rendered y no una SPA.

## 4. Usuarios de prueba

El **administrador** se crea a mano en la consola: ver la seccion 6.

Los **choferes** tambien se crean en la consola, con el rol `CHOFER`, y despues hay que
vincular la cuenta con el chofer de Logistica pasandole el `keycloakId` del usuario (el `sub`
del token) en `POST /choferes`. Sin esa vinculacion, `GET /rutas/mias` responde que la cuenta
no esta asociada a ningun chofer.

**Los donantes y las entidades beneficiarias NO se crean en la consola.** El endpoint
`GET /usuarios/yo` saca el `donanteId` y el `beneficiariaId` de la tabla `usuario` de
Donaciones, asi que una cuenta que exista solo en Keycloak se autentica pero queda sin perfil,
y los chequeos de propiedad le responden 403.

Esos se dan de alta con `POST /registro/donantes` y `POST /registro/beneficiarias`, que crean
la cuenta en Keycloak **y** la fila local en la misma operacion.

## 5. Variables de entorno

### Donaciones (`DonacionesApp`)

| Variable | Default | Comentario |
|---|---|---|
| `KEYCLOAK_URL` | `http://localhost:8180` | URL base donde corre Keycloak. |
| `KEYCLOAK_REALM` | `donatrack` | |
| `KEYCLOAK_CLIENT_ID` | `donaciones-backend` | |
| `KEYCLOAK_CLIENT_SECRET` | *(requerida)* | Si falta, el arranque falla con mensaje claro. |
| `KEYCLOAK_ISSUER` | *(opcional)* | Sobrescribe el `iss` esperado del JWT. Util en Docker, donde la URL interna y la publica no coinciden. |

### Logistica (`LogisticaApp`)

| Variable | Default | Comentario |
|---|---|---|
| `KEYCLOAK_URL` | `http://localhost:8180` | Usada siempre que la seguridad este activa. |
| `KEYCLOAK_REALM` | `donatrack` | Idem. |
| `KEYCLOAK_ISSUER` | *(opcional)* | Idem Donaciones. |
| `KEYCLOAK_CLIENT_ID` | `logistica-backend` | Solo con `LOGISTICA_MODO=http`. |
| `KEYCLOAK_CLIENT_SECRET` | *(requerida con `LOGISTICA_MODO=http`)* | El secret de **ese** client, distinto del de Donaciones. |
| `LOGISTICA_SEGURIDAD` | `on` | En `off` no registra el filtro y los endpoints quedan abiertos. Solo para probar en local sin Keycloak. |

El filtro de acceso usa `KEYCLOAK_URL` y `KEYCLOAK_REALM` **sin importar el modo**. El cliente
simulado evita las llamadas salientes a Donaciones, no la validacion de los tokens entrantes.

### Web (`WebApp`)

| Variable | Default | Comentario |
|---|---|---|
| `KEYCLOAK_URL` | `http://localhost:8180` | |
| `KEYCLOAK_REALM` | `donatrack` | |
| `KEYCLOAK_CLIENT_ID` | `donatrack-web` | Client publico, no lleva secret. |
| `WEB_URL_BASE` | `http://localhost:8082` | Con esto arma el redirect URI. Tiene que coincidir con lo configurado en el client. |
| `WEB_SEGURIDAD` | `on` | En `off` no exige sesion. Solo para probar en local sin Keycloak. |

Las dos variables de seguridad tienen default `on` a proposito: si esto se despliega sin setear
nada, arranca cerrado.

## 6. Primer administrador

`POST /administradores` esta restringido al rol `ADMINISTRADOR`, asi que el primer
administrador se crea a mano en la consola de Keycloak:

1. *Users > Add user*: el `Username` es el email (el realm tiene *Email as username* en ON).
2. *Credentials > Set password*: cargar la contrasena con **Temporary: OFF**. Si queda en
   ON, Keycloak marca `UPDATE_PASSWORD` como required action y el login del web falla con
   "Account is not fully set up".
3. *Role mapping > Assign role*: asignar el rol de realm `ADMINISTRADOR`.

Desde ese usuario ya se puede llamar a `POST /administradores` para dar de alta mas.

## 7. Cambio de contrasena

Cada usuario puede cambiar su contrasena en:

```
{KEYCLOAK_URL}/realms/donatrack/account
```

El web puede poner un link "Cambiar contrasena" que lleve ahi. No hace falta un endpoint
propio en Donaciones: las credenciales las administra Keycloak.

## 8. Prueba rapida con curl

```bash
# 1. Pedir un token para un usuario real (Resource Owner Password Grant, solo para pruebas).
#    En produccion el web lo saca con Authorization Code + PKCE.
TOKEN=$(curl -s -X POST \
  "http://localhost:8180/realms/donatrack/protocol/openid-connect/token" \
  -d "grant_type=password" \
  -d "client_id=donatrack-web" \
  -d "username=admin@donatrack.ar" \
  -d "password=mi-password" | jq -r .access_token)

# 2. Llamar a Donaciones con el token.
curl -s http://localhost:8080/usuarios/yo \
  -H "Authorization: Bearer $TOKEN" | jq

# 3. Y a Logistica, que valida el mismo token contra el mismo realm.
curl -s http://localhost:8081/camiones \
  -H "Authorization: Bearer $TOKEN" | jq
```

Respuesta esperada del paso 2:

```json
{
  "keycloakId": "f3c6...",
  "email": "admin@donatrack.ar",
  "roles": ["ADMINISTRADOR"],
  "donanteId": null,
  "beneficiariaId": null,
  "nombre": "admin@donatrack.ar"
}
```

Si el token esta vencido, mal firmado o sin `Bearer`, los dos backends responden 401 con el
`ErrorDTO` estandar. Si el rol del token no cubre la ruta, 403.

El password grant de este ejemplo **no funciona con cuentas creadas por un administrador**
mientras tengan pendiente la accion `UPDATE_PASSWORD`: Keycloak las rechaza con "Account is not
fully set up". Es esperado, y es la razon por la que el cliente web usa Authorization Code y no
un formulario propio.

## 9. Dar de alta otro administrador por API

Con el token de un administrador ya existente:

```bash
curl -s -X POST http://localhost:8080/administradores \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email":"nuevo-admin@donatrack.ar","contrasena":"una-contrasena-larga"}'
```

Respuesta esperada (201):

```json
{"keycloakId":"f3c6..."}
```

Casos de error:

- `400` si falta el email o la contrasena tiene menos de 8 caracteres.
- `409` si ya existe una cuenta con ese correo en Keycloak.
- `403` si el token no tiene el rol `ADMINISTRADOR`.

## 10. Volver a exportar el realm

Si cambias algo del realm y querias dejarlo en el repo, hay que exportar con Keycloak **detenido**:

```powershell
C:\keycloak\bin\kc.bat export --dir C:\keycloak-export --realm donatrack --users realm_file
```

Y antes de commitear el resultado hay que limpiarlo, porque **el export de linea de comandos no
enmascara nada**:

- Reemplazar los dos `"secret"` de los clients por `CAMBIAR-EN-CREDENTIALS`.
- Borrar del array `users` todos los usuarios **menos** los `service-account-*`, que son los que
  llevan los permisos de los clients. Si se exporta con `--users skip` esos tambien se pierden y
  el import deja los clients sin permisos.
- Verificar que no queden hashes de contrasena en el archivo.

