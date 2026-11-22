package com.mundomanitas.api.service.admin;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Generador PDF mínimo sin dependencias nuevas: usa las fuentes base de PDF
 * (Helvetica con WinAnsi, suficiente para español rioplatense).
 */
@Component
public class PlainPdfWriter {

    private static final int LINES_PER_PAGE = 44;
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;

    public byte[] write(String title, List<String> lines) {
        List<List<String>> pages = paginate(lines);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeAscii(output, "%PDF-1.4\n");

        int fontRegular = 3;
        int fontBold = 4;
        int firstPage = 5;
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
        writeAscii(output, "2 0 obj\n<< /Type /Pages /Kids [");
        writeAscii(output, kids.toString());
        writeAscii(output, "] /Count " + pages.size() + " >>\nendobj\n");

        offsets.add(position(output));
        writeAscii(output, font(fontRegular, "Helvetica"));
        offsets.add(position(output));
        writeAscii(output, font(fontBold, "Helvetica-Bold"));

        for (int index = 0; index < pages.size(); index++) {
            int pageObject = firstPage + index * 2;
            int contentObject = pageObject + 1;
            byte[] content = content(title, pages.get(index), index + 1, pages.size());

            offsets.add(position(output));
            writeAscii(output, page(pageObject, contentObject, fontRegular, fontBold));

            offsets.add(position(output));
            writeAscii(output, contentObject + " 0 obj\n<< /Length " + content.length + " >>\nstream\n");
            output.writeBytes(content);
            writeAscii(output, "\nendstream\nendobj\n");
        }

        long xref = position(output);
        writeAscii(output, "xref\n0 " + (objectCount + 1) + "\n");
        writeAscii(output, "0000000000 65535 f \n");
        for (int object = 1; object <= objectCount; object++) {
            writeAscii(output, String.format("%010d 00000 n \n", offsets.get(object)));
        }
        writeAscii(output, "trailer\n<< /Size " + (objectCount + 1) + " /Root 1 0 R >>\n");
        writeAscii(output, "startxref\n" + xref + "\n%%EOF\n");
        return output.toByteArray();
    }

    private String font(int object, String name) {
        return object + " 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /" + name
                + " /Encoding /WinAnsiEncoding >>\nendobj\n";
    }

    private String page(int pageObject, int contentObject, int regular, int bold) {
        return pageObject + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "
                + PAGE_WIDTH + " " + PAGE_HEIGHT + "] /Resources << /Font << /F1 "
                + regular + " 0 R /F2 " + bold + " 0 R >> >> /Contents " + contentObject
                + " 0 R >>\nendobj\n";
    }

    private byte[] content(String title, List<String> lines, int page, int total) {
        StringBuilder content = new StringBuilder("BT\n/F2 16 Tf\n50 792 Td\n(");
        content.append(escape(title));
        if (total > 1) content.append(" - ").append(page).append('/').append(total);
        content.append(") Tj\n/F1 10 Tf\n0 -34 Td\n");
        for (String line : lines) {
            content.append('(').append(escape(line)).append(") Tj\n0 -16 Td\n");
        }
        content.append("ET\n");
        return content.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private List<List<String>> paginate(List<String> lines) {
        List<List<String>> pages = new ArrayList<>();
        if (lines.isEmpty()) {
            pages.add(List.of("No hay datos para este informe."));
            return pages;
        }
        for (int from = 0; from < lines.size(); from += LINES_PER_PAGE) {
            pages.add(new ArrayList<>(lines.subList(from, Math.min(from + LINES_PER_PAGE, lines.size()))));
        }
        return pages;
    }

    private String escape(String raw) {
        String safe = raw == null ? "" : raw;
        StringBuilder result = new StringBuilder();
        for (char character : safe.toCharArray()) {
            if (character == '\\' || character == '(' || character == ')') result.append('\\');
            if (character < 256) {
                result.append(character);
            } else {
                result.append('?');
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
