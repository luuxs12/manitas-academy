package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.ExportResult;
import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.repository.AuditLogRepository;
import com.mundomanitas.api.repository.CertificateRepository;
import com.mundomanitas.api.repository.CourseModuleRepository;
import com.mundomanitas.api.repository.CourseRepository;
import com.mundomanitas.api.repository.ExerciseRepository;
import com.mundomanitas.api.repository.PaymentRepository;
import com.mundomanitas.api.repository.UserRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.apache.poi.ss.usermodel.FillPatternType;
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
class AdminExportServiceTest {

    @Mock private UserRepository users;
    @Mock private CourseRepository courses;
    @Mock private CourseModuleRepository modules;
    @Mock private VideoRepository videos;
    @Mock private ExerciseRepository exercises;
    @Mock private CertificateRepository certificates;
    @Mock private PaymentRepository payments;
    @Mock private AuditLogRepository logs;
    @Mock private AdminDashboardService dashboard;
    @Mock private AdminFormat format;

    private AdminExportService service;

    @BeforeEach
    void setUp() {
        service = new AdminExportService(users, courses, modules, videos, exercises,
                certificates, payments, logs, dashboard, format, new PlainPdfWriter());
        Course course = mock(Course.class);
        when(course.getTitle()).thenReturn("Python desde cero");
        when(courses.findAllByOrderByPositionAsc()).thenReturn(List.of(course));
    }

    @Test
    @DisplayName("el CSV abre con la marca y el nombre del informe")
    void csvCarriesBrandHeader() {
        ExportResult result = service.export("cursos", "csv");

        String text = new String(result.bytes(), StandardCharsets.UTF_8);
        assertThat(text).startsWith("\uFEFF\"Mundo Manitas\"");
        assertThat(text).contains("Informe de cursos");
        assertThat(text).contains("\"Título\";\"Identificador\"");
    }

    @Test
    @DisplayName("el Excel lleva el logo, banda de marca y tabla con cebra y filtro completo")
    void xlsxHasBrandedTable() throws Exception {
        ExportResult result = service.export("cursos", "xlsx");

        assertThat(hasImage(result.bytes())).isTrue();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.bytes()))) {
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

    private boolean hasImage(byte[] file) throws IOException {
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
        ExportResult result = service.export("cursos", "pdf");

        String text = new String(result.bytes(), StandardCharsets.ISO_8859_1);
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
