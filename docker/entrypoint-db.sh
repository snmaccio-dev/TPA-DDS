#!/bin/sh
set -e

# El persistence.xml se arma por filtrado de Maven al compilar, asi que la conexion
# quedaria horneada en la imagen. Para que una misma imagen sirva en cualquier entorno,
# se compila con marcadores y se reemplazan aca, al arrancar el contenedor.
: "${DB_URL:?Falta la variable DB_URL}"
: "${DB_USER:?Falta la variable DB_USER}"
: "${DB_PASSWORD:?Falta la variable DB_PASSWORD}"

# El reemplazo se hace con awk y no con sed a proposito. En sed, un & en el texto de
# reemplazo significa "todo lo que matcheo", asi que una URL con varios parametros se
# corrompe en silencio y el sintoma recien aparece al conectar, irreconocible. Aca los
# valores entran por ENVIRON y se buscan con index/substr, donde ningun caracter es
# especial, y se escapan antes los que el XML no admite.
awk '
function escaparXml(valor,   salida, caracter) {
  salida = ""
  while (length(valor) > 0) {
    caracter = substr(valor, 1, 1)
    if (caracter == "&") salida = salida "&amp;"
    else if (caracter == "<") salida = salida "&lt;"
    else if (caracter == ">") salida = salida "&gt;"
    else if (caracter == "\"") salida = salida "&quot;"
    else salida = salida caracter
    valor = substr(valor, 2)
  }
  return salida
}
function reemplazar(linea, marcador, valor,   posicion, salida) {
  salida = ""
  while ((posicion = index(linea, marcador)) > 0) {
    salida = salida substr(linea, 1, posicion - 1) valor
    linea = substr(linea, posicion + length(marcador))
  }
  return salida linea
}
BEGIN {
  url = escaparXml(ENVIRON["DB_URL"])
  usuario = escaparXml(ENVIRON["DB_USER"])
  clave = escaparXml(ENVIRON["DB_PASSWORD"])
}
{
  linea = reemplazar($0, "__DB_URL__", url)
  linea = reemplazar(linea, "__DB_USER__", usuario)
  linea = reemplazar(linea, "__DB_PASSWORD__", clave)
  print linea
}
' /app/config/META-INF/persistence.xml.plantilla \
  > /app/config/META-INF/persistence.xml

# /app/config va primero en el classpath, asi que este persistence.xml le gana al que
# viaja adentro del jar.
exec java -cp "/app/config:/app/app.jar:/app/libs/*" "${MAIN_CLASS}"
