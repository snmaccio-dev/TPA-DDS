document.addEventListener("DOMContentLoaded", () => {
    const page = document.body.dataset.page;

    // NOTIFICACIONES
    const toast = (msg) => {
        const el = document.createElement("div");
        el.className = "toast";
        el.textContent = msg;
        document.body.appendChild(el);
        setTimeout(() => el.remove(), 3200);
    };

    document.querySelectorAll("[data-toast]").forEach(el =>
        el.addEventListener("click", e => {
            e.preventDefault();
            toast(el.dataset.toast);
        })
    );

    // MODALES
    document.querySelectorAll("[data-open-modal]").forEach(el =>
        el.addEventListener("click", () =>
            document.getElementById(el.dataset.openModal)?.classList.add("open")
        )
    );

    document.querySelectorAll("[data-close-modal]").forEach(el =>
        el.addEventListener("click", () =>
            el.closest(".modal")?.classList.remove("open")
        )
    );

    document.querySelectorAll(".modal").forEach(el =>
        el.addEventListener("click", e => {
            if (e.target === el) el.classList.remove("open");
        })
    );

    // ALERTAS FIJAS (abajo a la derecha): las de error quedan hasta que se cierran con
    // la x; las demas (exito, advertencia) se cierran solas a los 6 segundos.
    const cerrarAlerta = el => {
        if (!el || el.classList.contains("oculta")) return;
        el.classList.add("oculta");
        setTimeout(() => el.remove(), 220);
    };
    document.querySelectorAll("[data-alerta]").forEach(el => {
        el.querySelector("[data-alerta-cerrar]")?.addEventListener("click", () => cerrarAlerta(el));
        if (!el.classList.contains("error")) {
            setTimeout(() => cerrarAlerta(el), 6000);
        }
    });

    // MENÚ MÓVIL
    document.querySelector(".mobile-menu")?.addEventListener("click", () =>
        document.body.classList.toggle("menu-open")
    );

    // MAPA DE DONACIONES PUBLICAS
    // Los datos los arma el servidor (#mapa-entregas) con las ultimas 5 entregas
    // confirmadas: un marcador por entidad y, en su popup, solo sus entregas de esas 5. El popup
    // se arma con nodos del DOM (textContent), nunca con HTML de los datos.
    if (page === "public-donations" && document.getElementById("donations-map")
        && typeof L !== "undefined") {
        let marcadoresDatos = [];
        try {
            const datos = document.getElementById("mapa-entregas");
            marcadoresDatos = JSON.parse(datos ? datos.textContent : "[]") || [];
        } catch (e) {
            marcadoresDatos = [];
        }

        const map = L.map("donations-map").setView([-34.61, -58.42], 12);

        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        }).addTo(map);

        // Solo fotos http(s) (o relativas al sitio): nada de javascript: ni data:.
        const urlDeFoto = (foto) => {
            if (!foto) return null;
            try {
                const url = new URL(foto, window.location.href);
                return url.protocol === "http:" || url.protocol === "https:" ? url.href : null;
            } catch (e) {
                return null;
            }
        };

        const texto = (etiqueta, contenido, clase) => {
            const el = document.createElement(etiqueta);
            el.textContent = contenido;
            if (clase) el.className = clase;
            return el;
        };

        const popupDe = (marcador) => {
            const cuerpo = document.createElement("div");
            cuerpo.className = "popup-entregas";
            cuerpo.appendChild(texto("b", marcador.razonSocial, "popup-entregas-titulo"));
            cuerpo.appendChild(texto("small",
                marcador.entregas.length === 1
                    ? "1 entrega confirmada"
                    : marcador.entregas.length + " entregas confirmadas",
                "popup-entregas-cantidad"));

            const lista = document.createElement("ul");
            marcador.entregas.forEach(entrega => {
                const item = document.createElement("li");
                const foto = urlDeFoto(entrega.foto);
                if (foto) {
                    const img = document.createElement("img");
                    img.src = foto;
                    img.alt = "Fotografía de " + entrega.descripcion;
                    img.loading = "lazy";
                    item.appendChild(img);
                }
                const detalle = document.createElement("div");
                detalle.appendChild(texto("span", entrega.descripcion, "popup-entrega-descripcion"));
                detalle.appendChild(texto("small",
                    entrega.cantidad + (entrega.fecha ? " · " + entrega.fecha : "")));
                item.appendChild(detalle);
                lista.appendChild(item);
            });
            cuerpo.appendChild(lista);
            return cuerpo;
        };

        const marcadores = marcadoresDatos.map(marcador =>
            L.marker([marcador.latitud, marcador.longitud], { title: marcador.razonSocial })
                .addTo(map)
                .bindPopup(popupDe(marcador), { maxWidth: 280, maxHeight: 320 })
        );

        // Igual que mapa-seguimiento.js: el mapa se ajusta a los marcadores.
        if (marcadores.length === 1) {
            map.setView(marcadores[0].getLatLng(), 14);
        } else if (marcadores.length > 1) {
            map.fitBounds(L.featureGroup(marcadores).getBounds().pad(0.2));
        }

        setTimeout(() => map.invalidateSize(), 250);
    }

    // IMPORTACIÓN CSV
    // El archivo se sube con fetch; la lista "Archivos subidos" se actualiza sin recargar.
    const csvForm = document.getElementById("csv-form");
    const csvLista = document.getElementById("csv-importaciones");

    if (csvForm && csvLista) {
        const CONSULTA_MS = 3000;

        // Misma estructura que el partial de alerta.
        const mostrarAlertaCsv = (texto) => {
            const alerta = document.createElement("div");
            alerta.className = "alerta error";
            alerta.setAttribute("role", "alert");
            alerta.innerHTML =
                '<span class="alerta-icono" aria-hidden="true">⚠</span>' +
                '<span class="alerta-texto"></span>' +
                '<button type="button" class="alerta-cerrar" aria-label="Cerrar alerta">×</button>';
            alerta.querySelector(".alerta-texto").textContent = texto;
            alerta.querySelector(".alerta-cerrar").addEventListener("click", () => cerrarAlerta(alerta));
            document.body.appendChild(alerta);
        };

        // Respuesta JSON de web; si no es JSON (p. ej. redirect al login) se informa igual.
        const leerJson = async (respuesta) => {
            try {
                return await respuesta.json();
            } catch (e) {
                return { mensaje: "No se pudo completar la operación. Recargá la página." };
            }
        };

        const actualizarFila = (fila, datos) => {
            const estado = fila.querySelector("[data-estado]");
            estado.className = "status-pill " + datos.estadoClase;
            estado.textContent = datos.estadoTexto;
            fila.querySelector("[data-motivo]").textContent = datos.motivoFallo || "";
            const fecha = fila.querySelector("[data-fecha]");
            if (fecha.textContent.trim() === "") fecha.textContent = datos.fechaTexto;
        };

        // Consulta el estado de una fila en curso cada 3 segundos hasta que termine.
        const seguir = (fila, espera = CONSULTA_MS) => {
            setTimeout(async () => {
                try {
                    const respuesta = await fetch(
                        "/importar-csv/" + fila.dataset.importacion + "/estado",
                        { headers: { "Accept": "application/json" } });
                    if (respuesta.ok) {
                        const datos = await respuesta.json();
                        actualizarFila(fila, datos);
                        if (!datos.enCurso) return;
                    }
                } catch (e) {
                    // Sin respuesta: se vuelve a intentar en la proxima consulta.
                }
                seguir(fila);
            }, espera);
        };

        csvLista.querySelectorAll("tr[data-en-curso]").forEach(fila => seguir(fila));

        csvForm.addEventListener("submit", async (e) => {
            e.preventDefault();

            const input = document.getElementById("csv-file");
            const archivo = input.files[0];

            if (!archivo) {
                mostrarAlertaCsv("Seleccioná un archivo CSV antes de subirlo.");
                return;
            }
            if (!archivo.name.toLowerCase().endsWith(".csv")) {
                mostrarAlertaCsv("El archivo tiene que tener extensión .csv.");
                return;
            }

            const boton = csvForm.querySelector("button[type=submit]");
            boton.disabled = true;

            try {
                const datos = new FormData();
                datos.append("archivo", archivo);

                const respuesta = await fetch("/importar-csv", {
                    method: "POST",
                    body: datos,
                    headers: { "Accept": "application/json" }
                });
                const cuerpo = await leerJson(respuesta);

                if (!respuesta.ok || cuerpo.id === undefined) {
                    mostrarAlertaCsv(cuerpo.mensaje || "No se pudo subir el archivo.");
                    return;
                }

                csvLista.querySelectorAll("[data-sin-importaciones]").forEach(f => f.remove());

                const fila = document.createElement("tr");
                fila.dataset.importacion = cuerpo.id;
                fila.innerHTML =
                    '<td></td><td data-fecha></td>' +
                    '<td><span class="status-pill info" data-estado>En curso</span> ' +
                    '<small class="muted" data-motivo></small></td>';
                fila.firstChild.textContent = archivo.name;
                csvLista.prepend(fila);
                input.value = "";

                // La primera consulta sale enseguida para traer la fecha que registró el backend.
                seguir(fila, 0);
            } catch (err) {
                mostrarAlertaCsv("El servicio no está disponible, intentá de nuevo en unos minutos.");
            } finally {
                boton.disabled = false;
            }
        });
    }

    // CARRUSEL DE DONACIONES

    // CARRUSEL DE DONACIONES
    const slides = document.querySelectorAll(".delivery-slide");
    const counter = document.getElementById("carouselCounter");
    const prev = document.getElementById("prevDelivery");
    const next = document.getElementById("nextDelivery");

    if (slides.length > 0 && prev && next && counter) {
        let currentSlide = 0;

        function showSlide(index) {
            currentSlide = (index + slides.length) % slides.length;

            slides.forEach((slide, i) => {
                slide.classList.toggle("active", i === currentSlide);
            });

            counter.textContent =
                `${currentSlide + 1} / ${slides.length}`;
        }

        prev.addEventListener("click", () => showSlide(currentSlide - 1));
        next.addEventListener("click", () => showSlide(currentSlide + 1));

        showSlide(0);
    }

});
