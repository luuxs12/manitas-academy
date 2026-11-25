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

/* YouTube informa el fin de la clase por postMessage. Es una pista, no una
   acción: marcar como visto sigue siendo una decisión confirmada con el botón. */
(() => {
    const hint = document.querySelector("[data-video-end-hint]");
    const frame = document.querySelector("[data-video-frame]");
    if (!hint || !frame) return;

    const allowed = ["https://www.youtube-nocookie.com", "https://www.youtube.com"];
    window.addEventListener("message", (event) => {
        if (!allowed.includes(event.origin)) return;
        let data = event.data;
        if (typeof data === "string") {
            try {
                data = JSON.parse(data);
            } catch (error) {
                return;
            }
        }
        if (data && data.event === "onStateChange" && data.info === 0) {
            hint.hidden = false;
        }
    });
})();
