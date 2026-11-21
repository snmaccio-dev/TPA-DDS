#!/usr/bin/env bash
# Reset con datos de demostracion. Se ejecuta A MANO; nada lo llama al arrancar.
#
#   ./seed.sh                          detecta compose.yaml (+ compose.oracle.yaml si existe)
#   ./seed.sh -f compose.yaml -f compose.oracle.yaml
#   ./seed.sh --yes                    sin pedir confirmacion
#
# Borra y recrea las bases donatrack_donaciones y donatrack_logistica y vacia el volumen
# de fotos de Logistica. NO toca la base de Keycloak: las cuentas se verifican y, las que
# hagan falta, las crean las propias altas (una sola vez; despues se vinculan por mail).
# Ver docs/DEMO.md.
set -euo pipefail

cd "$(dirname "$0")"
# En Git Bash (Windows) evita que MSYS reescriba rutas como /app/fotos o /seed.
export MSYS_NO_PATHCONV=1

IMAGEN_PYTHON="${SEED_PYTHON_IMAGE:-python:3.12-alpine}"
BASES=(donatrack_donaciones donatrack_logistica)

uso() {
  sed -n '2,11p' "$0" | sed 's/^# \{0,1\}//'
}

error() {
  echo "[RESET] ERROR: $*" >&2
  exit 1
}

paso() {
  echo "[RESET] $*"
}

CONFIRMADO=0
ARCHIVOS=()
while [ $# -gt 0 ]; do
  case "$1" in
    -y|--yes) CONFIRMADO=1 ;;
    -f) [ $# -ge 2 ] || error "-f necesita un archivo."; ARCHIVOS+=(-f "$2"); shift ;;
    -h|--help) uso; exit 0 ;;
    *) error "Argumento desconocido: $1 (usa --help)." ;;
  esac
  shift
done

if [ ${#ARCHIVOS[@]} -eq 0 ]; then
  if [ -f compose.oracle.yaml ] && [ -f Caddyfile ] && grep -q '^DOMINIO=' .env 2>/dev/null; then
    ARCHIVOS=(-f compose.oracle.yaml)
  else
    ARCHIVOS=(-f compose.yaml)
  fi
fi

compose() {
  docker compose "${ARCHIVOS[@]}" "$@"
}

# Toma una variable del entorno o, si no esta, del .env (sin ejecutar el archivo).
leer_env() {
  local nombre="$1" valor=""
  if [ -n "${!nombre:-}" ]; then
    export "${nombre?}"
    return 0
  fi
  [ -f .env ] || return 0
  valor="$(grep -E "^[[:space:]]*(export[[:space:]]+)?${nombre}=" .env | tail -n 1 | cut -d= -f2- | tr -d '\r')" || true
  valor="${valor#\"}"; valor="${valor%\"}"; valor="${valor#\'}"; valor="${valor%\'}"
  if [ -n "$valor" ]; then
    export "${nombre}=${valor}"
  fi
}

for variable in SEED_ADMIN_PASSWORD DONACIONES_CLIENT_SECRET LOGISTICA_CLIENT_SECRET \
                KEYCLOAK_REALM POSTGRES_USER \
                SEED_KEYCLOAK_URL SEED_DONACIONES_URL SEED_LOGISTICA_URL SEED_WEB_URL SEED_WEB_CLIENT_ID; do
  leer_env "$variable"
done

for variable in SEED_ADMIN_PASSWORD DONACIONES_CLIENT_SECRET LOGISTICA_CLIENT_SECRET; do
  [ -n "${!variable:-}" ] || error "Falta $variable (en el .env o exportada)."
done
USUARIO_DB="${POSTGRES_USER:-postgres}"

command -v docker >/dev/null || error "No se encontro docker."

paso "Archivos de compose: ${ARCHIVOS[*]}"
cat <<AVISO
[RESET] Esto BORRA y recrea las bases ${BASES[*]}
[RESET] y vacia las fotos de entregas de Logistica. La base de Keycloak no se toca.
AVISO

if [ "$CONFIRMADO" -ne 1 ]; then
  respuesta=""
  read -r -p "[RESET] Escribi BORRAR para continuar: " respuesta || true
  [ "$respuesta" = "BORRAR" ] || error "Cancelado; no se borro nada."
fi

paso "Levantando la base y Keycloak..."
compose up -d db keycloak
for _ in $(seq 1 60); do
  compose exec -T db pg_isready -U "$USUARIO_DB" >/dev/null 2>&1 && break
  sleep 2
done
compose exec -T db pg_isready -U "$USUARIO_DB" >/dev/null 2>&1 || error "La base no responde."

# El programa de carga corre en un contenedor en la misma red que los servicios, asi les
# habla por nombre igual en local y en el servidor.
contenedor_db="$(compose ps -q db)"
[ -n "$contenedor_db" ] || error "No se encontro el contenedor del servicio db."
red="$(docker inspect -f '{{range $nombre, $red := .NetworkSettings.Networks}}{{println $nombre}}{{end}}' "$contenedor_db" | head -n 1)"
[ -n "$red" ] || error "No se pudo detectar la red de docker compose."
directorio_seed="$(pwd -W 2>/dev/null || pwd)/seed"

seed() {
  docker run --rm --network "$red" -v "${directorio_seed}:/seed:ro" \
    -e SEED_ADMIN_PASSWORD -e DONACIONES_CLIENT_SECRET -e LOGISTICA_CLIENT_SECRET \
    -e KEYCLOAK_REALM -e SEED_KEYCLOAK_URL -e SEED_DONACIONES_URL \
    -e SEED_LOGISTICA_URL -e SEED_WEB_URL -e SEED_WEB_CLIENT_ID \
    "$IMAGEN_PYTHON" python -u /seed/seed.py "$@"
}

# Antes de borrar: si falta alguna cuenta de demo, se corta aca y no se pierde nada.
paso "Verificando las cuentas de demo en Keycloak..."
seed verificar || error "Las cuentas de demo no estan listas; no se borro nada."

paso "Deteniendo Donaciones y Logistica..."
compose stop donaciones logistica

for base in "${BASES[@]}"; do
  paso "Recreando la base $base..."
  compose exec -T db psql -U "$USUARIO_DB" -d postgres -v ON_ERROR_STOP=1 \
    -c "DROP DATABASE IF EXISTS $base WITH (FORCE);" \
    -c "CREATE DATABASE $base;"
done

paso "Vaciando las fotos de entregas..."
compose run --rm --no-deps -T --entrypoint sh logistica -c \
  'dir="${LOGISTICA_FOTOS_DIR:-/app/fotos}"; mkdir -p "$dir" && find "$dir" -mindepth 1 -delete'

# Al arrancar, Hibernate crea las tablas, Donaciones siembra las subcategorias y los
# repositorios en memoria (por ejemplo, las propuestas de asignacion) empiezan vacios.
paso "Levantando Donaciones y Logistica..."
compose up -d donaciones logistica

paso "Cargando los datos de demostracion..."
seed cargar || error "La carga fallo. Las bases quedaron recreadas; corregi el problema y volve a correr el reset."

# UNICA excepcion a "todo por las APIs" (ver docs/DEMO.md): el aviso de inactividad necesita
# donantes cuya ultima interaccion tenga mas de DIAS_SIN_INTERACCION (20) dias, y ninguna API
# permite cargar fechas pasadas (ni tiene que permitirlo). Se corren hacia atras las fechas
# de 5 donantes del CSV, elegidos por documento fijo y sin cuenta de demo (nunca donante1 ni
# donante2), y se deja su ultimo aviso en NULL para que el job los tome.
paso "Dejando 5 donantes del CSV inactivos para el aviso de inactividad..."
inactivos="$(compose exec -T db psql -U "$USUARIO_DB" -d donatrack_donaciones -v ON_ERROR_STOP=1 -tA -c "
  WITH corridos AS (
    UPDATE donante d
       SET ultima_interaccion = CURRENT_DATE - x.dias,
           fecha_ultimo_aviso_inactividad = NULL
      FROM persona_humana h,
           (VALUES ('40000030', 25), ('40000031', 32), ('40000032', 40),
                   ('40000033', 51), ('40000034', 60)) AS x(documento, dias)
     WHERE h.id = d.persona_id AND h.documento = x.documento
    RETURNING d.donante_id)
  SELECT count(*) FROM corridos;")" || error "No se pudieron correr las fechas de los donantes inactivos."
[ "$(echo "$inactivos" | tr -d '[:space:]')" = "5" ] \
  || error "Se esperaban 5 donantes inactivos y se actualizaron: ${inactivos:-ninguno}."

paso "Listo. Ver docs/DEMO.md para lo que muestra cada cuenta."
