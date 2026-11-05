
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

    document.querySelectorAll(".demo-form").forEach(form =>
        form.addEventListener("submit", e => {
            e.preventDefault();
            toast("Formulario de demostración. Conectá esta acción con el backend Javalin.");
            form.reset();
            form.closest(".modal")?.classList.remove("open");
        })
    );

    // MENÚ MÓVIL
    document.querySelector(".mobile-menu")?.addEventListener("click", () =>
        document.body.classList.toggle("menu-open")
    );

    // MAPAS

    // MAPAS
    if (
        page === "public-donations" ||
        page === "tracking-donor" ||
        page === "tracking-beneficiary"
    ) {
        const id = page === "public-donations"
            ? "donations-map"
            : "tracking-map";

        const map = L.map(id).setView([-34.70, -58.39], 11);

        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        }).addTo(map);

        if (page === "public-donations") {

            const donations = [
                {
                    lat: -34.706,
                    lng: -58.392,
                    title: "Frutas y verduras",
                    institution: "Comedor Los Pinos",
                    location: "Lanús",
                    image: "assets/img/entrega-01.jpg"
                },
                {
                    lat: -34.662,
                    lng: -58.365,
                    title: "Ropa de lujosa",
                    institution: "Casa de los estudiantes",
                    location: "Avellaneda",
                    image: "assets/img/entrega-02.jpg"
                },
                {
                    lat: -34.760,
                    lng: -58.405,
                    title: "Productos de higiene",
                    institution: "Centro La Esperanza",
                    location: "Lomas de Zamora",
                    image: "assets/img/entrega-03.jpg"
                }
            ];

            donations.forEach(donation => {
                const popupContent = `
                    <div class="donation-popup">
                        <img
                            src="${donation.image}"
                            alt="${donation.title}"
                            class="donation-popup-image"
                        >

                        <div class="donation-popup-content">
                            <span class="status-pill success">
                                ✓ Entrega confirmada
                            </span>

                            <h3>${donation.title}</h3>
                            <p>${donation.institution}</p>
                            <small>${donation.location}</small>
                        </div>
                    </div>
                `;

                L.marker([donation.lat, donation.lng])
                    .addTo(map)
                    .bindPopup(popupContent);
            });

        } else {
            const points = [
                [
                    -34.70,
                    -58.39,
                    "<b>Camión DT-04</b><br>En camino a Fundación Puentes<br>Última actualización: 10:15"
                ]
            ];

            points.forEach(p => {
                L.marker([p[0], p[1]])
                    .addTo(map)
                    .bindPopup(p[2]);
            });

            L.polyline([
                [-34.735, -58.43],
                [-34.72, -58.415],
                [-34.70, -58.39],
                [-34.68, -58.37]
            ], {
                color: "#27835a",
                weight: 4,
                dashArray: "7 8"
            }).addTo(map);
        }

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
    const slides = document.querySelectorAll(".delivery-slide");
    const dots = document.querySelectorAll(".carousel-dot");
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

            dots.forEach((dot, i) => {
                dot.classList.toggle("active", i === currentSlide);
            });

            counter.textContent =
                `0${currentSlide + 1} / 0${slides.length}`;
        }

        prev.addEventListener("click", () => showSlide(currentSlide - 1));
        next.addEventListener("click", () => showSlide(currentSlide + 1));

        dots.forEach((dot, index) => {
            dot.addEventListener("click", () => showSlide(index));
        });

        showSlide(0);
    }
});
