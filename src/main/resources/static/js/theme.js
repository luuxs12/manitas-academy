/* Tema claro/oscuro.
   Se carga en el <head> sin defer para fijar el tema antes de pintar y
   evitar el destello blanco. La elección manual se guarda en localStorage;
   si no hay elección, manda la preferencia del sistema. */

(() => {
    const KEY = "mm-theme";
    const root = document.documentElement;
    const media = window.matchMedia("(prefers-color-scheme: dark)");
    const buttons = () => [...document.querySelectorAll("[data-theme-toggle]")];

    const stored = () => {
        try { return localStorage.getItem(KEY); } catch { return null; }
    };

    const save = (theme) => {
        try { localStorage.setItem(KEY, theme); } catch { /* modo privado */ }
    };

    const apply = (theme) => {
        const dark = theme === "dark";
        root.dataset.theme = dark ? "dark" : "light";
        root.style.colorScheme = dark ? "dark" : "light";

        const meta = document.querySelector('meta[name="theme-color"]');
        if (meta) meta.setAttribute("content", dark ? "#0b0b0c" : "#ffffff");

        for (const button of buttons()) {
            button.setAttribute("aria-pressed", String(dark));
            button.setAttribute(
                "aria-label",
                dark ? "Cambiar a modo claro" : "Cambiar a modo oscuro"
            );
            button.title = dark ? "Modo claro" : "Modo oscuro";
        }
    };

    const preferred = () => (media.matches ? "dark" : "light");
    const account = () => {
        const meta = document.querySelector('meta[name="account-theme"]');
        const value = meta?.content;
        return value === "LIGHT" ? "light" : value === "DARK" ? "dark" : null;
    };
    const manual = stored();
    apply(manual === "dark" || manual === "light" ? manual : account() || preferred());

    // Los botones pueden venir después de este script: se enlazan al
    // terminar de parsear el documento.
    const bind = () => {
        for (const button of buttons()) {
            if (button.dataset.themeReady === "1") continue;
            button.dataset.themeReady = "1";
            button.addEventListener("click", () => {
                const next = root.dataset.theme === "dark" ? "light" : "dark";
                save(next);
                apply(next);
            });
        }
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", bind, { once: true });
    } else {
        bind();
    }

    media.addEventListener("change", () => {
        if (!stored() && !account()) apply(preferred());
    });
})();
