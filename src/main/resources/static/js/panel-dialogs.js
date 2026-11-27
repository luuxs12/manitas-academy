/* Modales pequeños del panel: confirmación y avisos. El <dialog> nativo da
   foco atrapado y Escape; acá solo se carga el contenido real y se devuelve
   el foco al disparador cuando se cierra. */

(() => {
    const confirm = document.getElementById("confirm-dialog");
    if (!confirm) return;

    const actions = document.getElementById("actions-dialog");
    const actionsTitle = actions?.querySelector("[data-actions-title]");
    const actionsMessage = actions?.querySelector("[data-actions-message]");
    const actionsOptions = actions?.querySelector("[data-actions-options]");

    const title = confirm.querySelector("[data-confirm-title]");
    const message = confirm.querySelector("[data-confirm-message]");
    const details = confirm.querySelector("[data-confirm-details]");
    let pending = null;
    let lastTrigger = null;

    const show = (dialog, trigger) => {
        if (!dialog || typeof dialog.showModal !== "function" || dialog.open) return;
        if (trigger !== undefined) lastTrigger = trigger ?? null;
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
    const openDeletion = async (trigger, focusTarget = trigger) => {
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
            show(confirm, focusTarget);
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
        show(confirm, form.querySelector("button[type='submit']") || undefined);
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
            if (actions?.contains(trigger)) actions.close();
            const origin = actions?.contains(trigger)
                ? trigger.closest("[data-actions]")?.querySelector("[data-actions-trigger]") : trigger;
            openDeletion(trigger, origin);
        }
    });

    // Acciones del temario: un disparador por fila y un modal con solo las
    // opciones que esa fila tiene. Nada se envía hasta elegir una opción.
    document.addEventListener("click", (event) => {
        const trigger = event.target instanceof Element
            ? event.target.closest("[data-actions-trigger]") : null;
        if (!trigger || !actions || !actionsOptions) return;

        event.preventDefault();
        const container = trigger.closest("[data-actions]");
        if (!container) return;

        if (actionsTitle) actionsTitle.textContent = container.dataset.actionsTitle || "Acciones";
        const message = container.dataset.actionsMessage || "";
        if (actionsMessage) {
            actionsMessage.textContent = message;
            actionsMessage.hidden = !message;
        }

        actionsOptions.replaceChildren();
        for (const option of container.querySelectorAll("[data-action-label]")) {
            const label = option.dataset.actionLabel;
            let button;
            if (option.dataset.actionHref) {
                button = document.createElement("a");
                button.href = option.dataset.actionHref;
                button.className = "btn btn-ghost";
            } else {
                button = document.createElement("button");
                button.type = "button";
                button.className = option.classList.contains("admin-confirm-link")
                    ? "btn btn-danger" : "btn btn-ghost";
                if (option.dataset.actionConfirm === "form") {
                    button.dataset.actionConfirm = "form";
                    button.dataset.actionForm = option.dataset.actionForm || "";
                }
                if (option.dataset.confirmUrl) button.dataset.confirmUrl = option.dataset.confirmUrl;
            }
            const icon = option.querySelector(".ico");
            if (icon) button.append(icon.cloneNode(true));
            const text = document.createElement("span");
            text.textContent = label;
            button.append(text);
            const aria = option.getAttribute("aria-label");
            if (aria) button.setAttribute("aria-label", aria);
            actionsOptions.append(button);
        }
        show(actions, trigger);
    });

    document.addEventListener("click", (event) => {
        const button = event.target instanceof Element
            ? event.target.closest("[data-action-confirm='form']") : null;
        if (!button || !actions.contains(button)) return;
        event.preventDefault();
        actions.close();
        document.getElementById(button.dataset.actionForm)?.requestSubmit();
    });

    // Clic en el fondo cierra, igual que Escape en un <dialog> nativo.
    document.addEventListener("click", (event) => {
        if (event.target instanceof HTMLDialogElement) event.target.close();
    });

    for (const dialog of document.querySelectorAll("dialog.ui-dialog")) {
        dialog.addEventListener("close", () => {
            if (dialog !== confirm && dialog !== actions) return;
            // Si el modal de acciones abrió una confirmación, el foco vuelve
            // al botón de esa confirmación, no al disparador que ya se cerró.
            if (document.querySelector("dialog[open]")) return;
            lastTrigger?.focus();
            lastTrigger = null;
        });
    }
})();
