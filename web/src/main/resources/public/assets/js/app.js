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

    // MAPA PÚBLICO
    if (page === "public-donations") {
        const map = L.map("donations-map").setView([-34.70, -58.39], 11);

        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        }).addTo(map);

        setTimeout(() => map.invalidateSize(), 250);
    }



    // IMPORTACIÓN CSV
    const csv = document.getElementById("process-csv");

    csv?.addEventListener("click", () => {
        const file = document.getElementById("csv-file").files[0];
        const out = document.getElementById("csv-feedback");

        if (!file) {
            out.textContent = "Seleccioná un archivo CSV antes de continuar.";
            return;
        }

        if (!file.name.toLowerCase().endsWith(".csv")) {
            out.textContent = "El archivo debe tener extensión .csv.";
            return;
        }

        out.textContent = `Archivo seleccionado: ${file.name} (${(file.size / 1024).toFixed(1)} KB). La validación de filas y la importación real se realizarán en el backend.`;
    });

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
