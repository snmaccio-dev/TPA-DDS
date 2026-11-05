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
- En *Realm settings > Email*, cargar el SMTP. Es obligatorio porque Keycloak manda el mail
  de `UPDATE_PASSWORD` cuando los administradores dan de alta usuarios sin contrasena.
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

## 6. Prueba rapida con curl

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
