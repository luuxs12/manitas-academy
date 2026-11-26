package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.ResultadoExportacion;
import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.repository.AuditoriaRegistroRepository;
import com.mundomanitas.api.repository.CertificadoRepository;
import com.mundomanitas.api.repository.CursoModuloRepository;
import com.mundomanitas.api.repository.CursoRepository;
import com.mundomanitas.api.repository.EjercicioRepository;
import com.mundomanitas.api.repository.PagoRepository;
import com.mundomanitas.api.repository.UsuarioRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Exportaciones con identidad visual")
class AdminExportacionServiceTest {

    @Mock private UsuarioRepository users;
    @Mock private CursoRepository courses;
    @Mock private CursoModuloRepository modules;
    @Mock private VideoRepository videos;
    @Mock private EjercicioRepository exercises;
    @Mock private CertificadoRepository certificates;
    @Mock private PagoRepository payments;
    @Mock private AuditoriaRegistroRepository logs;
    @Mock private AdminDashboardService dashboard;
    @Mock private AdminFormato format;

    private AdminExportacionService service;

    @BeforeEach
    void setUp() {
        service = new AdminExportacionService(users, courses, modules, videos, exercises,
                certificates, payments, logs, dashboard, format, new SimplePdfEscritor());
    }

    private void cursosDePrueba() {
        Curso curso = mock(Curso.class);
        when(curso.getTitulo()).thenReturn("Python desde cero");
        when(courses.buscarAllPorOrdenarPorPosicionAsc()).thenReturn(List.of(curso));
    }

    @Test
    @DisplayName("el CSV separa la marca, los metadatos y la tabla")
    void csvCarriesMarcaHeader() {
        cursosDePrueba();
        ResultadoExportacion resultado = service.exportacion("cursos", "csv");

        String[] lines = new String(resultado.bytes(), StandardCharsets.UTF_8).split("\r\n");
        assertThat(lines[0]).isEqualTo("\uFEFF\"Mundo Manitas\"");
        assertThat(lines[1]).isEqualTo("\"Informe de cursos\"");
        assertThat(lines[2]).startsWith("\"Generado\";\"");
        assertThat(lines[3]).isEqualTo("\"Registros\";\"1\"");
        assertThat(lines[4]).isEqualTo("\"\"");
        assertThat(lines[5]).contains("\"Título\";\"Identificador\"");
        assertThat(lines[6]).contains("\"Python desde cero\"");
    }

    @Test
    @DisplayName("el CSV neutraliza lo que Excel leería como fórmula")
    void csvNeutralizaFormulas() {
        Curso curso = mock(Curso.class);
        when(curso.getTitulo()).thenReturn("=HYPERLINK(\"http://ejemplo\")");
        when(courses.buscarAllPorOrdenarPorPosicionAsc()).thenReturn(List.of(curso));

        String text = new String(service.exportacion("cursos", "csv").bytes(), StandardCharsets.UTF_8);

        assertThat(text).contains("\"'=HYPERLINK(\"\"http://ejemplo\"\")\"");
    }

    @Test
    @DisplayName("el Excel lleva el logo, banda de marca y tabla con cebra y filtro completo")
    void xlsxHasBrandedTable() throws Exception {
        cursosDePrueba();
        ResultadoExportacion resultado = service.exportacion("cursos", "xlsx");

        assertThat(hasImagen(resultado.bytes())).isTrue();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(resultado.bytes()))) {
            var sheet = workbook.getSheet("Datos");
            assertThat(sheet.getRow(0).getCell(3).getStringCellValue()).isEqualTo("Mundo Manitas");
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).contains("Informe de cursos");
            assertThat(sheet.getRow(0).getHeightInPoints()).isGreaterThan(40);
            assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Título");
            assertThat(sheet.getRow(4).getCell(0).getStringCellValue()).isEqualTo("Python desde cero");

            var header = sheet.getRow(3).getCell(0).getCellStyle();
            assertThat(header.getFillPattern()).isEqualTo(FillPatternType.SOLID_FOREGROUND);
            assertThat(workbook.getFontAt(header.getFontIndex()).getBold()).isTrue();
            assertThat(header.getBorderBottom()).isNotNull();

            var body = sheet.getRow(4).getCell(0).getCellStyle();
            assertThat(body.getFillPattern()).isEqualTo(FillPatternType.SOLID_FOREGROUND);
            assertThat(body.getFillForegroundColorColor().getARGB())
                    .containsExactly((byte) 0xFF, (byte) 0xF5, (byte) 0xF5, (byte) 0xF3);

            assertThat(sheet.getPaneInformation()).isNotNull();
            var filter = CellRangeAddress.valueOf(sheet.getCTWorksheet().getAutoFilter().getRef());
            assertThat(filter.getFirstRow()).isEqualTo(3);
            assertThat(filter.getLastColumn()).isEqualTo(6);
            assertThat(sheet.getPrintSetup().getLandscape()).isTrue();
        }
    }

    @Test
    @DisplayName("el Excel detalla la marca, alinea los valores y firma el pie")
    void xlsxDetallaMarcaYAlineaValores() throws Exception {
        Curso curso = mock(Curso.class);
        when(curso.getTitulo()).thenReturn("Python desde cero");
        when(curso.getPosicion()).thenReturn(7);
        when(courses.buscarAllPorOrdenarPorPosicionAsc()).thenReturn(List.of(curso));

        ResultadoExportacion resultado = service.exportacion("cursos", "xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(resultado.bytes()))) {
            var sheet = workbook.getSheet("Datos");
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue())
                    .contains("1 registro").contains("Plataforma LSP");
            assertThat(sheet.getRow(2).getCell(0).getCellStyle().getFillForegroundColorColor().getARGB())
                    .containsExactly((byte) 0xFF, (byte) 0x00, (byte) 0xFF, (byte) 0x00);
            assertThat(sheet.getNumMergedRegions()).isEqualTo(2);
            assertThat(sheet.getRow(3).getCell(0).getCellStyle().getAlignment())
                    .isEqualTo(HorizontalAlignment.LEFT);
            assertThat(sheet.getRow(4).getCell(6).getCellStyle().getAlignment())
                    .isEqualTo(HorizontalAlignment.RIGHT);
            assertThat(sheet.getFooter().getCenter()).contains("Plataforma Accesible LSP");
        }
    }

    private boolean hasImagen(byte[] file) throws IOException {
        try (var zip = new java.util.zip.ZipInputStream(new ByteArrayInputStream(file))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().startsWith("xl/media/")) return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("el PDF lleva el logo, la tabla con estilo y la paginación")
    void pdfHasBrandedTable() {
        cursosDePrueba();
        ResultadoExportacion resultado = service.exportacion("cursos", "pdf");

        String text = new String(resultado.bytes(), StandardCharsets.ISO_8859_1);
        assertThat(text).startsWith("%PDF-1.4");
        assertThat(text).contains("/Subtype /Image");
        assertThat(text).contains("/Filter /FlateDecode");
        assertThat(text).contains("/Im0 Do");
        assertThat(text).contains("Mundo Manitas");
        assertThat(text).contains("Informe de cursos");
        assertThat(text).contains("Generado");
        assertThat(text).contains("Título");
        assertThat(text).contains("0.96 0.96 0.95 rg");
        assertThat(text).contains("Página 1 / 1");
    }
}
