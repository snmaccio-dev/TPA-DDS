#!/bin/sh
set -e

# El persistence.xml se arma por filtrado de Maven al compilar, asi que la conexion
# quedaria horneada en la imagen. Para que una misma imagen sirva en cualquier entorno,
# se compila con marcadores y se reemplazan aca, al arrancar el contenedor.
: "${DB_URL:?Falta la variable DB_URL}"
: "${DB_USER:?Falta la variable DB_USER}"
: "${DB_PASSWORD:?Falta la variable DB_PASSWORD}"

sed -e "s|__DB_URL__|${DB_URL}|g" \
    -e "s|__DB_USER__|${DB_USER}|g" \
    -e "s|__DB_PASSWORD__|${DB_PASSWORD}|g" \
    /app/config/META-INF/persistence.xml.plantilla \
    > /app/config/META-INF/persistence.xml

# /app/config va primero en el classpath, asi que este persistence.xml le gana al que
# viaja adentro del jar.
exec java -cp "/app/config:/app/app.jar:/app/libs/*" "${MAIN_CLASS}"
