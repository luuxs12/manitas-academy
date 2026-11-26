package com.mundomanitas.api.service.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Generador PDF artesanal")
class SimplePdfEscritorTest {

    private final SimplePdfEscritor escritor = new SimplePdfEscritor();

    @Test
    @DisplayName("dibuja el logo, la cabecera y las filas con cebra y bordes")
    void dibujaMarcaYTabla() {
        byte[] pdf = escritor.write("cursos", "24/11/2026", List.of(
                List.of("Curso", "Estado"),
                List.of("Python desde cero", "publicado")));

        String text = new String(pdf, StandardCharsets.ISO_8859_1);
        assertThat(text).startsWith("%PDF-1.4");
        assertThat(text).contains("/Subtype /Image");
        assertThat(text).contains("/Filter /FlateDecode");
        assertThat(text).contains("/Im0 Do");
        assertThat(text).contains("Mundo Manitas");
        assertThat(text).contains("Informe de cursos");
        assertThat(text).contains("Generado: 24/11/2026");
        assertThat(text).contains("0.96 0.96 0.95 rg");
        assertThat(text).contains("0.85 0.85 0.85 RG");
        assertThat(text).contains("Python desde cero");
    }

    @Test
    @DisplayName("muestra la ficha ejecutiva, las tarjetas KPI y el pie institucional")
    void dibujaFichaEjecutiva() {
        String text = new String(escritor.write("cursos", "24/11/2026", List.of(
                List.of("Curso", "Estado"),
                List.of("Python desde cero", "publicado"))), StandardCharsets.ISO_8859_1);

        assertThat(text).contains("0 1 0 rg\n0 838 595 4 re f");
        assertThat(text).contains("0 0 0 rg\n48 736 148 76 re f");
        assertThat(text).contains("136 0 0 46 54 751 cm\n/Im0 Do");
        assertThat(text).contains("Mundo Manitas · Plataforma accesible LSP");
        assertThat(text).contains("Fecha de generación:");
        assertThat(text).contains("Total de cursos:");
        assertThat(text).contains("1 registro");
        assertThat(text).contains("Estado:");
        assertThat(text).contains((char) 149 + " Activo");
        assertThat(text).contains("0 0 0 rg\n42 638 511 22 re f");
        assertThat(text).contains("1 1 1 rg");
        assertThat(text).contains("Aprende, crea y crece");
        assertThat(text).contains("Manitas Academy " + (char) 149 + " Educación sin barreras");
        assertThat(text).contains("Mundo Manitas · Documento administrativo oficial");
    }

    @Test
    @DisplayName("repite la cabecera y numera cada página")
    void repiteCabeceraYNumeraPaginas() {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Curso", "Estado"));
        for (int index = 1; index <= 40; index++) {
            rows.add(List.of("Curso " + index, "publicado"));
        }

        String text = new String(escritor.write("cursos", "24/11/2026", rows),
                StandardCharsets.ISO_8859_1);

        assertThat(text).contains("Página 1 / 2");
        assertThat(text).contains("Página 2 / 2");
        assertThat(count(text, "Curso")).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("informa cuando el informe no tiene filas")
    void sinFilasMuestraMensaje() {
        String text = new String(escritor.write("cursos", "24/11/2026",
                List.of(List.of("Curso", "Estado"))), StandardCharsets.ISO_8859_1);

        assertThat(text).contains("No hay datos para este informe.");
        assertThat(text).contains("/Im0 Do");
    }

    @Test
    @DisplayName("traduce los signos tipográficos a WinAnsi")
    void traduceSignos() {
        String text = new String(escritor.write("cursos", "24/11/2026", List.of(
                List.of("Curso", "Estado"),
                List.of("A — B … C", "publicado"))), StandardCharsets.ISO_8859_1);

        assertThat(text).contains("(A " + (char) 151 + " B " + (char) 133 + " C)");
    }

    private long count(String text, String needle) {
        long total = 0;
        int from = 0;
        while ((from = text.indexOf(needle, from)) >= 0) {
            total++;
            from += needle.length();
        }
        return total;
    }
}
