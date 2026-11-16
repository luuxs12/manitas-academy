/* Formularios de cuenta: mostrar contraseña y medir su fuerza. */

(() => {
    const toggles = document.querySelectorAll("[data-toggle-password]");

    for (const toggle of toggles) {
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

    const password = document.getElementById("password");
    const meter = document.querySelector("[data-password-meter]");
    const hint = document.querySelector("[data-password-hint]");

    if (!password || !meter || !hint) return;

    const updateStrength = () => {
        const value = password.value;
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

    password.addEventListener("input", updateStrength);
    updateStrength();
})();
