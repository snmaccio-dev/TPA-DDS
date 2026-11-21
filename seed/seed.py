#!/usr/bin/env python3
"""Carga de datos de demostracion de DonaTrack.

Lo ejecuta seed.sh dentro de un contenedor conectado a la red de docker compose, asi que
habla con los servicios por su nombre (keycloak, donaciones, logistica) igual en local y
en el servidor. Solo usa la biblioteca estandar de Python: no hay nada que instalar ni
compilar.

Comandos:
  verificar  Comprueba que las cuentas de seed/cuentas-demo.json existan en Keycloak con
             su rol y que el admin pueda iniciar sesion. No toca ninguna base.
  cargar     Espera a que Donaciones y Logistica respondan (recien levantados, con las
             bases vacias) y carga los datos SOLO por las APIs HTTP existentes.

Variables de entorno (seed.sh las toma del .env):
  SEED_ADMIN_PASSWORD        contrasena de la cuenta ADMINISTRADOR (obligatoria)
  DONACIONES_CLIENT_SECRET   cuenta de servicio donaciones-backend: con ella se consulta
                             Keycloak (cuentas, roles y keycloak_id del chofer)
  LOGISTICA_CLIENT_SECRET    cuenta de servicio logistica-backend (rol SERVICIO_LOGISTICA):
                             reporta las posiciones GPS de la ruta en curso
  KEYCLOAK_REALM, SEED_KEYCLOAK_URL, SEED_DONACIONES_URL, SEED_LOGISTICA_URL,
  SEED_WEB_URL, SEED_WEB_CLIENT_ID
                             opcionales, con defaults para la red de compose
"""

import datetime
import json
import os
import re
import struct
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
import zlib
import base64

DIRECTORIO = os.path.dirname(os.path.abspath(__file__))


def env(nombre, defecto=None):
    valor = os.environ.get(nombre)
    return valor if valor else defecto


KEYCLOAK = env("SEED_KEYCLOAK_URL", "http://keycloak:8080").rstrip("/")
REALM = env("KEYCLOAK_REALM", "donatrack")
DONACIONES = env("SEED_DONACIONES_URL", "http://donaciones:8080").rstrip("/")
LOGISTICA = env("SEED_LOGISTICA_URL", "http://logistica:8081").rstrip("/")
WEB = env("SEED_WEB_URL", "http://web:8082").rstrip("/")
CLIENTE_WEB = env("SEED_WEB_CLIENT_ID", "donatrack-web")

PERFILES_EXCLUYENTES = {"ADMINISTRADOR", "DONANTE", "BENEFICIARIA"}


class SeedError(Exception):
    pass


def log(mensaje):
    print("[SEED] " + mensaje, flush=True)


# =====================================================================================
# HTTP
# =====================================================================================

def pedir(metodo, url, token=None, json_cuerpo=None, form=None, multipart=None,
          encabezados=None, esperados=(200, 201, 202, 204)):
    datos = None
    headers = {"Accept": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    if json_cuerpo is not None:
        datos = json.dumps(json_cuerpo).encode("utf-8")
        headers["Content-Type"] = "application/json"
    elif form is not None:
        datos = urllib.parse.urlencode(form).encode("utf-8")
        headers["Content-Type"] = "application/x-www-form-urlencoded"
    elif multipart is not None:
        datos, tipo = multipart
        headers["Content-Type"] = tipo
    if encabezados:
        headers.update(encabezados)

    pedido = urllib.request.Request(url, data=datos, method=metodo, headers=headers)
    try:
        with urllib.request.urlopen(pedido, timeout=90) as respuesta:
            estado = respuesta.status
            cuerpo = respuesta.read()
    except urllib.error.HTTPError as e:
        estado = e.code
        cuerpo = e.read()

    texto = cuerpo.decode("utf-8", "replace")
    if estado not in esperados:
        raise SeedError(metodo + " " + url + " respondio " + str(estado) + ": " + texto[:600])
    if not texto.strip():
        return None
    try:
        return json.loads(texto)
    except ValueError:
        return texto


def multipart(campo, archivos):
    frontera = "----donatrack-seed-" + uuid.uuid4().hex
    partes = []
    for nombre, tipo, contenido in archivos:
        partes.append((
            "--" + frontera + "\r\n"
            + "Content-Disposition: form-data; name=\"" + campo + "\"; filename=\"" + nombre + "\"\r\n"
            + "Content-Type: " + tipo + "\r\n\r\n").encode("utf-8"))
        partes.append(contenido)
        partes.append(b"\r\n")
    partes.append(("--" + frontera + "--\r\n").encode("utf-8"))
    return b"".join(partes), "multipart/form-data; boundary=" + frontera


def esperar(nombre, condicion, segundos=300):
    limite = time.time() + segundos
    ultimo = None
    while time.time() < limite:
        try:
            if condicion():
                return
        except (SeedError, OSError, ValueError) as e:
            ultimo = e
        time.sleep(3)
    raise SeedError(nombre + " no respondio en " + str(segundos) + " s. Ultimo error: " + str(ultimo))


# =====================================================================================
# Keycloak
# =====================================================================================

class Token:
    """Access token que se pide de nuevo un minuto antes de vencer."""

    def __init__(self, descripcion, formulario):
        self.descripcion = descripcion
        self.formulario = formulario
        self.valor = None
        self.vence = 0

    def __call__(self):
        if self.valor is None or time.time() > self.vence - 60:
            try:
                datos = pedir("POST", url_token(), form=self.formulario())
            except SeedError as e:
                raise SeedError("No se pudo obtener el token de " + self.descripcion + ". " + str(e))
            self.valor = datos["access_token"]
            self.vence = time.time() + int(datos.get("expires_in", 300))
        return self.valor


def url_token():
    return KEYCLOAK + "/realms/" + REALM + "/protocol/openid-connect/token"


def obligatoria(nombre):
    valor = env(nombre)
    if not valor:
        raise SeedError("Falta la variable " + nombre + " (ponela en el .env o exportala).")
    return valor


def token_de_servicio(client_id, variable_secreto):
    secreto = obligatoria(variable_secreto)
    return Token("la cuenta de servicio " + client_id, lambda: {
        "grant_type": "client_credentials",
        "client_id": client_id,
        "client_secret": secreto,
    })


def token_admin(email):
    contrasena = obligatoria("SEED_ADMIN_PASSWORD")
    return Token(email + " (revisa SEED_ADMIN_PASSWORD y que el client " + CLIENTE_WEB
                 + " tenga Direct access grants)", lambda: {
                     "grant_type": "password",
                     "client_id": CLIENTE_WEB,
                     "username": email,
                     "password": contrasena,
                     "scope": "openid",
                 })


def roles_del_token(token):
    carga = token.split(".")[1]
    carga += "=" * (-len(carga) % 4)
    datos = json.loads(base64.urlsafe_b64decode(carga.encode("ascii")))
    return set(datos.get("realm_access", {}).get("roles", []))


def esperar_keycloak():
    esperar("Keycloak", lambda: pedir(
        "GET", KEYCLOAK + "/realms/" + REALM + "/.well-known/openid-configuration") is not None)


def leer_cuentas():
    with open(os.path.join(DIRECTORIO, "cuentas-demo.json"), encoding="utf-8") as archivo:
        return json.load(archivo)["cuentas"]


# Misma regla que ReglasDePersona.nombre en Donaciones: letras (con tildes, dieresis y
# enie), espacios y apostrofo, con al menos 3 letras.
LETRAS_NOMBRE = "A-Za-zÁÉÍÓÚáéíóúÜüÑñ"


def nombre_valido(valor):
    valor = " ".join((valor or "").split())
    return (re.fullmatch("[" + LETRAS_NOMBRE + "' ]+", valor) is not None
            and len(re.findall("[" + LETRAS_NOMBRE + "]", valor)) >= 3)


def verificar_cuentas(cuentas):
    """Resuelve cada cuenta por mail (sin IDs fijos) y controla rol y estado."""
    servicio = token_de_servicio("donaciones-backend", "DONACIONES_CLIENT_SECRET")
    admin_api = KEYCLOAK + "/admin/realms/" + REALM
    errores = []
    encontradas = {}

    for cuenta in cuentas:
        email = cuenta["email"]
        tipo = cuenta["tipo"]
        usuarios = pedir("GET", admin_api + "/users?exact=true&email="
                         + urllib.parse.quote(email), servicio())
        usuario = next((u for u in usuarios
                        if (u.get("email") or "").lower() == email.lower()), None)
        if usuario is None:
            errores.append(email + ": no existe en Keycloak (tiene que tener el rol " + tipo + ").")
            continue
        if not usuario.get("enabled", True):
            errores.append(email + ": la cuenta esta deshabilitada.")
        roles = {r["name"] for r in pedir(
            "GET", admin_api + "/users/" + usuario["id"] + "/role-mappings/realm/composite",
            servicio())}
        if tipo not in roles:
            errores.append(email + ": no tiene el rol " + tipo + ".")
        perfiles = roles & PERFILES_EXCLUYENTES
        if len(perfiles) > 1:
            errores.append(email + ": tiene mas de un perfil (" + ", ".join(sorted(perfiles))
                           + "); Donaciones rechaza esos tokens.")
        nombre, apellido = usuario.get("firstName"), usuario.get("lastName")
        if not nombre_valido(nombre) or not nombre_valido(apellido):
            log("Aviso: " + email + " no tiene en Keycloak un nombre y apellido que cumplan las "
                + "reglas de Donaciones (al menos 3 letras, sin numeros ni simbolos): se usan "
                + "valores de demo.")
        encontradas[email] = {
            "id": usuario["id"],
            "nombre": nombre if nombre_valido(nombre) else tipo.capitalize(),
            "apellido": apellido if nombre_valido(apellido) else "Demo",
        }

    if errores:
        raise SeedError("Las cuentas de demo no estan listas en Keycloak:\n  - "
                        + "\n  - ".join(errores)
                        + "\nCrealas o corregilas (ver docs/DEMO.md) y volve a correr el reset.")

    admin = next(c for c in cuentas if c["tipo"] == "ADMINISTRADOR")
    token = token_admin(admin["email"])
    if "ADMINISTRADOR" not in roles_del_token(token()):
        raise SeedError("El token de " + admin["email"] + " no trae el rol ADMINISTRADOR.")
    return encontradas, token


# =====================================================================================
# Datos de demostracion
# =====================================================================================

# Subcategorias que usa la demo; tienen que existir en el catalogo que siembra Donaciones.
# (descripcion, cantidad, condicion, perecedero)
BIENES = {
    "Fideos secos": ("Fideos secos en paquetes de 1 kg", 40, "NUEVO", True),
    "Arroz": ("Arroz largo fino", 50, "NUEVO", True),
    "Leche": ("Leche larga vida", 60, "NUEVO", True),
    "Aceite": ("Aceite de girasol de 1,5 l", 30, "NUEVO", True),
    "Legumbres": ("Lentejas y porotos secos", 25, "NUEVO", True),
    "Conservas": ("Latas de arvejas y choclo", 48, "NUEVO", True),
    "Camperas": ("Camperas de abrigo talles varios", 20, "USADO", False),
    "Buzos": ("Buzos de algodón", 30, "USADO", False),
    "Calzado": ("Zapatillas talles 34 a 42", 18, "USADO", False),
    "Utiles escolares": ("Kits de útiles escolares", 35, "NUEVO", False),
    "Cuadernos": ("Cuadernos rayados tapa dura", 80, "NUEVO", False),
    "Mochilas": ("Mochilas escolares", 15, "NUEVO", False),
    "Sillas": ("Sillas de madera", 12, "USADO", False),
    "Mesas": ("Mesas plegables", 6, "USADO", False),
    "Colchones": ("Colchones de una plaza", 8, "NUEVO", False),
    "Juegos de mesa": ("Juegos de mesa para chicos", 10, "USADO", False),
    # Solo las pide la red de escritorios (caso sin coincidencias).
    "Escritorios": ("Escritorios escolares", 6, "USADO", False),
    # Ninguna entidad la pide (caso sin candidatas compatibles).
    "Yerba": ("Yerba mate de 1 kg", 30, "NUEVO", True),
}

# Beneficiarias extra para que el matchmaking tenga varias candidatas. Mismas reglas que
# las cuentas de demo: mail fijo, la primera vez se crea la cuenta y despues se vincula.
# Las 5 beneficiarias que reciben entregas (las 2 de cuentas-demo.json y estas 3) tienen
# direccion en CABA y coordenadas fijas: el alta las manda y Donaciones no geocodifica.
BENEFICIARIAS_EXTRA = [
    {"email": "beneficiaria03@demo.donatrack.ar", "cuit": "30700000033",
     "razonSocial": "Hogar Rayito de Sol", "tipoOrganizacion": "ONG", "rubro": "Hogar de niños",
     "direccion": "Av. del Libertador 1473, Recoleta, CABA",
     "latitud": -34.5838, "longitud": -58.3930, "telefono": "1146333003",
     "nombre": "Marta", "apellido": "Quiroga"},
    {"email": "beneficiaria04@demo.donatrack.ar", "cuit": "30700000044",
     "razonSocial": "Club de Barrio Villa Soldati", "tipoOrganizacion": "ONG", "rubro": "Club social",
     "direccion": "Av. Ángel Gallardo 470, Caballito, CABA",
     "latitud": -34.6051, "longitud": -58.4386, "telefono": "1149194004",
     "nombre": "Raúl", "apellido": "Ferreyra"},
    {"email": "beneficiaria05@demo.donatrack.ar", "cuit": "30700000055",
     "razonSocial": "Parroquia San Cayetano Liniers", "tipoOrganizacion": "INSTITUCION",
     "rubro": "Asistencia social", "direccion": "Av. Sarmiento 2601, Palermo, CABA",
     "latitud": -34.5696, "longitud": -58.4115, "telefono": "1146415005",
     "nombre": "Inés", "apellido": "Domínguez"},
]

# Red de 20 entidades que solo piden escritorios, para que la asignacion muestre las dos
# ejecuciones por separado. Los dos algoritmos ordenan las mismas candidatas (las que
# necesitan la subcategoria) y toman hasta 10, asi que solo dejan de coincidir con mas de
# 10 candidatas. Ninguna recibio donaciones: Prioridad desempata por id y se queda con las
# 10 primeras en darse de alta, que piden poco; Compatibilidad se queda con las 10
# ultimas, que piden mucho. Los dos rankings quedan disjuntos.
# Apellidos de los representantes de la red: solo letras (las reglas de Donaciones no
# aceptan numeros en nombres y apellidos).
APELLIDOS_RED = [
    "Acosta", "Benítez", "Cabrera", "Díaz", "Escobar", "Ferreyra", "Giménez", "Herrera",
    "Ibáñez", "Juárez", "Ledesma", "Medina", "Núñez", "Ortiz", "Peralta", "Quiroga",
    "Ríos", "Sosa", "Toledo", "Vera",
]

RED_ESCRITORIOS = [
    {"email": "red-escritorios-%02d@demo.donatrack.ar" % n, "cuit": "3071%07d" % n,
     "razonSocial": "Escuela Red Sur N.º %d" % n, "tipoOrganizacion": "INSTITUCION",
     "rubro": "Educación", "direccion": "Av. Sáenz %d, CABA" % (1000 + n * 10),
     "telefono": "114900%04d" % n, "nombre": "Docente", "apellido": APELLIDOS_RED[n - 1],
     "cantidadNecesidad": 2 if n <= 10 else 20 + n}
    for n in range(1, 21)
]

NECESIDADES = {
    "beneficiaria1@gmail.com": ["Fideos secos", "Arroz", "Leche", "Aceite", "Legumbres", "Mesas"],
    "beneficiaria2@gmail.com": ["Utiles escolares", "Cuadernos", "Mochilas", "Camperas", "Buzos", "Sillas"],
    "beneficiaria03@demo.donatrack.ar": ["Colchones", "Camperas", "Calzado", "Conservas", "Leche"],
    "beneficiaria04@demo.donatrack.ar": ["Juegos de mesa", "Buzos", "Calzado", "Fideos secos"],
    "beneficiaria05@demo.donatrack.ar": ["Arroz", "Legumbres", "Conservas", "Aceite", "Camperas"],
}
NECESIDADES.update({b["email"]: ["Escritorios"] for b in RED_ESCRITORIOS})

CAMIONES = [
    {"patente": "AE123CD", "capacidadVolumen": 18, "altura": 2.6, "capacidadCarga": 4000},
    {"patente": "AF456GH", "capacidadVolumen": 22, "altura": 2.8, "capacidadCarga": 6000},
    {"patente": "AD789JK", "capacidadVolumen": 14, "altura": 2.4, "capacidadCarga": 3000},
    {"patente": "AG321LM", "capacidadVolumen": 26, "altura": 3.0, "capacidadCarga": 8000},
]

# Recorrido de la ruta en curso (Parque Patricios hacia Avellaneda).
RECORRIDO_GPS = [
    (-34.6370, -58.4020, 0), (-34.6395, -58.3990, 24), (-34.6420, -58.3950, 31),
    (-34.6448, -58.3905, 28), (-34.6475, -58.3860, 35), (-34.6502, -58.3812, 22),
]

# Estados objetivo. Los nombres son de la demo; ESPERADO dice en que estado de Donaciones
# tiene que terminar cada uno.
PENDIENTE, DEPOSITO, PROPUESTA, ASIGNADA, LISTA, TRASLADO, ENTREGADA, VENCIDA = (
    "PENDIENTE", "DEPOSITO", "PROPUESTA", "ASIGNADA", "LISTA", "TRASLADO", "ENTREGADA", "VENCIDA")
# Asignadas, ya en Logistica y con medidas, sin ruta: las toma JobPlanificacionRutas.
PARA_PLANIFICAR = "PARA_PLANIFICAR"
CICLO = [PENDIENTE, DEPOSITO, PROPUESTA, ASIGNADA, LISTA, TRASLADO, ENTREGADA, VENCIDA]
ESTADOS = CICLO + [PARA_PLANIFICAR]
CON_DESTINO = {ASIGNADA, LISTA, TRASLADO, ENTREGADA}
ESPERADO = {
    PENDIENTE: "PENDIENTE_RECEPCION", DEPOSITO: "EN_DEPOSITO", PROPUESTA: "EN_DEPOSITO",
    ASIGNADA: "ASIGNACION_REALIZADA", LISTA: "LISTA_PARA_ENTREGAR", TRASLADO: "EN_TRASLADO",
    ENTREGADA: "ENTREGADA", VENCIDA: "VENCIDA", PARA_PLANIFICAR: "ASIGNACION_REALIZADA",
}
DONACIONES_POR_DONANTE_CSV = 2


def solo_digitos(texto):
    return "".join(c for c in texto if c.isdigit())


def leer_csv_donantes():
    documentos = []
    with open(os.path.join(DIRECTORIO, "donantes-demo.csv"), encoding="utf-8") as archivo:
        for numero, linea in enumerate(archivo):
            campos = linea.rstrip("\r\n").split(",")
            if numero == 0 or len(campos) < 6:
                continue
            tipo, _, documento = campos[0], campos[1], campos[2]
            documentos.append(solo_digitos(documento) if tipo == "JURIDICA" else documento)
    return documentos


def armar_plan(doc_donante1, doc_donante2, beneficiarias, documentos_csv):
    """Lista de donaciones a crear: donante, estado objetivo, destino y subcategoria."""
    b1, b2 = "beneficiaria1@gmail.com", "beneficiaria2@gmail.com"
    plan = [
        # donante1: un recorrido completo, incluida una en traslado y una entregada.
        (doc_donante1, PENDIENTE, None, "Camperas"),
        (doc_donante1, DEPOSITO, None, "Sillas"),
        (doc_donante1, PROPUESTA, None, "Fideos secos"),
        (doc_donante1, ASIGNADA, b1, "Arroz"),
        (doc_donante1, TRASLADO, b1, "Leche"),
        (doc_donante1, ENTREGADA, b1, "Aceite"),
        (doc_donante1, VENCIDA, None, "Leche"),
        # donante2 (persona juridica).
        (doc_donante2, PENDIENTE, None, "Legumbres"),
        (doc_donante2, PROPUESTA, None, "Buzos"),
        (doc_donante2, ASIGNADA, b2, "Cuadernos"),
        (doc_donante2, LISTA, b2, "Mochilas"),
        (doc_donante2, TRASLADO, b2, "Utiles escolares"),
        (doc_donante2, ENTREGADA, b2, "Camperas"),
        (doc_donante2, VENCIDA, None, "Arroz"),
        # Casos de la pantalla de asignacion: "Fideos secos" y "Buzos" (arriba) tienen
        # coincidencias; "Escritorios" muestra las dos ejecuciones por separado y "Yerba"
        # no tiene ninguna entidad con una necesidad compatible.
        (doc_donante1, PROPUESTA, None, "Escritorios"),
        (doc_donante2, PROPUESTA, None, "Yerba"),
    ]
    # Para JobPlanificacionRutas: una por cada beneficiaria de demo, con una subcategoria que
    # esa entidad necesita (asi es candidata y se puede confirmar como destino).
    for n, destino in enumerate([b1, b2] + [b["email"] for b in BENEFICIARIAS_EXTRA]):
        plan.append((doc_donante1 if n % 2 == 0 else doc_donante2, PARA_PLANIFICAR, destino,
                     NECESIDADES[destino][1]))

    # Sin "Escritorios": las donaciones del CSV quedan en los casos con coincidencias.
    necesitadas = sorted({s for lista in NECESIDADES.values() for s in lista} - {"Escritorios"})
    todas = sorted(BIENES)
    perecederos = sorted(s for s, datos in BIENES.items() if datos[3])
    orden_beneficiarias = [b1, b2] + [b["email"] for b in BENEFICIARIAS_EXTRA]

    indice = 0
    for documento in documentos_csv:
        for _ in range(DONACIONES_POR_DONANTE_CSV):
            estado = CICLO[indice % len(CICLO)]
            vuelta = indice // len(CICLO)
            if estado in CON_DESTINO:
                destino = orden_beneficiarias[vuelta % len(orden_beneficiarias)]
                pedidas = NECESIDADES[destino]
                subcategoria = pedidas[vuelta % len(pedidas)]
            elif estado == PROPUESTA:
                destino = None
                subcategoria = necesitadas[vuelta % len(necesitadas)]
            elif estado == VENCIDA:
                destino = None
                subcategoria = perecederos[vuelta % len(perecederos)]
            else:
                destino = None
                subcategoria = todas[(indice * 7) % len(todas)]
            plan.append((documento, estado, destino, subcategoria))
            indice += 1

    return [{"donante": d, "estado": e, "destino": dest, "subcategoria": s,
             "beneficiariaId": beneficiarias.get(dest)} for d, e, dest, s in plan]


def orden_de_confirmacion(items):
    """Orden en que se confirman las entregas. El mapa publico muestra las ultimas 5: se
    arman con 4 entidades distintas y una de ellas con 2 entregas, para que haya un popup
    con varias."""
    repetida = "beneficiaria1@gmail.com"
    dos = [i for i in items if i["destino"] == repetida][:2]
    otras = [i for i in items if all(i is not d for d in dos)]
    tres, vistas = [], set()
    for item in reversed(otras):
        if item["destino"] != repetida and item["destino"] not in vistas and len(tres) < 3:
            tres.insert(0, item)
            vistas.add(item["destino"])
    if len(dos) < 2 or len(tres) < 3:
        return items
    resto = [i for i in otras if all(i is not t for t in tres)]
    return resto + [dos[0], tres[0], tres[1], dos[1], tres[2]]


def png(ancho, alto, desde, hasta):
    """PNG con un degradado: alcanza como foto de comprobante y no hace falta un archivo."""
    filas = []
    for y in range(alto):
        t = y / (alto - 1)
        color = bytes(int(a + (b - a) * t) for a, b in zip(desde, hasta))
        filas.append(b"\x00" + color * ancho)

    def bloque(tipo, datos):
        return (struct.pack(">I", len(datos)) + tipo + datos
                + struct.pack(">I", zlib.crc32(tipo + datos) & 0xFFFFFFFF))

    return (b"\x89PNG\r\n\x1a\n"
            + bloque(b"IHDR", struct.pack(">IIBBBBB", ancho, alto, 8, 2, 0, 0, 0))
            + bloque(b"IDAT", zlib.compress(b"".join(filas), 9))
            + bloque(b"IEND", b""))


PALETAS = [((31, 117, 88), (234, 245, 239)), ((36, 87, 197), (234, 242, 255)),
           ((200, 133, 32), (255, 243, 223)), ((120, 90, 200), (240, 237, 255))]


# =====================================================================================
# Carga
# =====================================================================================

class Carga:

    def __init__(self, cuentas, en_keycloak, admin):
        self.cuentas = {c["email"]: c for c in cuentas}
        self.keycloak = en_keycloak
        self.admin = admin
        self.gps = token_de_servicio("logistica-backend", "LOGISTICA_CLIENT_SECRET")
        self.beneficiarias = {}

    # -- atajos -------------------------------------------------------------------
    def don(self, metodo, ruta, **kwargs):
        return pedir(metodo, DONACIONES + ruta, self.admin(), **kwargs)

    def log_(self, metodo, ruta, **kwargs):
        return pedir(metodo, LOGISTICA + ruta, self.admin(), **kwargs)

    def cuentas_de(self, tipo):
        return [c for c in self.cuentas.values() if c["tipo"] == tipo]

    # -- pasos ----------------------------------------------------------------------
    def esperar_servicios(self):
        log("Esperando a Donaciones (con el catalogo sembrado) y a Logistica...")
        esperar("Donaciones", lambda: len(self.don("GET", "/subcategorias") or []) > 0)
        esperar("Logistica", lambda: pedir("GET", LOGISTICA + "/entregas/publicas") is not None)
        catalogo = {s["nombre"] for s in self.don("GET", "/subcategorias")}
        faltan = sorted(set(BIENES) - catalogo)
        if faltan:
            raise SeedError("El catalogo de Donaciones no tiene estas subcategorias: "
                            + ", ".join(faltan))

    def crear_donantes_demo(self):
        documentos = {}
        for cuenta in self.cuentas_de("DONANTE"):
            perfil = cuenta["perfil"]
            persona = self.keycloak[cuenta["email"]]
            cuerpo = {"email": cuenta["email"], "tipo": perfil["tipoPersona"],
                      "direccion": perfil.get("direccion")}
            if perfil["tipoPersona"] == "HUMANA":
                cuerpo.update(nombre=persona["nombre"], apellido=persona["apellido"],
                              edad=perfil.get("edad"), documento=perfil["documento"],
                              genero=perfil.get("genero"))
                documentos[cuenta["email"]] = perfil["documento"]
            else:
                cuerpo.update(cuit=perfil["cuit"], razonSocial=perfil["razonSocial"],
                              tipoOrganizacion=perfil["tipoOrganizacion"], rubro=perfil["rubro"])
                documentos[cuenta["email"]] = solo_digitos(perfil["cuit"])
            # El alta vincula la cuenta existente por mail (no crea otra).
            self.don("POST", "/donantes", json_cuerpo=cuerpo)
            log("Donante " + cuenta["email"] + " vinculado a su cuenta.")
        return documentos

    def crear_beneficiarias(self):
        altas = []
        for cuenta in self.cuentas_de("BENEFICIARIA"):
            persona = self.keycloak[cuenta["email"]]
            altas.append(dict(cuenta["perfil"], email=cuenta["email"],
                              nombre=persona["nombre"], apellido=persona["apellido"]))
        altas.extend(BENEFICIARIAS_EXTRA)
        # La red va al final y en orden: los ids crecientes son el desempate de Prioridad.
        altas.extend(RED_ESCRITORIOS)

        for alta in altas:
            creada = self.don("POST", "/entidades/nueva", json_cuerpo={
                "cuit": alta["cuit"], "razonSocial": alta["razonSocial"],
                "tipoOrganizacion": alta["tipoOrganizacion"], "rubro": alta["rubro"],
                "direccion": alta["direccion"], "telefono": alta["telefono"],
                "representantes": [{"nombre": alta["nombre"], "apellido": alta["apellido"],
                                    "email": alta["email"]}],
                # Solo las que reciben entregas; la red de escritorios queda sin ubicar.
                "latitud": alta.get("latitud"), "longitud": alta.get("longitud"),
            })
            self.beneficiarias[alta["email"]] = creada["id"]
            for orden, subcategoria in enumerate(NECESIDADES[alta["email"]]):
                recurrente = orden % 3 == 0
                self.don("POST", "/necesidades", json_cuerpo={
                    "cuitBeneficiaria": alta["cuit"],
                    "descripcion": "Necesitamos " + BIENES[subcategoria][0].lower(),
                    "cantidad": alta.get("cantidadNecesidad") or BIENES[subcategoria][1] * 2,
                    "subcategoria": subcategoria,
                    "tipo": "RECURRENTE" if recurrente else "EXTRAORDINARIA",
                    "periodo": "MENSUAL" if recurrente else None,
                })
            log("Beneficiaria " + alta["razonSocial"] + " (" + alta["email"] + ") con "
                + str(len(NECESIDADES[alta["email"]])) + " necesidades.")

    def crear_flota(self):
        for camion in CAMIONES:
            self.log_("POST", "/camiones", json_cuerpo=camion)
        cuenta = self.cuentas_de("CHOFER")[0]
        persona = self.keycloak[cuenta["email"]]
        chofer = self.log_("POST", "/choferes", json_cuerpo={
            "nombre": persona["nombre"], "apellido": persona["apellido"],
            "documento": cuenta["perfil"]["documento"], "licencia": cuenta["perfil"]["licencia"],
            "keycloakId": persona["id"],
        })
        log(str(len(CAMIONES)) + " camiones y el chofer " + cuenta["email"] + " cargados.")
        return chofer["id"]

    def importar_csv(self):
        with open(os.path.join(DIRECTORIO, "donantes-demo.csv"), "rb") as archivo:
            contenido = archivo.read()
        iniciada = self.don("POST", "/donantes/importaciones", multipart=multipart(
            "archivo", [("donantes-demo.csv", "text/csv", contenido)]))
        importacion_id = iniciada["importacionId"]
        log("Importacion CSV " + str(importacion_id) + " en curso...")

        resultado = {}

        def termino():
            resultado.update(self.don("GET", "/donantes/importaciones/" + str(importacion_id)))
            return resultado["estado"] != "EN_CURSO"

        esperar("La importacion CSV", termino, segundos=600)
        if resultado["estado"] == "FALLIDA":
            raise SeedError("La importacion CSV fallo: " + str(resultado.get("motivoFallo")))
        if resultado["filasConError"]:
            errores = "; ".join("fila " + str(e["fila"]) + ": " + e["mensaje"]
                                for e in resultado.get("errores") or [])
            raise SeedError("La importacion CSV tuvo filas con error: " + errores)
        log("CSV importado: " + str(resultado["creados"]) + " donantes creados, "
            + str(resultado["cuentasCreadas"]) + " cuentas creadas, "
            + str(resultado["cuentasVinculadas"]) + " cuentas existentes vinculadas.")

    def crear_donaciones(self, plan):
        hoy = datetime.date.today()
        for numero, item in enumerate(plan):
            descripcion, cantidad, condicion, perecedero = BIENES[item["subcategoria"]]
            vencimiento = None
            if item["estado"] == VENCIDA:
                vencimiento = hoy - datetime.timedelta(days=3 + numero % 5)
            elif perecedero:
                vencimiento = hoy + datetime.timedelta(days=45 + numero % 60)
            creadas = self.don("POST", "/donaciones", json_cuerpo={
                "donanteDocumento": item["donante"],
                "descripcion": descripcion,
                "bienes": [{
                    "descripcion": descripcion,
                    "subcategoria": item["subcategoria"],
                    "cantidad": cantidad + numero % 7,
                    "condicion": condicion,
                    "fechaVencimiento": vencimiento.isoformat() if vencimiento else None,
                }],
            })
            item["id"] = creadas[0]["donacionId"]
        log(str(len(plan)) + " donaciones registradas (pendientes de recepcion).")

    def recibir(self, items):
        for item in items:
            self.don("POST", "/donaciones/" + str(item["id"]) + "/confirmar-recepcion",
                     json_cuerpo={"donanteDocumento": item["donante"]})

    def confirmar_destino(self, items):
        for item in items:
            self.don("POST", "/donaciones/" + str(item["id"]) + "/destinatario",
                     json_cuerpo={"beneficiariaId": item["beneficiariaId"]})

    def planificar_rutas(self, por_estado):
        # 1) La corrida trae a Logistica lo asignado en Donaciones como entregas pendientes.
        #    Todavia ninguna tiene medidas, asi que no se manda nada a planificar.
        corrida = self.log_("POST", "/planificacion/corridas")
        if corrida["solicitudes"]:
            raise SeedError("La primera corrida no tenia que mandar nada a planificar.")
        entregas = {e["donacionId"]: e for e in self.log_("GET", "/entregas")}
        for estado in (ENTREGADA, TRASLADO, LISTA, PARA_PLANIFICAR):
            for item in por_estado[estado]:
                if item["id"] not in entregas:
                    raise SeedError("Logistica no incorporo la donacion " + str(item["id"]) + ".")
                item["entrega"] = entregas[item["id"]]

        # 2) Una corrida por grupo: se miden las entregas del grupo y la corrida las manda al
        #    planificador simulado, que contesta solo el callback unos segundos despues (como
        #    el proveedor externo). Cada grupo entra en un camion disponible, asi que queda
        #    una ruta por grupo, igual que antes.
        rutas = []
        for estado in (ENTREGADA, TRASLADO, LISTA):
            grupo = por_estado[estado]
            self.medir(grupo)
            corrida = self.log_("POST", "/planificacion/corridas")
            if len(corrida["solicitudes"]) != 1:
                raise SeedError("Se esperaba una solicitud de planificacion y hubo "
                                + str(len(corrida["solicitudes"])) + ".")
            rutas.append(self.esperar_ruta(estado, grupo))
        log(str(len(rutas)) + " rutas planificadas por el planificador simulado.")

        # 3) Las de PARA_PLANIFICAR quedan medidas y sin ruta: las toma el job.
        self.medir(por_estado[PARA_PLANIFICAR])
        return rutas

    def medir(self, grupo):
        for orden, item in enumerate(grupo):
            self.log_("POST", "/entregas/" + str(item["entrega"]["id"]) + "/medidas",
                      json_cuerpo={"pesoKg": 20 + orden * 3, "volumenM3": 0.3 + orden * 0.05})

    def esperar_ruta(self, estado, grupo):
        """Espera el callback del simulado: todas las entregas del grupo en una misma ruta."""
        ids = {item["entrega"]["id"] for item in grupo}
        rutas = set()

        def planificado():
            rutas.clear()
            for entrega in self.log_("GET", "/entregas"):
                if entrega["id"] in ids:
                    rutas.add(entrega["rutaId"])
            return None not in rutas

        esperar("La ruta del grupo " + estado, planificado, segundos=90)
        if len(rutas) != 1:
            raise SeedError("Las entregas del grupo " + estado + " quedaron en "
                            + str(len(rutas)) + " rutas y se esperaba una.")
        return rutas.pop()

    def iniciar(self, ruta_id, chofer_id):
        self.log_("POST", "/rutas/" + str(ruta_id) + "/inicio",
                  json_cuerpo={"choferId": chofer_id})

    def entregar_con_fotos(self, items):
        for numero, item in enumerate(items):
            entrega_id = str(item["entrega"]["id"])
            desde, hasta = PALETAS[numero % len(PALETAS)]
            fotos = [("entrega-" + entrega_id + "-" + str(n) + ".png", "image/png",
                      png(640, 420, desde if n == 1 else hasta, hasta if n == 1 else desde))
                     for n in (1, 2)]
            subidas = self.log_("POST", "/entregas/" + entrega_id + "/fotos",
                                multipart=multipart("fotos", fotos))
            self.log_("POST", "/entregas/" + entrega_id + "/confirmacion",
                      json_cuerpo={"fotos": subidas["urls"]})

    def reportar_posiciones(self, ruta_id, patente):
        # Sin "momento": Logistica toma su hora, asi las posiciones quedan en hora local
        # del servidor aunque este contenedor corra en UTC.
        for latitud, longitud, velocidad in RECORRIDO_GPS:
            pedir("POST", LOGISTICA + "/monitoreo/posiciones", self.gps(), json_cuerpo={
                "rutaId": ruta_id, "patenteCamion": patente, "latitud": latitud,
                "longitud": longitud, "velocidadKmh": velocidad,
            })
            time.sleep(0.5)

    def resumen(self, plan):
        esperados = {}
        for item in plan:
            esperados[ESPERADO[item["estado"]]] = esperados.get(ESPERADO[item["estado"]], 0) + 1
        reales = {}
        por_id = {d["donacionId"]: d["estado"] for d in self.don("GET", "/donaciones")}
        for estado in por_id.values():
            reales[estado] = reales.get(estado, 0) + 1
        distintas = [item for item in plan if por_id.get(item["id"]) != ESPERADO[item["estado"]]]

        log("Donaciones por estado:")
        for estado in sorted(set(esperados) | set(reales)):
            log("  " + estado.ljust(22) + str(reales.get(estado, 0)).rjust(4)
                + "  (esperadas " + str(esperados.get(estado, 0)) + ")")
        propuestas = self.don("GET", "/asignaciones/propuestas")
        log("Propuestas pendientes de asignacion: " + str(len(propuestas)))
        sin_candidatas = [p for p in propuestas if not p["candidatas"]]
        con_coincidencias = [p for p in propuestas if p["candidatas"] and p["huboCoincidencias"]]
        sin_coincidencias = [p for p in propuestas
                             if p["candidatas"] and not p["huboCoincidencias"]]
        log("  con coincidencias " + str(len(con_coincidencias))
            + ", sin coincidencias (dos rankings) " + str(len(sin_coincidencias))
            + ", sin candidatas compatibles " + str(len(sin_candidatas)))
        for nombre, grupo in (("con coincidencias", con_coincidencias),
                              ("sin coincidencias", sin_coincidencias),
                              ("sin candidatas", sin_candidatas)):
            if not grupo:
                log("ATENCION: no quedo ninguna propuesta " + nombre + ".")
        if distintas:
            raise SeedError(str(len(distintas)) + " donaciones no quedaron en el estado esperado, "
                            + "por ejemplo la " + str(distintas[0]["id"]) + " ("
                            + str(por_id.get(distintas[0]["id"])) + " en vez de "
                            + ESPERADO[distintas[0]["estado"]] + ").")

    def verificar_mapa_publico(self):
        """Lo que tiene que mostrar /donaciones-publicas con estos datos."""
        fijas = {c["email"]: (c["perfil"]["latitud"], c["perfil"]["longitud"])
                 for c in self.cuentas_de("BENEFICIARIA")}
        fijas.update({b["email"]: (b["latitud"], b["longitud"]) for b in BENEFICIARIAS_EXTRA})
        email_de = {self.beneficiarias[email]: email for email in fijas}

        # Todas las entregas (en traslado, entregadas, pendientes) van a las 5 ubicadas.
        otras = [e for e in self.log_("GET", "/entregas") if e["beneficiariaId"] not in email_de]
        if otras:
            raise SeedError(str(len(otras)) + " entregas van a entidades sin ubicacion fija, por "
                            + "ejemplo la " + str(otras[0]["id"]) + ".")

        entregadas = pedir("GET", LOGISTICA + "/entregas/publicas") or []
        por_entidad = {}
        for entrega in entregadas:
            por_entidad[entrega["beneficiariaId"]] = por_entidad.get(entrega["beneficiariaId"], 0) + 1
        if len(por_entidad) < 4 or max(por_entidad.values()) < 2:
            raise SeedError("Las entregas confirmadas tienen que repartirse entre al menos 4 "
                            + "entidades y alguna tener 2 o mas: " + str(por_entidad))

        ids = ",".join(str(i) for i in sorted(por_entidad))
        ubicaciones = {u["id"]: u for u in
                       pedir("GET", DONACIONES + "/entidades/ubicaciones?ids=" + ids) or []}
        for beneficiaria_id in por_entidad:
            ubicacion = ubicaciones.get(beneficiaria_id)
            latitud, longitud = fijas[email_de[beneficiaria_id]]
            if (ubicacion is None or abs(ubicacion["latitud"] - latitud) > 1e-6
                    or abs(ubicacion["longitud"] - longitud) > 1e-6):
                raise SeedError("La entidad " + email_de[beneficiaria_id] + " no quedo con sus "
                                + "coordenadas fijas: " + str(ubicacion))

        recientes = pedir("GET", LOGISTICA + "/entregas/publicas?limite=5") or []
        if [e["entregaId"] for e in recientes] != [e["entregaId"] for e in entregadas[:5]]:
            raise SeedError("GET /entregas/publicas?limite=5 no devolvio las 5 ultimas confirmadas.")

        log("Mapa publico: " + str(len(entregadas)) + " entregas confirmadas en "
            + str(len(por_entidad)) + " entidades ("
            + ", ".join(email_de[i] + " " + str(n) for i, n in sorted(por_entidad.items())) + ").")

        # La pagina, tal como la arma Web.
        try:
            html = pedir("GET", WEB + "/donaciones-publicas", encabezados={"Accept": "text/html"})
        except (SeedError, OSError) as e:
            log("ATENCION: no se pudo abrir " + WEB + "/donaciones-publicas para verificarla: "
                + str(e))
            return
        datos = re.search(r'<script type="application/json" id="mapa-entregas">(.*?)</script>',
                          html or "", re.S)
        marcadores = json.loads(datos.group(1)) if datos else []
        items = (html or "").count('class="donation-item"')
        # El mapa muestra las mismas 5 que la lista: un marcador por entidad de esas 5, y
        # entre todos los popups, exactamente esas 5 entregas.
        entidades_recientes = {e["beneficiariaId"] for e in recientes}
        if len(recientes) == 5 and len(entidades_recientes) != 4:
            raise SeedError("Las ultimas 5 entregas tenian que ser de 4 entidades (una con 2) y "
                            + "son de " + str(len(entidades_recientes)) + ".")
        en_popups = sum(len(m["entregas"]) for m in marcadores)
        if len(marcadores) != len(entidades_recientes) or en_popups != len(recientes):
            raise SeedError("/donaciones-publicas muestra " + str(len(marcadores))
                            + " marcadores con " + str(en_popups) + " entregas y se esperaban "
                            + str(len(entidades_recientes)) + " con " + str(len(recientes)) + ".")
        if items != len(recientes):
            raise SeedError("/donaciones-publicas lista " + str(items) + " entregas recientes y "
                            + "se esperaban " + str(len(recientes)) + ".")
        log("/donaciones-publicas: " + str(items) + " entregas recientes en " + str(len(marcadores))
            + " marcadores (uno por entidad de esas entregas).")

    def verificar_pendientes_para_el_job(self, items):
        """Lo que JobPlanificacionRutas tiene para procesar en su proxima corrida."""
        pendientes = {e["id"] for e in self.log_("GET", "/planificacion/pendientes")}
        faltan = [i["id"] for i in items if i["entrega"]["id"] not in pendientes]
        if faltan:
            raise SeedError("Estas donaciones no quedaron pendientes de planificar: " + str(faltan))

        ocupados = {r["patenteCamion"] for r in self.log_("GET", "/rutas")
                    if r["estado"] in ("PLANIFICADA", "EN_CURSO")}
        disponibles = sorted(c["patente"] for c in self.log_("GET", "/camiones")
                             if c["patente"] not in ocupados)
        if len(disponibles) < 2:
            raise SeedError("Se esperaban al menos 2 camiones disponibles y hay "
                            + str(disponibles) + ".")
        log("Para JobPlanificacionRutas: " + str(len(pendientes)) + " entregas medidas sin "
            + "ruta y " + str(len(disponibles)) + " camiones disponibles ("
            + ", ".join(disponibles) + ").")

    # -- orquestacion -----------------------------------------------------------------
    def correr(self):
        self.esperar_servicios()
        documentos = self.crear_donantes_demo()
        self.crear_beneficiarias()
        chofer_id = self.crear_flota()
        self.importar_csv()

        plan = armar_plan(documentos["donante1@gmail.com"], documentos["donante2@gmail.com"],
                          self.beneficiarias, leer_csv_donantes())
        por_estado = {estado: [i for i in plan if i["estado"] == estado] for estado in ESTADOS}

        self.crear_donaciones(plan)

        # Recepcion en deposito de todo lo que sigue el circuito. Las que quedan "en deposito"
        # sin propuesta se reciben despues del matchmaking.
        self.recibir([i for i in plan if i["estado"] not in (PENDIENTE, DEPOSITO)])
        for item in por_estado[VENCIDA]:
            self.don("POST", "/donaciones/" + str(item["id"]) + "/vencer")

        ejecucion = self.don("POST", "/asignaciones/ejecutar", json_cuerpo={})
        log("Asignacion ejecutada: " + str(ejecucion["procesadas"]) + " propuestas, "
            + str(len(ejecucion["fallidas"])) + " fallidas.")

        self.confirmar_destino(por_estado[LISTA] + por_estado[TRASLADO] + por_estado[ENTREGADA]
                               + por_estado[PARA_PLANIFICAR])
        self.recibir(por_estado[DEPOSITO])

        rutas = self.planificar_rutas(por_estado)
        ruta_entregada, ruta_en_curso, _ = rutas

        # Ruta 1: el chofer la hizo completa y cada entrega se confirmo con fotos.
        self.iniciar(ruta_entregada, chofer_id)
        self.entregar_con_fotos(orden_de_confirmacion(por_estado[ENTREGADA]))
        # Ruta 2: en curso, con posiciones GPS. La ruta 3 queda planificada para hoy.
        self.iniciar(ruta_en_curso, chofer_id)
        self.reportar_posiciones(
            ruta_en_curso, self.log_("GET", "/rutas/" + str(ruta_en_curso))["patenteCamion"])

        # Asignadas que todavia no pasaron a Logistica.
        self.confirmar_destino(por_estado[ASIGNADA])

        self.resumen(plan)
        self.verificar_mapa_publico()
        self.verificar_pendientes_para_el_job(por_estado[PARA_PLANIFICAR])
        log("Carga terminada.")


def main():
    if len(sys.argv) != 2 or sys.argv[1] not in ("verificar", "cargar"):
        print("Uso: seed.py verificar|cargar", file=sys.stderr)
        return 2
    try:
        cuentas = leer_cuentas()
        log("Esperando a Keycloak...")
        esperar_keycloak()
        en_keycloak, admin = verificar_cuentas(cuentas)
        log("Las " + str(len(cuentas)) + " cuentas de demo existen en Keycloak con su rol.")
        if sys.argv[1] == "cargar":
            Carga(cuentas, en_keycloak, admin).correr()
        return 0
    except SeedError as e:
        print("[SEED] ERROR: " + str(e), file=sys.stderr, flush=True)
        return 1


if __name__ == "__main__":
    sys.exit(main())
