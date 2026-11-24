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
 * Logo oficial que comparten las exportaciones. Se lee del mismo recurso que
 * usa la web para que CSV, Excel y PDF muestren la misma marca.
 */
public final class ExportBrand {

    private static final String LOGO_PATH = "static/img/brand/logo-claro.png";
    private static final byte[] LOGO = load();

    private ExportBrand() {
    }

    public static byte[] logoPng() {
        return LOGO.clone();
    }

    /** Escala el logo sobre fondo blanco para incrustarlo en el PDF. */
    public static BufferedImage logoEscalado(int width) {
        BufferedImage original;
        try {
            original = ImageIO.read(new ByteArrayInputStream(LOGO));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        int height = Math.round(width * (float) original.getHeight() / original.getWidth());
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(original, 0, 0, width, height, null);
        graphics.dispose();
        return scaled;
    }

    private static byte[] load() {
        try (InputStream stream = ExportBrand.class.getClassLoader().getResourceAsStream(LOGO_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("No se encontró el logo de la marca: " + LOGO_PATH);
            }
            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
