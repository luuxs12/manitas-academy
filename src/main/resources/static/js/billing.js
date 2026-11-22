/* Checkout embebido de Stripe: el formulario se monta dentro del panel. */

(() => {
    const modal = document.getElementById("billing-checkout");
    if (!modal) return;

    const endpoint = modal.dataset.endpoint;
    const publishableKey = modal.dataset.publishableKey;
    const csrf = document.getElementById("billing-csrf")?.value || "";
    const mountPoint = document.getElementById("billing-checkout-mount");
    const message = document.getElementById("billing-checkout-message");
    const closeButton = modal.querySelector("[data-billing-close]");

    const showMessage = (text) => {
        if (message) message.textContent = text || "";
    };

    const close = () => {
        modal.hidden = true;
        if (mountPoint) mountPoint.replaceChildren();
        showMessage("");
    };

    closeButton?.addEventListener("click", close);
    modal.addEventListener("click", (event) => {
        if (event.target === modal) close();
    });
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !modal.hidden) close();
    });

    for (const button of document.querySelectorAll("[data-checkout-plan]")) {
        button.addEventListener("click", async () => {
            if (typeof Stripe !== "function") {
                showMessage("No se pudo cargar Stripe. Revisá tu conexión e intentá de nuevo.");
                modal.hidden = false;
                return;
            }

            modal.hidden = false;
            showMessage("Preparando el pago…");
            if (mountPoint) mountPoint.replaceChildren();

            const fetchClientSecret = async () => {
                const response = await fetch(endpoint, {
                    method: "POST",
                    headers: {
                        "X-CSRF-TOKEN": csrf,
                        "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
                        Accept: "application/json"
                    },
                    body: new URLSearchParams({ plan: button.dataset.checkoutPlan })
                });
                const data = await response.json();
                if (!response.ok) {
                    throw new Error(data.error || "No se pudo iniciar el pago.");
                }
                showMessage("");
                return data.clientSecret;
            };

            try {
                const stripe = Stripe(publishableKey);
                const checkout = await stripe.createEmbeddedCheckoutPage({ fetchClientSecret });
                await checkout.mount("#billing-checkout-mount");
            } catch (error) {
                showMessage(error.message || "No se pudo iniciar el pago.");
            }
        });
    }
})();
