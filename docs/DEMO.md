# Datos de demostración

`seed.sh` deja las bases de Donaciones y Logística con un juego de datos conocido. Se corre
**a mano**: nada lo ejecuta al arrancar.

## 1. Cuentas que tienen que existir en Keycloak

En el realm `donatrack`, creadas a mano, con **nombre y apellido cargados** y una
**contraseña no temporal**:

| Mail                      | Rol           |
|---------------------------|---------------|
| `admin@gmail.com`         | ADMINISTRADOR |
| `donante1@gmail.com`      | DONANTE       |
| `donante2@gmail.com`      | DONANTE       |
| `beneficiaria1@gmail.com` | BENEFICIARIA  |
| `beneficiaria2@gmail.com` | BENEFICIARIA  |
| `chofer1@gmail.com`       | CHOFER        |

Cada cuenta tiene que tener un solo perfil: Donaciones rechaza, por ejemplo, un token con
DONANTE y ADMINISTRADOR a la vez. La lista y los datos de cada perfil (DNI, CUIT,
dirección) están en `seed/cuentas-demo.json`. No hay IDs fijos: el `keycloak_id` se busca por
mail en cada ejecución.

Antes de borrar nada, el reset verifica que las 6 cuentas existan, estén habilitadas y
tengan su rol. Si falta alguna, corta con un error y no toca las bases.

## 2. Cómo correrlo

Hace falta Docker (con `docker compose`) y que el `.env` tenga `DONACIONES_CLIENT_SECRET` y
`LOGISTICA_CLIENT_SECRET`, igual que para levantar el sistema. La contraseña del admin va
por `SEED_ADMIN_PASSWORD`, exportada o en el `.env` (que no se versiona). Nunca la escribas
en el repo.

Local:

```bash
SEED_ADMIN_PASSWORD='...' ./seed.sh
```

En el servidor (detecta `compose.oracle.yaml` si está junto a `compose.yaml`, o se le pasan
los `-f`):

```bash
SEED_ADMIN_PASSWORD='...' ./seed.sh -f compose.yaml -f compose.oracle.yaml
```

Pide escribir `BORRAR` para continuar. Con `--yes` no pregunta.

Pasos que hace:
1. Levanta `db` y `keycloak` y verifica las cuentas.
2. Detiene `donaciones` y `logistica`.
3. Borra y recrea las bases y vacía las fotos.
4. Vuelve a levantar `donaciones` y `logistica`. Hibernate crea las tablas y Donaciones siembra
   el catálogo de subcategorías; los repositorios en memoria, como las propuestas de
   asignación, arrancan vacíos.
5. Carga los datos.

La carga corre en un contenedor `python:3.12-alpine` conectado a la red de compose, así
que funciona igual en local y en el servidor sin instalar nada.

## 3. Qué borra y qué no

- **Borra:** las bases `donatrack_donaciones` y `donatrack_logistica` y el volumen de fotos de
  entregas de Logística.
- **No borra:** la base de Keycloak. No se borra ni se modifica ninguna cuenta.

Las cuentas `@demo.donatrack.ar`, que son los 40 donantes de `seed/donantes-demo.csv` y 3
beneficiarias extra, **se crean una sola vez**: la primera vez que corre el reset. En los
resets siguientes, la importación CSV y las altas encuentran la cuenta por mail y la
**vinculan**, así que Keycloak no crece. No tienen contraseña: si alguien quiere entrar con
una, usa "Olvidé mi contraseña".

## 4. Qué se carga

Todo se carga por las APIs HTTP de los servicios, operando como `admin@gmail.com`; no hay
SQL de datos de negocio. Las fechas se calculan al momento de correr la seed. Las
notificaciones salen de los propios flujos: asignación, inicio de ruta y entrega.

- **Donantes:**
  - `donante1` es una persona humana y `donante2` una persona jurídica.
  - Los otros 40 donantes vienen de la importación CSV.
- **Beneficiarias:**
  - `beneficiaria1` es el Comedor Los Gorriones y `beneficiaria2` la Escuela Rural N° 14.
  - Las 3 extra son `@demo.donatrack.ar`.
  - Cada una tiene sus necesidades.
- **Flota:**
  - 4 camiones.
  - El chofer `chofer1`, vinculado a su cuenta.
- **Unas 94 donaciones**, repartidas en todos los estados:
  - pendiente de recepción;
  - en depósito;
  - pendiente de asignación, con propuestas para que el admin elija;
  - asignada;
  - lista para entregar;
  - en traslado;
  - entregada con fotos;
  - vencida.
- **3 rutas**, armadas con las acciones reales: recepción en depósito, ejecución de la
  asignación, confirmación del destinatario, corrida de planificación, inicio de ruta y
  confirmación de la entrega.
  - **Ruta 1, finalizada:** sus entregas se confirmaron con fotos.
  - **Ruta 2, en curso:** tiene posiciones GPS.
  - **Ruta 3, planificada para hoy y sin iniciar.**

El planificador simulado de Logística no responde por sí mismo. La seed contesta el
callback `POST /planificacion/callback` como lo haría el proveedor externo. Por eso
Logística tiene que correr con `LOGISTICA_PLANIFICADOR=simulado`, que es lo que usa
`compose.yaml`.

## 5. Qué ve cada cuenta

| Cuenta | Qué ve |
|--------|--------|
| `admin@gmail.com` | Panel con los conteos reales. Depósito con pendientes de recepción, en depósito y vencidas. Asignaciones con propuestas para elegir destinataria. 4 camiones. Monitoreo con la ruta en curso en el mapa. Rutas planificadas con las 3 rutas. En Importar CSV, la importación de los 40 donantes. |
| `donante1@gmail.com` | Mis donaciones en todos los estados: pendiente, en depósito, en propuesta, asignada, en traslado, entregada y vencida. Seguimiento con su donación en traslado en el mapa. Notificaciones de asignación, inicio de ruta y entrega. |
| `donante2@gmail.com` | Lo mismo, como persona jurídica. Además tiene una donación lista para entregar en la ruta 3. |
| `beneficiaria1@gmail.com` | Sus necesidades y sus donaciones asignadas. Seguimiento con la ruta en curso hacia ella. En Confirmar recepción, las entregas en traslado que todavía no confirmó. Una es la de `donante1`; otra viene de un donante del CSV. Notificaciones. |
| `beneficiaria2@gmail.com` | Lo mismo, más una entrega en la ruta 3, todavía sin iniciar. |
| `chofer1@gmail.com` | Mis rutas: la ruta 2 en curso y la ruta 3 planificada, que puede iniciar. |
| Sin sesión | La landing y Donaciones entregadas muestran las entregas confirmadas con sus fotos. |

"Entregada pendiente de confirmar recepción" es, en el sistema, una entrega **en traslado**:
la beneficiaria solo puede confirmar la recepción, con fotos, mientras la entrega está en
camino.
