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
        if (event.source !== frame.contentWindow) return;
        let data = event.data;
        if (typeof data === "string") {
            try {
                data = JSON.parse(data);
            } catch (error) {
                return;
            }
        }
        if (data && data.event === "onStateChange" && data.info === 0) {
            hint.textContent = "El video terminó. Si lo viste, marcá la clase como vista.";
        }
    });
})();

/* Visor de PDF del temario. El archivo se pide con la sesión y se muestra
   desde un blob: así no se enmarca la respuesta privada, que el sitio sirve
   con X-Frame-Options: DENY. */
(() => {
    const frame = document.querySelector("[data-pdf-frame]");
    if (!frame) return;

    const url = frame.dataset.pdfUrl;
    const status = document.querySelector("[data-pdf-status]");
    let objectUrl = null;

    const say = (text) => {
        if (status) status.textContent = text;
    };

    if (!url) {
        say("No se pudo abrir el documento.");
        return;
    }

    say("Cargando documento…");

    fetch(url, { credentials: "same-origin" })
        .then((response) => {
            if (!response.ok) throw new Error("No se pudo abrir el documento");
            return response.blob();
        })
        .then((blob) => {
            objectUrl = URL.createObjectURL(blob);
            frame.src = objectUrl;
            say("Documento listo para leer.");
        })
        .catch(() => {
            say("No se pudo abrir el documento. Probá descargarlo.");
        });

    window.addEventListener("pagehide", () => {
        if (objectUrl) URL.revokeObjectURL(objectUrl);
    });
})();
