/* Formularios de cuenta: mostrar contraseña y medir su fuerza.
   Se carga en todas las páginas, así que trabaja con grupos: cada campo de
   contraseña nuevo trae su botón, su medidor y su ayuda. */

(() => {
    for (const toggle of document.querySelectorAll("[data-toggle-password]")) {
        const input = document.getElementById(toggle.dataset.togglePassword);
        if (!input) continue;

        toggle.addEventListener("click", () => {
            const show = input.type === "password";
            input.type = show ? "text" : "password";
            toggle.textContent = show ? "Ocultar" : "Mostrar";
            toggle.setAttribute("aria-pressed", String(show));
            input.focus();
        });
    }

    for (const group of document.querySelectorAll("[data-password-group]")) {
        const input = group.querySelector("[data-password-input]");
        const meter = group.querySelector("[data-password-meter]");
        const hint = group.querySelector("[data-password-hint]");

        if (!input || !meter || !hint) continue;

        const updateStrength = () => {
            const value = input.value;
            let score = 0;

            if (value.length >= 8) score += 1;
            if (value.length >= 8 && /[a-z]/i.test(value) && /\d/.test(value)) score += 1;
            if (value.length >= 12 || /[^a-z0-9]/i.test(value)) score += 1;

            meter.dataset.score = String(score);

            if (value.length === 0) {
                hint.textContent = "Mínimo 8 caracteres. Mezclá letras y números.";
            } else if (score <= 1) {
                hint.textContent = "Todavía es débil. Sumá números o un símbolo.";
            } else if (score === 2) {
                hint.textContent = "Va bien. Un símbolo la hace más fuerte.";
            } else {
                hint.textContent = "Buena contraseña.";
            }
        };

        input.addEventListener("input", updateStrength);
        updateStrength();
    }
})();
