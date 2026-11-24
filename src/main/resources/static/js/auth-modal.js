/* Modal de cuenta: abre login o registro sobre la página, sin recargar.
   Si el navegador no ejecuta este archivo, los enlaces siguen llevando a
   /login y /registro, que funcionan como páginas completas. */

(() => {
    const dialog = document.getElementById("auth-dialog");
    if (!dialog) return;

    const body = document.body;
    const tabs = [...dialog.querySelectorAll("[data-auth-tab]")];
    const views = [...dialog.querySelectorAll("[data-auth-view]")];
    const loginForm = dialog.querySelector("[data-auth-login]");
    const registerForm = dialog.querySelector("[data-auth-register]");
    let lastTrigger = null;
    const pausedMedia = [];

    // El fondo difuminado sobre un video en reproducción cuesta caro:
    // al abrir el modal se pausa y al cerrar se reanuda lo que sonaba.
    const pauseMedia = () => {
        for (const video of document.querySelectorAll("video")) {
            if (!video.paused) {
                pausedMedia.push(video);
                video.pause();
            }
        }
    };

    const resumeMedia = () => {
        for (const video of pausedMedia) {
            video.play().catch(() => {});
        }
        pausedMedia.length = 0;
    };

    const fieldNames = ["fullName", "email", "password", "confirmPassword", "acceptTerms"];

    const setTab = (name, { focus = false } = {}) => {
        for (const tab of tabs) {
            tab.setAttribute("aria-pressed", String(tab.dataset.authTab === name));
        }

        for (const view of views) {
            view.hidden = view.dataset.authView !== name;
        }

        dialog.setAttribute(
            "aria-labelledby",
            name === "login" ? "auth-login-title" : "auth-register-title",
        );

        if (focus) {
            const view = views.find((item) => item.dataset.authView === name);
            const first = view?.querySelector("input:not([type='hidden']):not([type='checkbox'])");
            if (first) first.focus();
        }
    };

    const clearErrors = () => {
        for (const field of fieldNames) {
            const error = dialog.querySelector(`#modal-${field}-error`);
            if (error) {
                error.textContent = "";
                error.hidden = true;
            }

            const input = dialog.querySelector(`#modal-${field}`);
            if (input) {
                input.classList.remove("is-invalid");
                input.removeAttribute("aria-invalid");
            }
        }

        for (const alert of dialog.querySelectorAll("[data-auth-error]")) {
            alert.textContent = "";
            alert.hidden = true;
        }
    };

    const showAlert = (form, message) => {
        const view = form.closest("[data-auth-view]");
        const alert = view?.querySelector("[data-auth-error]");
        if (!alert) return;

        alert.textContent = message;
        alert.hidden = false;
    };

    const showFieldErrors = (doc) => {
        let firstInvalid = null;

        for (const field of fieldNames) {
            const source = doc.getElementById(`${field}-error`);
            if (!source) continue;

            const error = dialog.querySelector(`#modal-${field}-error`);
            const input = dialog.querySelector(`#modal-${field}`);
            const message = source.textContent.trim();
            if (!error || !message) continue;

            error.textContent = message;
            error.hidden = false;
            input?.classList.add("is-invalid");
            input?.setAttribute("aria-invalid", "true");

            if (input) {
                const describedBy = new Set(
                    (input.getAttribute("aria-describedby") || "").split(/\s+/).filter(Boolean),
                );
                describedBy.add(error.id);
                input.setAttribute("aria-describedby", [...describedBy].join(" "));
            }

            if (!firstInvalid) firstInvalid = input;
        }

        firstInvalid?.focus();
    };

    const setBusy = (form, busy) => {
        const button = form.querySelector("[data-auth-submit]");
        if (!button) return;

        if (busy) {
            button.dataset.label = button.textContent;
            button.textContent = form === loginForm ? "Entrando…" : "Creando tu cuenta…";
            button.disabled = true;
        } else {
            button.textContent = button.dataset.label || button.textContent;
            button.disabled = false;
        }
    };

    const loginSucceeded = (response) => {
        const url = new URL(response.url);
        return response.ok && !(url.pathname === "/login" && url.searchParams.has("error"));
    };

    const sendLogin = async (form) => {
        const response = await fetch(form.action, {
            method: "POST",
            body: new FormData(form),
            credentials: "same-origin",
            headers: { "X-Requested-With": "XMLHttpRequest" },
        });

        if (loginSucceeded(response)) {
            window.location.replace(response.url);
            return;
        }

        showAlert(form, "El correo o la contraseña no coinciden. Revisalos y probá de nuevo.");
        form.querySelector("input[name='username']")?.focus();
    };

    const autoLogin = async (email, password, responseText) => {
        const doc = new DOMParser().parseFromString(responseText, "text/html");
        const token = doc.querySelector("input[name='_csrf']")?.value;
        const data = new URLSearchParams({ username: email, password });

        if (token) data.set("_csrf", token);

        const response = await fetch(loginForm.action, {
            method: "POST",
            body: data,
            credentials: "same-origin",
            headers: { "X-Requested-With": "XMLHttpRequest" },
        });

        if (loginSucceeded(response)) {
            window.location.replace(response.url);
            return;
        }

        // La cuenta existe pero el navegador no quedó dentro: se ofrece entrar.
        setTab("login", { focus: true });
        const username = dialog.querySelector("#modal-login-email");
        if (username) username.value = email;
        showAlert(
            loginForm,
            "Tu cuenta quedó creada. Escribí tu contraseña para entrar.",
        );
    };

    const sendRegister = async (form) => {
        const data = new FormData(form);
        const email = String(data.get("email") || "");
        const password = String(data.get("password") || "");

        const response = await fetch(form.action, {
            method: "POST",
            body: data,
            credentials: "same-origin",
            headers: { "X-Requested-With": "XMLHttpRequest" },
        });

        const url = new URL(response.url);
        const text = await response.text();

        if (!response.ok) {
            showAlert(form, "No pudimos crear tu cuenta ahora. Probá de nuevo en un momento.");
            return;
        }

        if (url.pathname === "/registro") {
            const doc = new DOMParser().parseFromString(text, "text/html");
            showFieldErrors(doc);
            return;
        }

        // El servidor ya abrió la sesión: se entra al panel según el rol.
        if (loginSucceeded(response)) {
            window.location.replace(response.url);
            return;
        }

        await autoLogin(email, password, text);
    };

    const open = (name, trigger) => {
        lastTrigger = trigger || document.activeElement;
        clearErrors();
        setTab(name === "register" ? "register" : "login");
        pauseMedia();
        dialog.showModal();
        body.classList.add("has-auth-dialog");

        // El navegador enfoca el botón de cerrar al abrir el diálogo; se
        // espera ese paso para dejar el foco en el primer campo visible.
        setTimeout(() => {
            const view = views.find((item) => item.dataset.authView === name);
            view?.querySelector("input:not([type='hidden']):not([type='checkbox'])")?.focus();
        }, 0);
    };

    for (const trigger of document.querySelectorAll("[data-auth-open]")) {
        trigger.addEventListener("click", (event) => {
            event.preventDefault();
            const name = trigger.dataset.authOpen;

            // Los enlaces del propio modal solo cambian de pestaña.
            if (dialog.open) {
                clearErrors();
                setTab(name, { focus: true });
                return;
            }

            open(name, trigger);
        });
    }

    // Al cerrar sesión se vuelve a la portada con «/?login»: el formulario
    // queda abierto sobre la página principal, sin pantalla de login aparte.
    const params = new URLSearchParams(window.location.search);
    if (params.has("login") || params.has("registro")) {
        open(params.has("registro") ? "register" : "login");
        window.history.replaceState({}, "", window.location.pathname);
    }

    const close = () => dialog.close();

    for (const tab of tabs) {
        tab.addEventListener("click", () => {
            clearErrors();
            setTab(tab.dataset.authTab);
        });
    }

    for (const button of dialog.querySelectorAll("[data-auth-close]")) {
        button.addEventListener("click", close);
    }

    dialog.addEventListener("click", (event) => {
        if (event.target === dialog) close();
    });

    dialog.addEventListener("close", () => {
        body.classList.remove("has-auth-dialog");
        resumeMedia();
        if (lastTrigger && document.contains(lastTrigger)) lastTrigger.focus();
        lastTrigger = null;
    });

    loginForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        clearErrors();
        setBusy(loginForm, true);

        try {
            await sendLogin(loginForm);
        } catch {
            showAlert(loginForm, "No pudimos conectar. Revisá tu conexión y probá de nuevo.");
        } finally {
            setBusy(loginForm, false);
        }
    });

    registerForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        clearErrors();
        setBusy(registerForm, true);

        try {
            await sendRegister(registerForm);
        } catch {
            showAlert(registerForm, "No pudimos conectar. Revisá tu conexión y probá de nuevo.");
        } finally {
            setBusy(registerForm, false);
        }
    });
})();
