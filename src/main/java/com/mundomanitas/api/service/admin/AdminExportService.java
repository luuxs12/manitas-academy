package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.ExportResult;
import com.mundomanitas.api.dto.admin.KpiCard;
import com.mundomanitas.api.dto.admin.ReportSummary;
import com.mundomanitas.api.model.AuditLog;
import com.mundomanitas.api.model.Certificate;
import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.CourseModule;
import com.mundomanitas.api.model.Exercise;
import com.mundomanitas.api.model.Payment;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.AuditLogRepository;
import com.mundomanitas.api.repository.CertificateRepository;
import com.mundomanitas.api.repository.CourseModuleRepository;
import com.mundomanitas.api.repository.CourseRepository;
import com.mundomanitas.api.repository.ExerciseRepository;
import com.mundomanitas.api.repository.PaymentRepository;
import com.mundomanitas.api.repository.UserRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Exporta los datos existentes a CSV, XLSX o PDF. Sin filas inventadas. */
@Service
public class AdminExportService {

    private final UserRepository users;
    private final CourseRepository courses;
    private final CourseModuleRepository modules;
    private final VideoRepository videos;
    private final ExerciseRepository exercises;
    private final CertificateRepository certificates;
    private final PaymentRepository payments;
    private final AuditLogRepository logs;
    private final AdminDashboardService dashboard;
    private final AdminFormat format;
    private final PlainPdfWriter pdf;

    public AdminExportService(
            UserRepository users,
            CourseRepository courses,
            CourseModuleRepository modules,
            VideoRepository videos,
            ExerciseRepository exercises,
            CertificateRepository certificates,
            PaymentRepository payments,
            AuditLogRepository logs,
            AdminDashboardService dashboard,
            AdminFormat format,
            PlainPdfWriter pdf
    ) {
        this.users = users;
        this.courses = courses;
        this.modules = modules;
        this.videos = videos;
        this.exercises = exercises;
        this.certificates = certificates;
        this.payments = payments;
        this.logs = logs;
        this.dashboard = dashboard;
        this.format = format;
        this.pdf = pdf;
    }

    @Transactional(readOnly = true)
    public ExportResult export(String module, String requestedFormat) {
        String name = module == null ? "" : module.toLowerCase();
        String extension = "pdf".equalsIgnoreCase(requestedFormat) ? "pdf"
                : "xlsx".equalsIgnoreCase(requestedFormat) ? "xlsx" : "csv";
        List<List<String>> data = rows(name);
        List<List<String>> rows = decorated(data, name);
        String file = "mundo-manitas-" + name + "-" + LocalDate.now() + "." + extension;
        String generatedAt = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return switch (extension) {
            case "xlsx" -> new ExportResult(xlsx(rows), file,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "pdf" -> new ExportResult(pdf.write(name, generatedAt, data), file, "application/pdf");
            default -> new ExportResult(csv(rows), file, "text/csv; charset=UTF-8");
        };
    }

    /** Antepone la marca y el nombre del informe sin alterar los datos exportados. */
    private List<List<String>> decorated(List<List<String>> rows, String module) {
        List<List<String>> result = new ArrayList<>();
        result.add(row("Mundo Manitas"));
        result.add(row("Informe de " + module));
        result.addAll(rows);
        return result;
    }

    private List<List<String>> rows(String module) {
        return switch (module) {
            case "usuarios" -> {
                List<List<String>> rows = header("Rol", "Nombre", "Correo", "Plan", "Activo", "Alta");
                for (User user : users.findAll()) {
                    rows.add(row(user.getRole(), user.getFullName(), user.getEmail(),
                            user.getPlan().getLabel(), user.isActive() ? "sí" : "no",
                            format.full(user.getCreatedAt())));
                }
                yield rows;
            }
            case "cursos" -> {
                List<List<String>> rows = header("Título", "Identificador", "Categoría", "Nivel",
                        "Lecciones", "Acceso", "Posición");
                for (Course course : courses.findAllByOrderByPositionAsc()) {
                    rows.add(row(course.getTitle(), course.getSlug(), course.getCategory(),
                            course.getLevel(), course.getLessons(), course.priceLabel(),
                            course.getPosition()));
                }
                yield rows;
            }
            case "modulos" -> {
                Map<String, String> names = courseNames();
                List<List<String>> rows = header("Curso", "Módulo", "Posición");
                for (CourseModule moduleRow : modules.findAllByOrderByPositionAsc()) {
                    rows.add(row(names.getOrDefault(moduleRow.getCourseId().toString(), "Curso eliminado"),
                            moduleRow.getTitle(), moduleRow.getPosition()));
                }
                yield rows;
            }
            case "videos" -> {
                Map<String, String> modulesNames = moduleNames();
                Map<String, String> courseNames = courseNames();
                List<List<String>> rows = header("Curso", "Módulo", "Video", "URL", "Posición");
                for (Video video : videos.findAll()) {
                    CourseModule moduleRow = modules.findById(video.getModuleId()).orElse(null);
                    rows.add(row(
                            moduleRow == null ? "Curso eliminado"
                                    : courseNames.getOrDefault(moduleRow.getCourseId().toString(), "Curso eliminado"),
                            modulesNames.getOrDefault(video.getModuleId().toString(), "Módulo eliminado"),
                            video.getTitle(), video.getUrl(), video.getPosition()));
                }
                yield rows;
            }
            case "ejercicios" -> {
                Map<String, String> modulesNames = moduleNames();
                List<List<String>> rows = header("Módulo", "Ejercicio", "Consigna", "Posición");
                for (Exercise exercise : exercises.findAll()) {
                    rows.add(row(
                            modulesNames.getOrDefault(exercise.getModuleId().toString(), "Módulo eliminado"),
                            exercise.getTitle(), exercise.getPrompt(), exercise.getPosition()));
                }
                yield rows;
            }
            case "certificados" -> {
                Map<String, String> names = userNames();
                Map<String, String> courseNames = courseNames();
                List<List<String>> rows = header("Código", "Persona", "Curso", "Emisión", "Estado");
                for (Certificate certificate : certificates.findAllByOrderByIssuedAtDesc()) {
                    rows.add(row(certificate.getCode(),
                            names.getOrDefault(certificate.getUserId().toString(), "Usuario eliminado"),
                            courseNames.getOrDefault(certificate.getCourseId().toString(), "Curso eliminado"),
                            format.full(certificate.getIssuedAt()),
                            certificate.isActive() ? "vigente" : "revocado"));
                }
                yield rows;
            }
            case "pagos" -> {
                Map<String, String> names = userNames();
                List<List<String>> rows = header("Persona", "Plan", "Monto (S/)", "Estado", "Fecha");
                for (Payment payment : payments.findAll()) {
                    rows.add(row(
                            names.getOrDefault(payment.getUser().getId().toString(), "Usuario eliminado"),
                            payment.getPlan().getLabel(), payment.getAmountCents() / 100,
                            payment.getStatus(), format.full(payment.getCreatedAt())));
                }
                yield rows;
            }
            case "auditoria" -> {
                Map<String, String> names = userNames();
                List<List<String>> rows = header("Fecha", "Quién", "Acción", "Entidad", "Detalle");
                for (AuditLog log : logs.findAllByOrderByCreatedAtDesc()) {
                    rows.add(row(format.full(log.getCreatedAt()),
                            log.getActorId() == null ? "Sistema"
                                    : names.getOrDefault(log.getActorId().toString(), "Usuario eliminado"),
                            log.getAction(), log.getEntity(), log.getDetail()));
                }
                yield rows;
            }
            case "reportes" -> {
                ReportSummary report = dashboard.reports();
                List<List<String>> rows = header("Indicador", "Valor");
                rows.add(row("Matrículas", report.enrollments()));
                rows.add(row("Finalizaciones", report.completed()));
                rows.add(row("Tasa de finalización", report.completionPercent() + "%"));
                rows.add(row("Ingresos cobrados (S/)", report.revenueCents() / 100));
                rows.add(row("Usuarios activos (30 días)", report.activeUsers()));
                rows.add(row("Retención", report.retentionPercent() + "%"));
                for (KpiCard kpi : dashboard.kpis()) {
                    rows.add(row(kpi.label(), kpi.value()));
                }
                yield rows;
            }
            default -> throw new IllegalArgumentException("El módulo solicitado no se puede exportar.");
        };
    }

    private byte[] csv(List<List<String>> rows) {
        StringBuilder text = new StringBuilder("\uFEFF");
        for (List<String> row : rows) {
            for (int index = 0; index < row.size(); index++) {
                if (index > 0) text.append(';');
                text.append('"').append(row.get(index).replace("\"", "\"\"")).append('"');
            }
            text.append("\r\n");
        }
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] xlsx(List<List<String>> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Datos");
            int columnCount = rows.isEmpty() ? 0 : rows.getFirst().size();

            Font brandFont = workbook.createFont();
            brandFont.setBold(true);
            brandFont.setColor(IndexedColors.BLACK.getIndex());
            brandFont.setFontHeightInPoints((short) 16);
            CellStyle brandStyle = greenStyle(workbook, brandFont);

            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setColor(IndexedColors.BLACK.getIndex());
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.BLACK.getIndex());
            CellStyle headerStyle = greenStyle(workbook, headerFont);

            Font bodyFont = workbook.createFont();
            bodyFont.setColor(IndexedColors.BLACK.getIndex());
            CellStyle bodyStyle = workbook.createCellStyle();
            bodyStyle.setFont(bodyFont);

            for (int index = 0; index < rows.size(); index++) {
                Row excelRow = sheet.createRow(index);
                CellStyle style = index == 0 ? brandStyle
                        : index == 1 ? titleStyle
                        : index == 2 ? headerStyle : bodyStyle;
                List<String> values = rows.get(index);
                for (int column = 0; column < values.size(); column++) {
                    Cell cell = excelRow.createCell(column);
                    cell.setCellValue(values.get(column));
                    cell.setCellStyle(style);
                }
            }
            sheet.createFreezePane(0, 3);
            if (rows.size() > 2 && columnCount > 0) {
                sheet.setAutoFilter(new CellRangeAddress(2, rows.size() - 1, 0, columnCount - 1));
            }
            for (int column = 0; column < columnCount; column++) sheet.autoSizeColumn(column);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private CellStyle greenStyle(XSSFWorkbook workbook, Font font) {
        CellStyle style = workbook.createCellStyle();
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.BRIGHT_GREEN.getIndex());
        style.setFont(font);
        return style;
    }

    private List<List<String>> header(String... names) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of(names));
        return rows;
    }

    private List<String> row(Object... values) {
        return java.util.Arrays.stream(values)
                .map(value -> value == null ? "" : String.valueOf(value))
                .toList();
    }

    private Map<String, String> userNames() {
        return users.findAll().stream().collect(Collectors.toMap(
                user -> user.getId().toString(), User::getFullName, (a, b) -> a));
    }

    private Map<String, String> courseNames() {
        return courses.findAll().stream().collect(Collectors.toMap(
                course -> course.getId().toString(), Course::getTitle, (a, b) -> a));
    }

    private Map<String, String> moduleNames() {
        return modules.findAll().stream().collect(Collectors.toMap(
                module -> module.getId().toString(), CourseModule::getTitle, (a, b) -> a));
    }
}
