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

    // ALERTAS FIJAS (abajo a la derecha): se cierran solas a los 6 segundos o con el x.
    const cerrarAlerta = el => {
        if (!el || el.classList.contains("oculta")) return;
        el.classList.add("oculta");
        setTimeout(() => el.remove(), 220);
    };
    document.querySelectorAll("[data-alerta]").forEach(el => {
        el.querySelector("[data-alerta-cerrar]")?.addEventListener("click", () => cerrarAlerta(el));
        setTimeout(() => cerrarAlerta(el), 6000);
    });

    // MENÚ MÓVIL
    document.querySelector(".mobile-menu")?.addEventListener("click", () =>
        document.body.classList.toggle("menu-open")
    );

    // MAPA DE DONACIONES PUBLICAS
    // Solo el mapa base: las entregas todavia no traen coordenadas, y la pantalla lo
    // aclara. Los mapas de seguimiento los arma mapa-seguimiento.js con datos reales.
    if (page === "public-donations" && document.getElementById("donations-map")) {
        const map = L.map("donations-map").setView([-34.70, -58.39], 11);

        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        }).addTo(map);

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
            setTimeout(() => cerrarAlerta(alerta), 6000);
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
