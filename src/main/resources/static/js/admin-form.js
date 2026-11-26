/* Los formularios y filtros del panel eligen primero el curso y después un
   módulo de ese curso, sin pedirle a quien administra que lea nombres largos. */

(() => {
    const groups = document.querySelectorAll("[data-admin-curso]");
    if (!groups.length) return;

    for (const group of groups) {
        const course = group.querySelector("[data-course-select]");
        const modules = group.querySelector("[data-module-select]");
        if (!course || !modules) continue;

        const requiereCurso = group.dataset.requerirCurso === "true";
        const options = Array.from(modules.querySelectorAll("option[data-curso]"));

        const sync = () => {
            const curso = course.value;
            modules.disabled = requiereCurso && !curso;

            for (const option of options) {
                const visible = !curso || option.dataset.curso === curso;
                option.hidden = !visible;
                option.disabled = !visible;
                option.textContent = curso && option.dataset.nombre
                    ? option.dataset.nombre
                    : (option.dataset.label || option.textContent);
            }

            // Nunca elegimos un módulo por defecto: en un alta sería fácil
            // guardar el contenido en el lugar equivocado.
            if (modules.value) {
                const seleccionada = options.find(option => option.value === modules.value && !option.hidden);
                if (!seleccionada) modules.value = "";
            }
        };

        // En una edición el curso no viene en el formulario: se deduce del
        // módulo guardado antes de deshabilitar nada.
        const selected = modules.value
            ? options.find(option => option.value === modules.value)
            : null;
        if (selected && !course.value) course.value = selected.dataset.curso || "";

        course.addEventListener("change", () => {
            modules.value = "";
            sync();
        });
        sync();
    }
})();
