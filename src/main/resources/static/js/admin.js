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

