/* Subida de archivos a R2: completa la URL del formulario con la respuesta real
   del servidor. Si R2 no está configurado, el mensaje lo dice sin fingir éxito. */
(() => {
    for (const zone of document.querySelectorAll("[data-upload]")) {
        const input = zone.querySelector("[data-upload-file]");
        const status = zone.querySelector("[data-upload-status]");
        const target = document.querySelector(zone.dataset.uploadTarget);
        const field = zone.dataset.uploadField || "url";
        const csrf = zone.querySelector("[data-upload-csrf]")?.value || "";
        if (!input || !zone.dataset.uploadEndpoint) continue;

        input.addEventListener("change", async () => {
            const file = input.files?.[0];
            if (!file) return;
            if (status) status.textContent = "Subiendo archivo…";

            const body = new FormData();
            body.append("archivo", file);
            try {
                const response = await fetch(zone.dataset.uploadEndpoint, {
                    method: "POST",
                    headers: { "X-CSRF-TOKEN": csrf, Accept: "application/json" },
                    body
                });
                const data = await response.json();
                if (!response.ok) {
                    throw new Error(data.error || "No se pudo subir el archivo.");
                }
                if (target) {
                    target.value = data[field] || "";
                    target.dispatchEvent(new Event("change", { bubbles: true }));
                }
                if (status) status.textContent = "Archivo guardado en R2.";
            } catch (error) {
                if (status) status.textContent = error.message || "No se pudo subir el archivo.";
            }
        });
    }
})();

/* Notificaciones del panel administrativo por polling: se consulta la API real
   y solo se muestran hechos ocurridos (auditoría y pagos confirmados). */

(() => {
    const main = document.querySelector("[data-admin-since]");
    const list = document.querySelector("[data-admin-events]");
    const empty = document.querySelector("[data-admin-empty]");
    const badge = document.getElementById("admin-notify-badge");
    if (!list || !badge) return;

    const initial = main?.dataset.adminSince;
    const parsed = initial ? new Date(initial) : new Date(Date.now() - 24 * 60 * 60 * 1000);
    let since = Number.isNaN(parsed.getTime())
        ? new Date(Date.now() - 24 * 60 * 60 * 1000)
        : parsed;
    let busy = false;

    const render = (events, unread) => {
        list.replaceChildren();
        for (const event of events) {
            const item = document.createElement("li");
            item.className = "panel-notify-item";

            const title = document.createElement("strong");
            title.textContent = event.title || "Actividad";
            const detail = document.createElement("span");
            detail.textContent = event.detail || "";
            const when = document.createElement("span");
            when.className = "admin-event-when";
            when.textContent = event.when || "";

            item.append(title, detail, when);
            list.append(item);
        }
        if (empty) empty.hidden = events.length > 0;
        badge.textContent = unread > 9 ? "9+" : String(unread);
        badge.hidden = unread === 0;
    };

    const refresh = async () => {
        if (busy) return;
        busy = true;
        try {
            const url = "/admin/api/notificaciones?desde=" + encodeURIComponent(since.toISOString());
            const response = await fetch(url, { headers: { Accept: "application/json" } });
            if (!response.ok) return;
            const data = await response.json();
            render(Array.isArray(data.eventos) ? data.eventos : [], Number(data.sinLeer) || 0);
            if (data.servidor) {
                const server = new Date(data.servidor);
                if (!Number.isNaN(server.getTime())) since = server;
            }
        } catch (_) {
            /* Sin conexión: se conserva el último estado conocido. */
        } finally {
            busy = false;
        }
    };

    refresh();
    setInterval(refresh, 30000);
    document.addEventListener("visibilitychange", () => {
        if (!document.hidden) refresh();
    });
})();

/* Confirmación accesible: un solo diálogo modal para todos los borrados.
   El formulario se envía únicamente cuando la persona confirma; Escape y
   Cancelar lo dejan intacto y el foco vuelve al control que abrió el diálogo. */
(() => {
    const dialog = document.getElementById("admin-confirm-dialog");
    if (!dialog) return;

    const message = dialog.querySelector("[data-confirm-message]");
    const accept = dialog.querySelector("[data-confirm-accept]");
    const cancel = dialog.querySelector("[data-confirm-cancel]");
    let pending = null;
    let opener = null;

    for (const form of document.querySelectorAll("form[data-confirm]")) {
        form.addEventListener("submit", (event) => {
            event.preventDefault();
            pending = form;
            opener = document.activeElement;
            message.textContent = form.dataset.confirm || "¿Continuar?";
            accept.textContent = form.dataset.confirmAccept || "Eliminar";
            dialog.showModal();
            cancel.focus();
        });
    }

    cancel.addEventListener("click", () => dialog.close());
    accept.addEventListener("click", () => {
        const form = pending;
        pending = null;
        dialog.close();
        if (form) form.submit();
    });
    dialog.addEventListener("close", () => {
        pending = null;
        if (opener && typeof opener.focus === "function") opener.focus();
        opener = null;
    });
})();
