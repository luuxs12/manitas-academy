/* Menús del panel (cuenta y notificaciones): se cierran al tocar fuera
   o con Escape. */

(() => {
    const menus = [...document.querySelectorAll("[data-panel-menu]")];
    if (!menus.length) return;

    const closeAll = (except) => {
        for (const menu of menus) {
            if (menu !== except) menu.open = false;
        }
    };

    document.addEventListener("click", (event) => {
        for (const menu of menus) {
            if (menu.open && !menu.contains(event.target)) {
                menu.open = false;
            }
        }
    });

    for (const menu of menus) {
        menu.addEventListener("keydown", (event) => {
            if (event.key !== "Escape") return;
            menu.open = false;
            menu.querySelector("summary")?.focus();
        });

        // Al abrir uno, el otro se cierra: dos popovers a la vez se pisan.
        menu.addEventListener("toggle", () => {
            if (menu.open) closeAll(menu);
        });
    }
})();

/* El certificado se imprime (o se guarda en PDF) desde el navegador. */
(() => {
    for (const button of document.querySelectorAll("[data-print]")) {
        button.addEventListener("click", () => window.print());
    }
})();
