/* Menú de cuenta del panel: se cierra al tocar fuera o con Escape. */

(() => {
    const menu = document.querySelector("[data-panel-menu]");
    if (!menu) return;

    const summary = menu.querySelector("summary");

    document.addEventListener("click", (event) => {
        if (menu.open && !menu.contains(event.target)) {
            menu.open = false;
        }
    });

    menu.addEventListener("keydown", (event) => {
        if (event.key !== "Escape") return;
        menu.open = false;
        summary?.focus();
    });
})();
