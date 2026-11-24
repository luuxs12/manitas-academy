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
 * Generador PDF sin dependencias nuevas: fuentes base de PDF, logo embebido
 * como XObject y tabla con cebra, bordes y cabecera repetida por página.
 */
@Component
public class PlainPdfWriter {

    private static final int PAGE_WIDTH = 595;
    private static final int MARGIN = 42;
    private static final int LOGO_WIDTH = 160;
    private static final int TABLE_TOP = 690;
    private static final int TABLE_BOTTOM = 74;
    private static final int HEADER_ROW = 22;
    private static final int BODY_ROW = 18;
    private static final int ROWS_PER_PAGE = (TABLE_TOP - TABLE_BOTTOM - HEADER_ROW) / BODY_ROW;

    /** @param table primera fila = cabeceras; el resto = datos reales. */
    public byte[] write(String module, String generatedAt, List<List<String>> table) {
        List<String> headers = table.isEmpty() ? List.of() : table.getFirst();
        List<List<String>> data = table.size() > 1
                ? new ArrayList<>(table.subList(1, table.size())) : List.of();
        List<List<List<String>>> pages = paginate(data);

        BufferedImage logo = ExportBrand.logoEscalado(LOGO_WIDTH);
        byte[] image = flate(rgb(logo));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeAscii(output, "%PDF-1.4\n");

        int firstPage = 6;
        int objectCount = firstPage - 1 + pages.size() * 2;
        List<Long> offsets = new ArrayList<>(objectCount + 1);
        offsets.add(0L);

        offsets.add(position(output));
        writeAscii(output, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        StringBuilder kids = new StringBuilder();
        for (int index = 0; index < pages.size(); index++) {
            kids.append(firstPage + index * 2).append(" 0 R");
            if (index < pages.size() - 1) kids.append(' ');
        }
        offsets.add(position(output));
        writeAscii(output, "2 0 obj\n<< /Type /Pages /Kids [" + kids + "] /Count "
                + pages.size() + " >>\nendobj\n");

        offsets.add(position(output));
        writeAscii(output, font(3, "Helvetica"));
        offsets.add(position(output));
        writeAscii(output, font(4, "Helvetica-Bold"));

        offsets.add(position(output));
        writeAscii(output, "5 0 obj\n<< /Type /XObject /Subtype /Image /Width " + logo.getWidth()
                + " /Height " + logo.getHeight()
                + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /Length "
                + image.length + " >>\nstream\n");
        output.writeBytes(image);
        writeAscii(output, "\nendstream\nendobj\n");

        for (int index = 0; index < pages.size(); index++) {
            int pageObject = firstPage + index * 2;
            int contentObject = pageObject + 1;
            byte[] content = content(module, generatedAt, headers, pages.get(index), data.size(),
                    index + 1, pages.size(), logo.getHeight());

            offsets.add(position(output));
            writeAscii(output, page(pageObject, contentObject));

            offsets.add(position(output));
            writeAscii(output, contentObject + " 0 obj\n<< /Length " + content.length + " >>\nstream\n");
            output.writeBytes(content);
            writeAscii(output, "\nendstream\nendobj\n");
        }

        long xref = position(output);
        writeAscii(output, "xref\n0 " + (objectCount + 1) + "\n");
        writeAscii(output, "0000000000 65535 f \n");
        for (int object = 1; object <= objectCount; object++) {
            writeAscii(output, String.format(Locale.ROOT, "%010d 00000 n \n", offsets.get(object)));
        }
        writeAscii(output, "trailer\n<< /Size " + (objectCount + 1) + " /Root 1 0 R >>\n");
        writeAscii(output, "startxref\n" + xref + "\n%%EOF\n");
        return output.toByteArray();
    }

    private String font(int object, String name) {
        return object + " 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /" + name
                + " /Encoding /WinAnsiEncoding >>\nendobj\n";
    }

    private String page(int pageObject, int contentObject) {
        return pageObject + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "
                + PAGE_WIDTH + " 842] /Resources << /Font << /F1 3 0 R /F2 4 0 R >>"
                + " /XObject << /Im0 5 0 R >> >> /Contents " + contentObject
                + " 0 R >>\nendobj\n";
    }

    private byte[] content(String module, String generatedAt, List<String> headers,
                           List<List<String>> rows, int recordCount, int page, int total,
                           int logoHeight) {
        StringBuilder content = new StringBuilder();
        content.append("q\n0 1 0 rg\n0 786 ").append(PAGE_WIDTH).append(" 56 re f\nQ\n");
        text(content, "F2", 18, MARGIN, 818, "MUNDO MANITAS", "0 0 0");
        String tag = "INFORME · " + module.toUpperCase(Locale.ROOT);
        text(content, "F2", 9, PAGE_WIDTH - MARGIN - tag.length() * 5, 818, tag, "0 0 0");

        content.append("q\n").append(LOGO_WIDTH).append(" 0 0 ").append(logoHeight)
                .append(' ').append(MARGIN).append(" 716 cm\n/Im0 Do\nQ\n");
        int detailX = MARGIN + LOGO_WIDTH + 20;
        text(content, "F2", 16, detailX, 758, "Informe de " + module, "0 0 0");
        text(content, "F1", 9, detailX, 738,
                "Generado: " + generatedAt + " · " + recordCount
                        + (recordCount == 1 ? " registro" : " registros"), "0.4 0.4 0.4");
        content.append("q\n0 1 0 rg\n").append(MARGIN).append(" 706 511 2 re f\nQ\n");

        int[] widths = widths(headers, rows);
        drawHeader(content, headers, widths);
        if (rows.isEmpty()) {
            content.append("q\n0.96 0.96 0.95 rg\n").append(MARGIN).append(' ')
                    .append(TABLE_TOP - HEADER_ROW - BODY_ROW).append(" 511 ").append(BODY_ROW)
                    .append(" re f\nQ\n");
            text(content, "F1", 10, MARGIN + 12, TABLE_TOP - HEADER_ROW - 13,
                    "No hay datos para este informe.", "0.4 0.4 0.4");
        } else {
            drawBody(content, rows, widths);
        }

        content.append("q\n0.85 0.85 0.85 RG\n0.4 w\n").append(MARGIN).append(" 58 m 553 58 l S\nQ\n");
        text(content, "F1", 8, MARGIN, 42, "Mundo Manitas · Informe de " + module, "0.4 0.4 0.4");
        String footer = "Página " + page + " / " + total;
        text(content, "F1", 8, 553 - footer.length() * 4, 42, footer, "0.4 0.4 0.4");
        return content.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private void drawHeader(StringBuilder content, List<String> headers, int[] widths) {
        int top = TABLE_TOP - HEADER_ROW;
        content.append("q\n0 1 0 rg\n").append(MARGIN).append(' ').append(top)
                .append(" 511 ").append(HEADER_ROW).append(" re f\nQ\n");
        int x = MARGIN;
        for (int column = 0; column < widths.length; column++) {
            String value = column < headers.size() ? headers.get(column) : "";
            text(content, "F2", 9, x + 5, top + 7, fit(value, widths[column]), "0 0 0");
            x += widths[column];
        }
    }

    private void drawBody(StringBuilder content, List<List<String>> rows, int[] widths) {
        for (int index = 0; index < rows.size(); index++) {
            int top = TABLE_TOP - HEADER_ROW - (index + 1) * BODY_ROW;
            if (index % 2 == 0) {
                content.append("q\n0.96 0.96 0.95 rg\n").append(MARGIN).append(' ').append(top)
                        .append(" 511 ").append(BODY_ROW).append(" re f\nQ\n");
            }
            int x = MARGIN;
            for (int column = 0; column < widths.length; column++) {
                List<String> row = rows.get(index);
                String value = column < row.size() ? row.get(column) : "";
                text(content, "F1", 9, x + 5, top + 5, fit(value, widths[column]), "0 0 0");
                x += widths[column];
            }
        }
        content.append("q\n0.85 0.85 0.85 RG\n0.4 w\n");
        for (int column = 0; column <= widths.length; column++) {
            int x = MARGIN;
            for (int previous = 0; previous < column; previous++) x += widths[previous];
            content.append(x).append(' ').append(TABLE_TOP - HEADER_ROW).append(" m ")
                    .append(x).append(' ').append(TABLE_TOP - HEADER_ROW - rows.size() * BODY_ROW)
                    .append(" l S\n");
        }
        for (int index = 0; index <= rows.size(); index++) {
            int y = TABLE_TOP - HEADER_ROW - index * BODY_ROW;
            content.append(MARGIN).append(' ').append(y).append(" m 553 ").append(y).append(" l S\n");
        }
        content.append("Q\n");
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
            widths[column] = Math.min(200, Math.max(70, longest * 6 + 16));
        }
        int total = 0;
        for (int width : widths) total += width;
        double factor = (PAGE_WIDTH - 2 * MARGIN) / (double) total;
        for (int column = 0; column < columns; column++) {
            widths[column] = Math.max(40, (int) (widths[column] * factor));
        }
        return widths;
    }

    private String fit(String value, int width) {
        String text = value == null ? "" : value;
        int limit = Math.max(4, (width - 10) / 5);
        if (text.length() <= limit) return text;
        return text.substring(0, limit - 1) + '…';
    }

    private List<List<List<String>>> paginate(List<List<String>> data) {
        List<List<List<String>>> pages = new ArrayList<>();
        if (data.isEmpty()) {
            pages.add(List.of());
            return pages;
        }
        for (int from = 0; from < data.size(); from += ROWS_PER_PAGE) {
            pages.add(new ArrayList<>(data.subList(from, Math.min(from + ROWS_PER_PAGE, data.size()))));
        }
        return pages;
    }

    private byte[] flate(byte[] raw) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(raw.length);
        try (DeflaterOutputStream stream = new DeflaterOutputStream(output,
                new Deflater(Deflater.DEFAULT_COMPRESSION, true))) {
            stream.write(raw);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return output.toByteArray();
    }

    private byte[] rgb(BufferedImage image) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(image.getWidth() * image.getHeight() * 3);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
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
        StringBuilder result = new StringBuilder();
        for (char character : safe.toCharArray()) {
            switch (character) {
                case '—' -> result.append((char) 151);
                case '–' -> result.append((char) 150);
                case '…' -> result.append((char) 133);
                case '“' -> result.append((char) 147);
                case '”' -> result.append((char) 148);
                case '‘' -> result.append((char) 145);
                case '’' -> result.append((char) 146);
                case '•' -> result.append((char) 149);
                default -> {
                    if (character == '\\' || character == '(' || character == ')') result.append('\\');
                    result.append(character < 256 ? character : '?');
                }
            }
        }
        return result.toString();
    }

    private long position(ByteArrayOutputStream output) {
        return output.size();
    }

    private void writeAscii(ByteArrayOutputStream output, String text) {
        output.writeBytes(text.getBytes(StandardCharsets.ISO_8859_1));
    }
}
