document.addEventListener("DOMContentLoaded", () => {
    const grid = document.getElementById("deliveries-grid");

    if (!grid) return;

    const cards = Array.from(
        grid.querySelectorAll(".delivery-card")
    );

    const pagination = document.getElementById("deliveries-pagination");
    const prev = document.getElementById("deliveries-prev");
    const next = document.getElementById("deliveries-next");
    const pageInfo = document.getElementById("deliveries-page-info");

    const pageSize = 3;
    const totalPages = Math.ceil(cards.length / pageSize);

    let currentPage = 0;

    if (!pagination || !prev || !next || !pageInfo) {
        return;
    }

    function renderPage() {
        const start = currentPage * pageSize;
        const end = start + pageSize;

        cards.forEach((card, index) => {
            card.hidden = index < start || index >= end;
        });

        pageInfo.textContent =
            `Página ${currentPage + 1} de ${totalPages}`;

        prev.disabled = currentPage === 0;
        next.disabled = currentPage >= totalPages - 1;

        pagination.hidden = totalPages <= 1;
    }

    prev.addEventListener("click", () => {
        if (currentPage > 0) {
            currentPage--;
            renderPage();
        }
    });

    next.addEventListener("click", () => {
        if (currentPage < totalPages - 1) {
            currentPage++;
            renderPage();
        }
    });

    renderPage();
});
