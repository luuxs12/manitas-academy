package com.mundomanitas.api.service.admin;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Recursos gráficos y marcas compartidos por las exportaciones de administración.
 */
public final class ExportBrand {

    private static final String PATH_CLARO = "static/img/brand/logo-claro.png";
    private static final String PATH_OSCURO = "static/img/brand/logo-oscuro.png";

    private static final byte[] LOGO_CLARO = load(PATH_CLARO);
    private static final byte[] LOGO_OSCURO = load(PATH_OSCURO);

    private ExportBrand() {
    }

    /** Logo con letras oscuras sobre transparencia para fondos claros (Excel). */
    public static byte[] logoPng() {
        return LOGO_CLARO.clone();
    }

    /** Logo escalado sobre fondo oscuro para el badge de cabecera del PDF. */
    public static BufferedImage logoEscalado(int width) {
        BufferedImage original = toBufferedImage(LOGO_OSCURO);
        int height = Math.max(1, Math.round(width * (float) original.getHeight() / original.getWidth()));
        return renderScaled(LOGO_OSCURO, width, height, Color.BLACK);
    }

    private static BufferedImage renderScaled(byte[] bytes, int width, int height, Color background) {
        BufferedImage original = toBufferedImage(bytes);
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setColor(background);
        graphics.fillRect(0, 0, width, height);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(original, 0, 0, width, height, null);
        graphics.dispose();
        return scaled;
    }

    private static BufferedImage toBufferedImage(byte[] bytes) {
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static byte[] load(String path) {
        try (InputStream stream = ExportBrand.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("No se encontró el logo: " + path);
            }
            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
