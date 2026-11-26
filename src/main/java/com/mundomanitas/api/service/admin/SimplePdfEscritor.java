package com.mundomanitas.api.service.admin;

import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Generador PDF artesanal con identidad visual oficial de Mundo Manitas.
 * Reproduce fielmente la ficha ejecutiva, tarjetas KPI, tabla con cabecera oscura
 * y pie de página institucional.
 */
@Component
public class SimplePdfEscritor {

    private static final int PAGE_WIDTH = 595;
    private static final int MARGIN = 42;
    private static final int CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN; // 511
    private static final int RIGHT_EDGE = MARGIN + CONTENT_WIDTH;     // 553
    private static final int LOGO_WIDTH = 136;
    private static final int TABLE_TOP = 660;
    private static final int HEADER_ROW = 22;
    private static final int BODY_ROW = 18;
    private static final int ROWS_PER_PAGE = (TABLE_TOP - HEADER_ROW - 70) / BODY_ROW; // 31

    private record Etiquetas(String titulo, String subtitle, String total) {
    }

    /** @param table primera fila = cabeceras; el resto = datos reales. */
    public byte[] write(String modulo, String generatedAt, List<List<String>> table) {
        String lower = modulo.toLowerCase(Locale.ROOT);
        return build(new Etiquetas("Informe de " + modulo,
                "Listado de " + lower + " registrados en la plataforma",
                "Total de " + lower + ":"), generatedAt, table);
    }

    /** Variante para documentos con rótulos propios; deja intactos los informes. */
    public byte[] writeDocument(String titulo, String subtitle, String total,
                                String generatedAt, List<List<String>> table) {
        return build(new Etiquetas(titulo, subtitle, total), generatedAt, table);
    }

    private byte[] build(Etiquetas etiquetas, String generatedAt, List<List<String>> table) {
        List<String> headers = table.isEmpty() ? List.of() : table.getFirst();
        List<List<String>> datos = table.size() > 1
                ? new ArrayList<>(table.subList(1, table.size())) : List.of();
        List<List<List<String>>> pages = paginate(datos);

        BufferedImage logo = ExportacionMarca.logoEscalado(LOGO_WIDTH);
        byte[] imagen = flate(rgb(logo));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeAscii(output, "%PDF-1.4\n");

        int firstPage = 6;
        int objectContar = firstPage - 1 + pages.size() * 2;
        List<Long> offsets = new ArrayList<>(objectContar + 1);
        offsets.add(0L);

        writeCatalogAndPages(output, offsets, pages.size(), firstPage);
        writeFonts(output, offsets);
        writeImagenObject(output, offsets, logo, imagen);
        writePageObjects(output, offsets, pages, etiquetas, generatedAt, headers, datos.size(), firstPage);
        writeTrailer(output, offsets, objectContar);

        return output.toByteArray();
    }

    private void writeCatalogAndPages(ByteArrayOutputStream output, List<Long> offsets,
                                      int pageContar, int firstPage) {
        offsets.add(posicion(output));
        writeAscii(output, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        StringBuilder kids = new StringBuilder();
        for (int index = 0; index < pageContar; index++) {
            kids.append(firstPage + index * 2).append(" 0 R");
            if (index < pageContar - 1) kids.append(' ');
        }
        offsets.add(posicion(output));
        writeAscii(output, "2 0 obj\n<< /Type /Pages /Kids [" + kids + "] /Count "
                + pageContar + " >>\nendobj\n");
    }

    private void writeFonts(ByteArrayOutputStream output, List<Long> offsets) {
        offsets.add(posicion(output));
        writeAscii(output, font(3, "Helvetica"));
        offsets.add(posicion(output));
        writeAscii(output, font(4, "Helvetica-Bold"));
    }

    private void writeImagenObject(ByteArrayOutputStream output, List<Long> offsets,
                                  BufferedImage logo, byte[] imagen) {
        offsets.add(posicion(output));
        writeAscii(output, "5 0 obj\n<< /Type /XObject /Subtype /Image /Width " + logo.getWidth()
                + " /Height " + logo.getHeight()
                + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /Length "
                + imagen.length + " >>\nstream\n");
        output.writeBytes(imagen);
        writeAscii(output, "\nendstream\nendobj\n");
    }

    private void writePageObjects(ByteArrayOutputStream output, List<Long> offsets,
                                  List<List<List<String>>> pages, Etiquetas etiquetas,
                                  String generatedAt, List<String> headers,
                                  int datosSize, int firstPage) {
        for (int index = 0; index < pages.size(); index++) {
            int pageObject = firstPage + index * 2;
            int contenidoObject = pageObject + 1;
            byte[] content = content(etiquetas, generatedAt, headers, pages.get(index), datosSize,
                    index + 1, pages.size());

            offsets.add(posicion(output));
            writeAscii(output, page(pageObject, contenidoObject));

            offsets.add(posicion(output));
            writeAscii(output, contenidoObject + " 0 obj\n<< /Length " + content.length + " >>\nstream\n");
            output.writeBytes(content);
            writeAscii(output, "\nendstream\nendobj\n");
        }
    }

    private void writeTrailer(ByteArrayOutputStream output, List<Long> offsets, int objectContar) {
        long xref = posicion(output);
        writeAscii(output, "xref\n0 " + (objectContar + 1) + "\n");
        writeAscii(output, "0000000000 65535 f \n");
        for (int object = 1; object <= objectContar; object++) {
            writeAscii(output, String.format(Locale.ROOT, "%010d 00000 n \n", offsets.get(object)));
        }
        writeAscii(output, "trailer\n<< /Size " + (objectContar + 1) + " /Root 1 0 R >>\n");
        writeAscii(output, "startxref\n" + xref + "\n%%EOF\n");
    }

    private String font(int object, String name) {
        return object + " 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /" + name
                + " /Encoding /WinAnsiEncoding >>\nendobj\n";
    }

    private String page(int pageObject, int contenidoObject) {
        return pageObject + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "
                + PAGE_WIDTH + " 842] /Resources << /Font << /F1 3 0 R /F2 4 0 R >>"
                + " /XObject << /Im0 5 0 R >> >> /Contents " + contenidoObject
                + " 0 R >>\nendobj\n";
    }

    private byte[] content(Etiquetas etiquetas, String generatedAt, List<String> headers,
                           List<List<String>> rows, int recordContar, int page, int total) {
        StringBuilder content = new StringBuilder();
        drawTopBarAndTarjeta(content, etiquetas);
        drawKpiCards(content, etiquetas, generatedAt, recordContar);
        drawTable(content, headers, rows);
        drawFooter(content, page, total);
        return content.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private void drawTopBarAndTarjeta(StringBuilder content, Etiquetas etiquetas) {
        content.append("q\n0 1 0 rg\n0 838 ").append(PAGE_WIDTH).append(" 4 re f\nQ\n");
        content.append("q\n0.98 0.98 0.99 rg\n0.85 0.85 0.85 RG\n0.5 w\n")
                .append(MARGIN).append(" 730 ").append(CONTENT_WIDTH).append(" 88 re B\nQ\n");

        content.append("q\n0 0 0 rg\n48 736 148 76 re f\nQ\n");
        content.append("q\n136 0 0 46 54 751 cm\n/Im0 Do\nQ\n");

        text(content, "F2", 16, 210, 786, etiquetas.titulo(), "0 0 0");
        text(content, "F1", 9, 210, 768, etiquetas.subtitle(), "0.4 0.4 0.4");
        text(content, "F1", 8, 210, 752, "Mundo Manitas · Plataforma accesible LSP", "0.55 0.55 0.55");

        content.append("q\n0 0 0 rg\n500 730 m 514 730 l 534 818 l 520 818 l f\nQ\n");
        content.append("q\n0 1 0 rg\n516 730 m 534 730 l 553 790 l 553 818 l 535 818 l f\nQ\n");
    }

    private void drawKpiCards(StringBuilder content, Etiquetas etiquetas, String generatedAt, int count) {
        int tarjetaWidth = (CONTENT_WIDTH - 16) / 3; // 165
        int y = 678;

        for (int i = 0; i < 3; i++) {
            int x = MARGIN + i * (tarjetaWidth + 8);
            content.append("q\n0.98 0.98 0.99 rg\n0.85 0.85 0.85 RG\n0.5 w\n")
                    .append(x).append(' ').append(y).append(' ').append(tarjetaWidth).append(" 40 re B\nQ\n");
        }

        text(content, "F1", 7, MARGIN + 10, y + 24, "Fecha de generación:", "0.45 0.45 0.45");
        text(content, "F2", 8, MARGIN + 10, y + 9, "Generado: " + generatedAt, "0.1 0.1 0.1");

        text(content, "F1", 7, MARGIN + tarjetaWidth + 18, y + 24, etiquetas.total(), "0.45 0.45 0.45");
        text(content, "F2", 8, MARGIN + tarjetaWidth + 18, y + 9, count
                + (count == 1 ? " registro" : " registros"), "0.1 0.1 0.1");

        text(content, "F1", 7, MARGIN + (tarjetaWidth + 8) * 2 + 10, y + 24, "Estado:", "0.45 0.45 0.45");
        text(content, "F2", 8, MARGIN + (tarjetaWidth + 8) * 2 + 10, y + 9, "• Activo", "0 1 0");
    }

    private void drawTable(StringBuilder content, List<String> headers, List<List<String>> rows) {
        int[] widths = widths(headers, rows);
        drawHeader(content, headers, widths);
        if (rows.isEmpty()) {
            int top = TABLE_TOP - HEADER_ROW - BODY_ROW;
            content.append("q\n0.96 0.96 0.95 rg\n").append(MARGIN).append(' ')
                    .append(top).append(' ').append(CONTENT_WIDTH).append(' ').append(BODY_ROW)
                    .append(" re f\nQ\n");
            text(content, "F1", 9, MARGIN + 14, top + 5,
                    "No hay datos para este informe.", "0.4 0.4 0.4");
        } else {
            drawBody(content, rows, widths);
        }
    }

    private void drawHeader(StringBuilder content, List<String> headers, int[] widths) {
        int top = TABLE_TOP - HEADER_ROW;
        content.append("q\n0 0 0 rg\n").append(MARGIN).append(' ').append(top)
                .append(' ').append(CONTENT_WIDTH).append(' ').append(HEADER_ROW).append(" re f\nQ\n");
        content.append("q\n0 0 0 RG\n0.5 w\n").append(MARGIN).append(' ').append(top)
                .append(' ').append(CONTENT_WIDTH).append(' ').append(HEADER_ROW).append(" re S\nQ\n");

        int x = MARGIN;
        for (int column = 0; column < widths.length; column++) {
            String value = column < headers.size() ? headers.get(column) : "";
            text(content, "F2", 8, x + 8, top + 7, fit(value, widths[column]), "1 1 1");
            x += widths[column];
        }
    }

    private void drawBody(StringBuilder content, List<List<String>> rows, int[] widths) {
        for (int index = 0; index < rows.size(); index++) {
            int top = TABLE_TOP - HEADER_ROW - (index + 1) * BODY_ROW;
            if (index % 2 == 0) {
                content.append("q\n0.96 0.96 0.95 rg\n").append(MARGIN).append(' ').append(top)
                        .append(' ').append(CONTENT_WIDTH).append(' ').append(BODY_ROW).append(" re f\nQ\n");
            }
            int x = MARGIN;
            for (int column = 0; column < widths.length; column++) {
                List<String> row = rows.get(index);
                String value = column < row.size() ? row.get(column) : "";
                text(content, "F1", 8, x + 8, top + 5, fit(value, widths[column]), "0.1 0.1 0.1");
                x += widths[column];
            }
        }
        content.append("q\n0.85 0.85 0.85 RG\n0.4 w\n");
        int actualX = MARGIN;
        for (int column = 0; column <= widths.length; column++) {
            content.append(actualX).append(' ').append(TABLE_TOP - HEADER_ROW).append(" m ")
                    .append(actualX).append(' ').append(TABLE_TOP - HEADER_ROW - rows.size() * BODY_ROW)
                    .append(" l S\n");
            if (column < widths.length) actualX += widths[column];
        }
        for (int index = 0; index <= rows.size(); index++) {
            int y = TABLE_TOP - HEADER_ROW - index * BODY_ROW;
            content.append(MARGIN).append(' ').append(y).append(" m ").append(RIGHT_EDGE).append(' ').append(y).append(" l S\n");
        }
        content.append("Q\n");
    }

    private void drawFooter(StringBuilder content, int page, int total) {
        content.append("q\n0 1 0 rg\n42 44 24 2 re f\nQ\n");
        text(content, "F2", 8, MARGIN, 49, "Aprende, crea y crece", "0.15 0.15 0.15");
        String motto = "Manitas Academy • Educación sin barreras";
        text(content, "F1", 8, RIGHT_EDGE - (int) Math.round(motto.length() * 4.6), 49, motto, "0.4 0.4 0.4");

        content.append("q\n0.85 0.85 0.85 RG\n0.4 w\n").append(MARGIN).append(" 36 m ")
                .append(RIGHT_EDGE).append(" 36 l S\nQ\n");
        text(content, "F1", 7, MARGIN, 24, "Mundo Manitas · Documento administrativo oficial", "0.5 0.5 0.5");
        String pageText = "Página " + page + " / " + total;
        text(content, "F2", 8, RIGHT_EDGE - (int) Math.round(pageText.length() * 4.8), 24, pageText, "0.2 0.2 0.2");
    }

    private void text(StringBuilder content, String font, int size, int x, int y,
                      String value, String rgb) {
        content.append("BT\n").append(rgb).append(" rg\n/").append(font).append(' ')
                .append(size).append(" Tf\n1 0 0 1 ").append(x).append(' ').append(y)
                .append(" Tm\n(").append(escape(value)).append(") Tj\nET\n");
    }

    private int[] widths(List<String> headers, List<List<String>> rows) {
        int columns = Math.max(1, headers.size());
        int[] widths = new int[columns];
        for (int column = 0; column < columns; column++) {
            int longest = column < headers.size() ? headers.get(column).length() : 1;
            for (List<String> row : rows) {
                if (column < row.size()) longest = Math.max(longest, row.get(column).length());
            }
            widths[column] = Math.min(220, Math.max(60, longest * 6 + 18));
        }
        int total = 0;
        for (int width : widths) total += width;
        int disponible = PAGE_WIDTH - 2 * MARGIN; // 511
        double factor = disponible / (double) total;
        int assigned = 0;
        for (int column = 0; column < columns; column++) {
            widths[column] = Math.max(35, (int) Math.round(widths[column] * factor));
            assigned += widths[column];
        }
        int diff = disponible - assigned;
        if (diff != 0) {
            int widestIndex = 0;
            for (int i = 1; i < columns; i++) {
                if (widths[i] > widths[widestIndex]) widestIndex = i;
            }
            widths[widestIndex] += diff;
        }
        return widths;
    }

    private String fit(String value, int width) {
        String text = value == null ? "" : value;
        int limit = Math.max(4, (width - 12) * 10 / 48);
        if (text.length() <= limit) return text;
        return text.substring(0, limit - 1) + '…';
    }

    private List<List<List<String>>> paginate(List<List<String>> datos) {
        List<List<List<String>>> pages = new ArrayList<>();
        if (datos.isEmpty()) {
            pages.add(List.of());
            return pages;
        }
        for (int from = 0; from < datos.size(); from += ROWS_PER_PAGE) {
            pages.add(new ArrayList<>(datos.subList(from, Math.min(from + ROWS_PER_PAGE, datos.size()))));
        }
        return pages;
    }

    private byte[] flate(byte[] raw) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(raw.length);
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, false);
        try (DeflaterOutputStream stream = new DeflaterOutputStream(output, deflater)) {
            stream.write(raw);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } finally {
            deflater.end();
        }
        return output.toByteArray();
    }

    private byte[] rgb(BufferedImage imagen) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(imagen.getWidth() * imagen.getHeight() * 3);
        for (int y = 0; y < imagen.getHeight(); y++) {
            for (int x = 0; x < imagen.getWidth(); x++) {
                int pixel = imagen.getRGB(x, y);
                output.write(pixel >> 16 & 0xFF);
                output.write(pixel >> 8 & 0xFF);
                output.write(pixel & 0xFF);
            }
        }
        return output.toByteArray();
    }

    /** Escapa strings PDF y traduce los signos tipográficos a WinAnsi. */
    private String escape(String raw) {
        String safe = raw == null ? "" : raw;
        StringBuilder resultado = new StringBuilder();
        for (char character : safe.toCharArray()) {
            switch (character) {
                case '—' -> resultado.append((char) 151);
                case '–' -> resultado.append((char) 150);
                case '…' -> resultado.append((char) 133);
                case '“' -> resultado.append((char) 147);
                case '”' -> resultado.append((char) 148);
                case '‘' -> resultado.append((char) 145);
                case '’' -> resultado.append((char) 146);
                case '•' -> resultado.append((char) 149);
                default -> {
                    if (character == '\\' || character == '(' || character == ')') resultado.append('\\');
                    resultado.append(character < 256 ? character : '?');
                }
            }
        }
        return resultado.toString();
    }

    private long posicion(ByteArrayOutputStream output) {
        return output.size();
    }

    private void writeAscii(ByteArrayOutputStream output, String text) {
        output.writeBytes(text.getBytes(StandardCharsets.ISO_8859_1));
    }
}
