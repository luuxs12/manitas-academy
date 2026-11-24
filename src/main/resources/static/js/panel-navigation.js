/**
 * Mantiene a la persona en su panel cuando usa el botón "atrás" del navegador.
 *
 * Solo actúa en la portada pública, cuando la navegación fue hacia atrás y
 * existe una sesión: el enlace de cuenta del encabezado ya apunta al panel
 * correspondiente al rol. Sin sesión no hay enlace y la portada sigue visible,
 * de modo que cerrar sesión devuelve a la página principal.
 */
(function () {
    if (window.location.pathname !== "/") return;

    const navegacion = performance.getEntriesByType("navigation")[0];
    if (!navegacion || navegacion.type !== "back_forward") return;

    const enlace = document.querySelector(".nav-user[href]");
    if (enlace) window.location.replace(enlace.href);
})();
