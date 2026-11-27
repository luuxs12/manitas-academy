/* Los iconos del panel usan la copia local de Lucide.
   Los marcadores <i data-lucide> se sustituyen al terminar de parsear el DOM. */

(() => {
    document.addEventListener("DOMContentLoaded", () => {
        if (typeof lucide === "undefined" || typeof lucide.createIcons !== "function") return;
        lucide.createIcons();
    });
})();
