# Integracion con Keycloak

Donaciones delega la autenticacion y los roles en Keycloak. Este documento junta la configuracion
que hay que dejar lista antes de arrancar los microservicios.

## 1. Keycloak en Docker

Levantar Keycloak en el puerto 8180 (para dejar libre el 8080 de Donaciones):

```bash
docker run -d --name donatrack-keycloak \
  -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:24.0 start-dev
```

Consola de administracion: http://localhost:8180 (usuario `admin`, contrasena `admin`).

## 2. Realm `donatrack`

- Crear un realm nuevo llamado **`donatrack`**.
- En *Realm settings > Login*, activar **Email as username** (asi el `username` en Keycloak
  queda igual al `email` que mandan los microservicios).
- En *Realm settings > Email*, cargar el SMTP. Hace falta para las altas que Donaciones
  hace **sin contrasena**: los donantes cargados en el deposito (`POST /donantes`) y las
  beneficiarias dadas de alta por un administrador (`POST /entidades/*`). En esos casos
  Keycloak le manda al usuario el correo de `UPDATE_PASSWORD` para que la configure.
  Los administradores (`POST /administradores`) reciben la contrasena directamente de
  quien los da de alta, asi que para ese flujo el SMTP no es necesario.
  Para desarrollo sirve [MailHog](https://github.com/mailhog/MailHog) en `localhost:1025`.

## 3. Roles de realm

Crear estos **5 roles de realm** (Realm roles > Create role). Los nombres tienen que
coincidir exactamente con el enum `RolDeAcceso`:

| Nombre | Lo tiene... |
|---|---|
| `DONANTE` | personas y organizaciones que donan |
| `BENEFICIARIA` | entidades receptoras |
| `ADMINISTRADOR` | staff interno |
| `CHOFER` | chofer de Logistica (lo vincula el modulo Logistica) |
| `SERVICIO_LOGISTICA` | service account del client `logistica-backend` |

## 4. Clients

### 4.1 `donaciones-backend` (service account para la Admin REST API)

- *Client type*: OpenID Connect.
- *Client authentication*: **ON** (confidencial).
- *Service accounts roles*: **ON**. (El resto de los flows se pueden desactivar.)
- En *Service account roles* asignar los roles **de client** `realm-management > manage-users`
  y `realm-management > view-realm`. Son los que Donaciones necesita para crear usuarios,
  asignarles roles, mandar el mail de UPDATE_PASSWORD y borrarlos en el rollback.
- En *Credentials* copiar el **Client secret**: va en la variable `KEYCLOAK_CLIENT_SECRET`
  de Donaciones.

### 4.2 `logistica-backend` (service account para llamar a Donaciones)

- *Client type*: OpenID Connect.
- *Client authentication*: **ON**.
- *Service accounts roles*: **ON**.
- En *Service account roles* asignar el **rol de realm** `SERVICIO_LOGISTICA`.
- En *Credentials* copiar el **Client secret**: va en `KEYCLOAK_CLIENT_SECRET` de Logistica.

### 4.3 `donatrack-web` (login del front)

- *Client type*: OpenID Connect.
- *Client authentication*: **OFF** (es el front, es publico).
- Habilitar **Standard flow** (authorization code).
- *Valid redirect URIs*: la URL del web (por ej. `http://localhost:4200/*`).
- *Web origins*: la misma URL (`http://localhost:4200`).
- No pide secret.

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

Las mismas 5 variables. `KEYCLOAK_CLIENT_ID` apunta al client `logistica-backend` y
`KEYCLOAK_CLIENT_SECRET` es el secret de **ese** client (distinto del de Donaciones).
Solo se usan cuando `LOGISTICA_MODO=http`; con el cliente simulado no hace falta.

### Web

Las variables tipicas de una SPA con Keycloak: `KEYCLOAK_URL`, `KEYCLOAK_REALM` y
`KEYCLOAK_CLIENT_ID=donatrack-web`.

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
#    En produccion el web lo saca con Authorization Code.
TOKEN=$(curl -s -X POST \
  "http://localhost:8180/realms/donatrack/protocol/openid-connect/token" \
  -d "grant_type=password" \
  -d "client_id=donatrack-web" \
  -d "username=admin@donatrack.ar" \
  -d "password=mi-password" | jq -r .access_token)

# 2. Llamar a Donaciones con el token.
curl -s http://localhost:8080/usuarios/yo \
  -H "Authorization: Bearer $TOKEN" | jq
```

Respuesta esperada:

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

Si el token esta vencido, mal firmado o sin `Bearer`, Donaciones responde 401 con el
`ErrorDTO` estandar. Si el rol del token no cubre la ruta, 403.

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
