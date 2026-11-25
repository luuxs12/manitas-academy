/* Modales pequeños del panel: confirmación y avisos. El <dialog> nativo da
   foco atrapado y Escape; acá solo se carga el contenido real y se devuelve
   el foco al disparador cuando se cierra. */

(() => {
    const confirm = document.getElementById("confirm-dialog");
    if (!confirm) return;

    const title = confirm.querySelector("[data-confirm-title]");
    const message = confirm.querySelector("[data-confirm-message]");
    const details = confirm.querySelector("[data-confirm-details]");
    let pending = null;
    let lastTrigger = null;

    const show = (dialog, trigger) => {
        if (!dialog || typeof dialog.showModal !== "function" || dialog.open) return;
        lastTrigger = trigger ?? null;
        dialog.showModal();
        dialog.querySelector("[data-dialog-close]")?.focus();
    };

    const fill = (heading, text, items) => {
        if (title) title.textContent = heading || "Confirmar acción";
        if (message) message.textContent = text || "Revisá los datos antes de continuar.";
        if (!details) return;
        details.replaceChildren();
        for (const item of items ?? []) {
            const li = document.createElement("li");
            li.textContent = item;
            details.append(li);
        }
        details.hidden = !(items?.length);
    };

    // El aviso llega en el flash del servidor: se muestra una vez y se cierra.
    const notice = document.getElementById("success-dialog")
        || document.getElementById("error-dialog");
    show(notice);

    // Borrados: la página de respaldo trae el impacto real, la acción y el CSRF.
    const openDeletion = async (trigger) => {
        const url = trigger.dataset.confirmUrl;
        if (!url) return;
        try {
            const response = await fetch(url, {
                headers: { "X-Requested-With": "XMLHttpRequest" }
            });
            if (!response.ok) throw new Error("No se pudo abrir la confirmación");
            const page = new DOMParser().parseFromString(await response.text(), "text/html");
            const panel = page.getElementById("confirm-panel");
            const form = panel?.querySelector("form[method='post']");
            if (!panel || !form) throw new Error("La confirmación no trae datos");

            fill(
                panel.querySelector("h1")?.textContent?.trim(),
                panel.querySelector("#confirm-dialog-message")?.textContent?.trim(),
                [...panel.querySelectorAll(".admin-confirm-details li")]
                    .map((li) => li.textContent.trim())
            );

            // Se clona el formulario renderizado: conserva la acción y el token
            // CSRF reales sin que el script tenga que reconocer sus campos.
            pending = () => {
                const sender = document.importNode(form, true);
                sender.hidden = true;
                document.body.append(sender);
                sender.submit();
            };
            show(confirm, trigger);
        } catch (error) {
            pending = null;
            window.location.assign(url);
        }
    };

    // Formularios: se confirma antes de enviar y se deja el formulario original
    // tal cual, con sus datos y su validación.
    let confirmedForm = null;
    document.addEventListener("submit", (event) => {
        const form = event.target;
        if (!(form instanceof HTMLFormElement) || !form.hasAttribute("data-confirm-form")) return;
        if (form === confirmedForm) {
            confirmedForm = null;
            return;
        }
        event.preventDefault();
        fill("Confirmar acción", form.dataset.confirmMessage);
        pending = () => {
            confirmedForm = form;
            form.requestSubmit();
        };
        show(confirm, form.querySelector("button[type='submit']"));
    });

    document.addEventListener("click", (event) => {
        const target = event.target instanceof Element ? event.target : null;
        if (!target) return;

        const close = target.closest("[data-dialog-close]");
        if (close) {
            const dialog = close.closest("dialog");
            // Fuera del modal, "Cancelar y volver" sigue siendo un enlace normal.
            if (dialog) {
                event.preventDefault();
                dialog.close();
            }
            return;
        }

        if (target.closest("[data-confirm-submit]")) {
            event.preventDefault();
            const action = pending;
            pending = null;
            confirm.close();
            action?.();
            return;
        }

        const trigger = target.closest("[data-confirm-url]");
        if (trigger) {
            event.preventDefault();
            openDeletion(trigger);
        }
    });

    // Clic en el fondo cierra, igual que Escape en un <dialog> nativo.
    document.addEventListener("click", (event) => {
        if (event.target instanceof HTMLDialogElement) event.target.close();
    });

    for (const dialog of document.querySelectorAll("dialog.ui-dialog")) {
        dialog.addEventListener("close", () => {
            if (dialog !== confirm) return;
            lastTrigger?.focus();
            lastTrigger = null;
        });
    }
})();
