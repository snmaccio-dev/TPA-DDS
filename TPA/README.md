# DonaTrack — Sistema de Gestión y Trazabilidad de Donaciones

**Grupo 13 · UTN FRBA · Diseño de Sistemas · 2026**  
**Entrega 1 — Arquitectura y Modelado en Objetos**

---

## Índice

1. [Descripción del sistema](#1-descripción-del-sistema)
2. [Cómo ejecutar](#2-cómo-ejecutar)
3. [Estructura del proyecto](#3-estructura-del-proyecto)
4. [Decisiones de diseño](#4-decisiones-de-diseño)
   - [4.1 Modelo de dominio](#41-modelo-de-dominio)
   - [4.2 Patrón State — Estados de la Donación](#42-patrón-state--estados-de-la-donación)
   - [4.3 Patrón Strategy — Canales de notificación](#43-patrón-strategy--canales-de-notificación)
   - [4.4 Patrón Observer — Notificaciones al cambiar estado](#44-patrón-observer--notificaciones-al-cambiar-estado)
   - [4.5 Patrón Template Method — Importación CSV](#45-patrón-template-method--importación-csv)
   - [4.6 Patrón Factory Method — Creación de personas desde CSV](#46-patrón-factory-method--creación-de-personas-desde-csv)
   - [4.7 Patrón Singleton — Repositorios en memoria](#47-patrón-singleton--repositorios-en-memoria)
   - [4.8 Patrón Builder — Construcción de Donaciones](#48-patrón-builder--construcción-de-donaciones)
5. [Lógica de segmentación](#5-lógica-de-segmentación)
6. [Simulación de notificaciones](#6-simulación-de-notificaciones)

---

## 1. Descripción del sistema

DonaTrack organiza y da trazabilidad a las donaciones de UTN Solidaria, desde su recepción en el depósito hasta su entrega a entidades beneficiarias. Esta primera entrega cubre:

- Registro de personas donantes (humanas y jurídicas)
- Ingreso y segmentación automática de donaciones por subcategoría
- Trazabilidad completa del ciclo de vida de cada donación
- Gestión de entidades beneficiarias y sus necesidades
- Importación masiva de donantes desde archivo CSV
- Notificaciones simuladas por email, SMS y WhatsApp

---

## 2. Cómo ejecutar

**Requisitos:** Java 17 · Maven 3.x

```bash
# Desde la carpeta TPA/
mvn compile
mvn exec:java -Dexec.mainClass="donatrack.Main"
```

El `Main` ejecuta seis demostraciones que cubren todos los requerimientos de la entrega.

---

## 3. Estructura del proyecto

```
src/main/java/donatrack/
├── model/
│   ├── persona/         → Persona (abstract), PersonaHumana, PersonaJuridica + enums
│   ├── entidad/         → EntidadBeneficiaria
│   ├── contacto/        → MedioContacto, TipoContacto
│   ├── usuario/         → Usuario
│   ├── donacion/        → Donacion, Bien, DonacionBuilder, Unidades
│   │   └── estado/      → EstadoDonacion (interfaz) + 7 estados concretos
│   ├── catalogo/        → Categoria, Subcategoria
│   └── necesidad/       → Necesidad
├── notificacion/        → Notificador, 3 implementaciones, Observer, Servicio
├── importacion/         → ImportadorCSV (abstract), ImportadorCSVPersonas, PersonaFactory
├── repositorio/         → RepositorioPersonas, RepositorioDonaciones, RepositorioEntidades
├── servicio/            → ServicioSegmentacion, ServicioDonaciones, ServicioPersonas
└── Main.java
```

---

## 4. Decisiones de diseño

### 4.1 Modelo de dominio

El modelo de clases respeta fielmente el diagrama de clases definido para la entrega (`PRIMERA ENTREGA/diagrama_de_clases.puml`). Las clases de dominio son:

| Clase | Responsabilidad |
|---|---|
| `Persona` (abstract) | Base común para humanas, jurídicas y entidades beneficiarias |
| `PersonaHumana` | Donante individual con datos personales y género |
| `PersonaJuridica` | Organización donante con razón social y representantes habilitados |
| `EntidadBeneficiaria` | Organización receptora; gestiona sus propias necesidades |
| `MedioContacto` | Canal de comunicación tipado (email, teléfono, WhatsApp) |
| `Donacion` | Unidad mínima de asignación agrupada por subcategoría |
| `Bien` | Ítem físico con descripción, subcategoría y unidad de medida |
| `Categoria` / `Subcategoria` | Jerarquía de clasificación de bienes |
| `Necesidad` | Requerimiento material de una entidad beneficiaria |

**Por qué `Persona` es abstracta:** las tres variantes (humana, jurídica, beneficiaria) comparten dirección, contactos y usuario, pero tienen identidad distinta. La abstracción evita duplicación y permite tratar a cualquier persona de forma polimórfica en el servicio de notificaciones.

**Por qué `EntidadBeneficiaria` extiende `Persona`:** el enunciado la define como un tipo de actor registrable con contactos y dirección. Reutilizar la jerarquía evita duplicar la gestión de medios de contacto y usuario, y mantiene coherencia con el diagrama.

---

### 4.2 Patrón State — Estados de la Donación

**Problema:** una `Donacion` atraviesa hasta siete estados con reglas estrictas sobre qué transiciones son válidas. Sin este patrón, `Donacion` acumularía condicionales `if/switch` que crecen con cada nuevo estado, violando el principio Abierto/Cerrado.

**Solución:** la interfaz `EstadoDonacion` define todas las operaciones posibles. Cada estado concreto implementa solo las transiciones que le corresponden y lanza `IllegalStateException` para las inválidas.

```
EN_DEPOSITO ──asignar()──► ASIGNACION_REALIZADA ──planificarRuta()──► LISTA_PARA_ENTREGAR
                                                                              │
                                                                    iniciarTraslado()
                                                                              │
                                                                          EN_TRASLADO
                                                                         /           \
                                                              confirmarEntrega()   fallarEntrega()
                                                                     │                   │
                                                                 ENTREGADA        ENTREGA_FALLIDA
                                                                                        │
                                                                              devolverAlDeposito()
                                                                                        │
                                                                                   EN_DEPOSITO

Cualquier estado ──vencer()──► VENCIDA  (acción del administrador)
```

**Por qué no un enum con switch:** agregar un nuevo estado requeriría modificar el switch en `Donacion`. Con el patrón State, se agrega una clase nueva sin tocar las existentes.

**Archivos:** `model/donacion/estado/EstadoDonacion.java` y las 7 clases concretas en el mismo paquete.

---

### 4.3 Patrón Strategy — Canales de notificación

**Problema:** el sistema debe enviar notificaciones por email, SMS y WhatsApp. Los tres canales son intercambiables y en futuras entregas se reemplazarán por integraciones reales. Si la lógica de selección del canal estuviera en `ServicioNotificaciones` con condicionales, agregar un canal nuevo implicaría modificar esa clase.

**Solución:** la interfaz `Notificador` declara un único método `notificar(destinatario, mensaje)`. Cada canal es una estrategia independiente. `ServicioNotificaciones` selecciona la estrategia en función del `TipoContacto` y la invoca sin saber qué implementación usa.

```java
interface Notificador {
    void notificar(String destinatario, String mensaje);
}
// NotificadorEmail · NotificadorSMS · NotificadorWhatsApp
```

**Por qué esto facilita la Entrega 2:** cuando se integren los servicios reales, solo se reemplaza el cuerpo de cada clase concreta. El resto del sistema no cambia.

**Archivos:** `notificacion/Notificador.java` y sus tres implementaciones.

---

### 4.4 Patrón Observer — Notificaciones al cambiar estado

**Problema:** cuando una donación cambia de estado, el donante debe recibir una notificación. Si `Donacion` invocara directamente `ServicioNotificaciones`, quedaría acoplada a la capa de notificaciones, dificultando pruebas y extensiones futuras (por ejemplo, notificar también a la entidad beneficiaria).

**Solución:** `Donacion` mantiene una lista de `DonacionObserver`. Al cambiar de estado llama `notificarObservers()`. El observador concreto `NotificadorDonacionObserver` recibe el evento y delega en `ServicioNotificaciones`.

```
Donacion (Subject)
    └── cambiarEstado() → notificarObservers()
                               └── DonacionObserver.onCambioEstado()
                                        └── NotificadorDonacionObserver
                                                 └── ServicioNotificaciones (Strategy)
```

**Beneficio:** agregar un segundo observador (por ejemplo, para auditoría o para notificar a la entidad beneficiaria) no modifica ni `Donacion` ni el observador existente.

**Archivos:** `notificacion/DonacionObserver.java`, `notificacion/NotificadorDonacionObserver.java`.

---

### 4.5 Patrón Template Method — Importación CSV

**Problema:** el proceso de importar cualquier tipo de entidad desde CSV sigue siempre el mismo flujo: abrir archivo, leer línea por línea, saltear cabecera, manejar errores. Solo varía qué objeto se construye a partir de cada línea.

**Solución:** `ImportadorCSV<T>` es una clase abstracta con el método `importar()` como template method (marcado `final` para que no se sobreescriba). El paso variable `procesarFila()` es abstracto y lo implementa cada subclase.

```java
// flujo fijo (no se puede sobreescribir)
public final List<T> importar(String rutaArchivo) { ... procesarFila(campos) ... }

// paso variable
protected abstract T procesarFila(String[] campos);
```

**Por qué `final`:** garantiza que ninguna subclase rompa el flujo de lectura del archivo (manejo de errores por fila, salto de cabecera, cierre del stream).

**Extensibilidad:** para importar entidades beneficiarias desde CSV bastará con crear `ImportadorCSVEntidades extends ImportadorCSV<EntidadBeneficiaria>` sin duplicar la lógica de lectura.

**Archivos:** `importacion/ImportadorCSV.java`, `importacion/ImportadorCSVPersonas.java`.

---

### 4.6 Patrón Factory Method — Creación de personas desde CSV

**Problema:** cada línea del CSV puede corresponder a una `PersonaHumana` o a una `PersonaJuridica`. `ImportadorCSVPersonas` no debe saber cómo construir cada tipo; esa responsabilidad no le pertenece.

**Solución:** `PersonaFactory.crear(String[] campos)` encapsula la decisión. Lee el campo `TipoPersona` y devuelve la instancia correcta. El importador solo llama al factory y recibe una `Persona`.

```java
return switch (campos[0].toUpperCase()) {
    case "HUMANA"   -> crearPersonaHumana(campos);
    case "JURIDICA" -> crearPersonaJuridica(campos);
    default -> throw new IllegalArgumentException(...);
};
```

**Por qué no un constructor con parámetros:** la construcción de cada tipo involucra lógica distinta (parseo del nombre, asignación de tipo de organización). Centralizar eso en el factory evita que esa lógica se disperse.

**Archivos:** `importacion/PersonaFactory.java`.

---

### 4.7 Patrón Singleton — Repositorios en memoria

**Problema:** en esta entrega no se utiliza base de datos. Los repositorios son mapas en memoria y deben ser compartidos por todos los servicios para que los datos persistan durante la ejecución. Si cada servicio instanciara su propio repositorio, operarían sobre colecciones independientes.

**Solución:** cada repositorio expone un único punto de acceso `getInstance()`. La instancia se crea la primera vez que se solicita (lazy initialization).

```java
public static RepositorioPersonas getInstance() {
    if (instancia == null) instancia = new RepositorioPersonas();
    return instancia;
}
```

**Por qué Singleton y no inyección de dependencias:** la entrega no usa frameworks. Singleton es la alternativa más directa para garantizar instancia única sin Spring ni Guice. En entregas futuras con persistencia real, los repositorios se reemplazarán por implementaciones con JPA/JDBC y el Singleton desaparecerá sin afectar al resto del sistema.

**Archivos:** `repositorio/RepositorioPersonas.java`, `RepositorioDonaciones.java`, `RepositorioEntidades.java`.

---

### 4.8 Patrón Builder — Construcción de Donaciones

**Problema:** `Donacion` requiere al menos una subcategoría y una lista de bienes. El servicio de segmentación construye donaciones en un loop, y el constructor telescópico (`new Donacion(sub, bienes, estado, ...)`) se vuelve confuso cuando los parámetros crecen.

**Solución:** `DonacionBuilder` expone una API fluida y valida las precondiciones antes de construir.

```java
Donacion d = new DonacionBuilder()
    .conSubcategoria(subcategoria)
    .agregarBienes(listaBienes)
    .build();
```

**Por qué el Builder valida:** `build()` lanza `IllegalStateException` si falta la subcategoría o la lista está vacía. Esto hace que el error sea detectado en el momento de construcción y no en algún método posterior.

**Archivos:** `model/donacion/DonacionBuilder.java`.

---

## 5. Lógica de segmentación

Cuando un donante ingresa bienes, el sistema no crea una única donación con todo. El enunciado establece que la subcategoría es la unidad mínima de asignación. `ServicioSegmentacion` agrupa los bienes por subcategoría y genera una `Donacion` independiente por grupo.

**Ejemplo:**
```
Entrada: [silla, silla, mesa, fideos, tomate]
         ─────   ─────  ────  ──────  ──────
         sillas sillas mesas fideos  tomate

Salida: Donacion(sillas, 2 bienes)   → EN_DEPOSITO
        Donacion(mesas, 1 bien)      → EN_DEPOSITO
        Donacion(fideos, 1 bien)     → EN_DEPOSITO
        Donacion(tomate, 1 bien)     → EN_DEPOSITO
```

Cada `Donacion` resultante queda registrada en `RepositorioDonaciones` y lista para el algoritmo de asignación de la Entrega 2.

---

## 6. Simulación de notificaciones

En esta entrega los tres canales imprimen en consola en lugar de llamar a servicios externos. Esto fue una decisión deliberada: el enunciado indica explícitamente que "los envíos son simulados" en la Entrega 1.

La arquitectura Strategy + Observer garantiza que cuando llegue la integración real, solo sea necesario reemplazar el cuerpo de `NotificadorEmail`, `NotificadorSMS` y `NotificadorWhatsApp` sin modificar ninguna otra clase del sistema.
