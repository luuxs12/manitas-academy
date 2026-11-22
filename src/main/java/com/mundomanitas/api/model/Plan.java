package com.mundomanitas.api.model;

/**
 * Plan contratado por una cuenta.
 *
 * <p>El plan gratuito abre los cursos marcados como gratuitos; los planes
 * mensual y anual abren todo el catálogo y habilitan los certificados.
 * Los montos son los publicados en el sitio, en céntimos de sol peruano.</p>
 */
public enum Plan {

    FREE("Gratis", false, 0, "Gratis"),
    MONTHLY("Mensual", true, 2_900, "S/ 29 al mes"),
    ANNUAL("Anual", true, 29_000, "S/ 290 al año");

    private final String label;
    private final boolean premium;
    private final int amountCents;
    private final String priceLabel;

    Plan(String label, boolean premium, int amountCents, String priceLabel) {
        this.label = label;
        this.premium = premium;
        this.amountCents = amountCents;
        this.priceLabel = priceLabel;
    }

    public String getLabel() {
        return label;
    }

    public boolean isPremium() {
        return premium;
    }

    public int amountCents() {
        return amountCents;
    }

    public String priceLabel() {
        return priceLabel;
    }

    /** Planes que se pueden contratar: Gratis no se compra ni se cambia a mano. */
    public static Plan[] purchasable() {
        return new Plan[]{MONTHLY, ANNUAL};
    }
}
