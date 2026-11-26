package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.ResultadoExportacion;
import com.mundomanitas.api.dto.admin.TarjetaKpi;
import com.mundomanitas.api.dto.admin.ResumenReporte;
import com.mundomanitas.api.model.AuditoriaRegistro;
import com.mundomanitas.api.model.Certificado;
import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.model.CursoModulo;
import com.mundomanitas.api.model.Ejercicio;
import com.mundomanitas.api.model.Pago;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.AuditoriaRegistroRepository;
import com.mundomanitas.api.repository.CertificadoRepository;
import com.mundomanitas.api.repository.CursoModuloRepository;
import com.mundomanitas.api.repository.CursoRepository;
import com.mundomanitas.api.repository.EjercicioRepository;
import com.mundomanitas.api.repository.PagoRepository;
import com.mundomanitas.api.repository.UsuarioRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PageMargin;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFSheet;
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
import java.util.stream.Collectors;

/** Exporta los datos existentes a CSV, XLSX o PDF. Sin filas inventadas. */
@Service
public class AdminExportacionService {

    private final UsuarioRepository users;
    private final CursoRepository courses;
    private final CursoModuloRepository modules;
    private final VideoRepository videos;
    private final EjercicioRepository exercises;
    private final CertificadoRepository certificates;
    private final PagoRepository payments;
    private final AuditoriaRegistroRepository logs;
    private final AdminDashboardService dashboard;
    private final AdminFormato format;
    private final SimplePdfEscritor pdf;

    public AdminExportacionService(
            UsuarioRepository users,
            CursoRepository courses,
            CursoModuloRepository modules,
            VideoRepository videos,
            EjercicioRepository exercises,
            CertificadoRepository certificates,
            PagoRepository payments,
            AuditoriaRegistroRepository logs,
            AdminDashboardService dashboard,
            AdminFormato format,
            SimplePdfEscritor pdf
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
    public ResultadoExportacion exportacion(String modulo, String requestedFormato) {
        String name = modulo == null ? "" : modulo.toLowerCase();
        String extension = "pdf".equalsIgnoreCase(requestedFormato) ? "pdf"
                : "xlsx".equalsIgnoreCase(requestedFormato) ? "xlsx" : "csv";
        List<List<String>> datos = rows(name);
        String file = "mundo-manitas-" + name + "-" + LocalDate.now() + "." + extension;
        String generatedAt = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return switch (extension) {
            case "xlsx" -> new ResultadoExportacion(xlsx(name, generatedAt, datos), file,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "pdf" -> new ResultadoExportacion(pdf.write(name, generatedAt, datos), file, "application/pdf");
            default -> new ResultadoExportacion(csv(name, generatedAt, datos), file, "text/csv; charset=UTF-8");
        };
    }

    private List<List<String>> rows(String modulo) {
        return switch (modulo) {
            case "usuarios" -> {
                List<List<String>> rows = header("Rol", "Nombre", "Correo", "Plan", "Activo", "Alta");
                for (Usuario usuario : users.findAll()) {
                    rows.add(row(usuario.getRol(), usuario.getNombreCompleto(), usuario.getEmail(),
                            usuario.getPlan().getLabel(), usuario.isActivo() ? "sí" : "no",
                            format.completo(usuario.getFechaCreacion())));
                }
                yield rows;
            }
            case "cursos" -> {
                List<List<String>> rows = header("Título", "Identificador", "Categoría", "Nivel",
                        "Lecciones", "Acceso", "Posición");
                for (Curso curso : courses.buscarAllPorOrdenarPorPosicionAsc()) {
                    rows.add(row(curso.getTitulo(), curso.getSlug(), curso.getCategoria(),
                            curso.getNivel(), curso.getLessons(), curso.priceLabel(),
                            curso.getPosicion()));
                }
                yield rows;
            }
            case "modulos" -> {
                Map<String, String> names = cursoNames();
                List<List<String>> rows = header("Curso", "Módulo", "Posición");
                for (CursoModulo filaModulo : modules.buscarAllPorOrdenarPorPosicionAsc()) {
                    rows.add(row(names.getOrDefault(filaModulo.getCursoId().toString(), "Curso eliminado"),
                            filaModulo.getTitulo(), filaModulo.getPosicion()));
                }
                yield rows;
            }
            case "videos" -> {
                Map<String, String> modulesNames = moduloNames();
                Map<String, String> cursoNames = cursoNames();
                List<List<String>> rows = header("Curso", "Módulo", "Video", "YouTube", "Posición");
                for (Video video : videos.findAll()) {
                    CursoModulo filaModulo = modules.findById(video.getModuloId()).orElse(null);
                    rows.add(row(
                            filaModulo == null ? "Curso eliminado"
                                    : cursoNames.getOrDefault(filaModulo.getCursoId().toString(), "Curso eliminado"),
                            modulesNames.getOrDefault(video.getModuloId().toString(), "Módulo eliminado"),
                            video.getTitulo(), video.getYoutubeId(), video.getPosicion()));
                }
                yield rows;
            }
            case "ejercicios" -> {
                Map<String, String> modulesNames = moduloNames();
                List<List<String>> rows = header("Módulo", "Ejercicio", "Consigna", "Posición");
                for (Ejercicio ejercicio : exercises.findAll()) {
                    rows.add(row(
                            modulesNames.getOrDefault(ejercicio.getModuloId().toString(), "Módulo eliminado"),
                            ejercicio.getTitulo(), ejercicio.getPrompt(), ejercicio.getPosicion()));
                }
                yield rows;
            }
            case "certificados" -> {
                Map<String, String> names = usuarioNames();
                Map<String, String> cursoNames = cursoNames();
                List<List<String>> rows = header("Código", "Persona", "Curso", "Emisión", "Estado");
                for (Certificado certificado : certificates.buscarAllPorOrdenarPorFechaEmisionDesc()) {
                    rows.add(row(certificado.getCode(),
                            names.getOrDefault(certificado.getUsuarioId().toString(), "Usuario eliminado"),
                            cursoNames.getOrDefault(certificado.getCursoId().toString(), "Curso eliminado"),
                            format.completo(certificado.getFechaEmision()),
                            certificado.isActivo() ? "vigente" : "revocado"));
                }
                yield rows;
            }
            case "pagos" -> {
                Map<String, String> names = usuarioNames();
                List<List<String>> rows = header("Persona", "Plan", "Monto (S/)", "Estado", "Fecha");
                for (Pago pago : payments.findAll()) {
                    rows.add(row(
                            names.getOrDefault(pago.getUsuario().getId().toString(), "Usuario eliminado"),
                            pago.getPlan().getLabel(), pago.getAmountCents() / 100,
                            pago.getEstado(), format.completo(pago.getFechaCreacion())));
                }
                yield rows;
            }
            case "auditoria" -> {
                Map<String, String> names = usuarioNames();
                List<List<String>> rows = header("Fecha", "Quién", "Acción", "Entidad", "Detalle");
                for (AuditoriaRegistro log : logs.buscarAllPorOrdenarPorFechaCreacionDesc()) {
                    rows.add(row(format.completo(log.getFechaCreacion()),
                            log.getActorId() == null ? "Sistema"
                                    : names.getOrDefault(log.getActorId().toString(), "Usuario eliminado"),
                            log.getAction(), log.getEntity(), log.getDetail()));
                }
                yield rows;
            }
            case "reportes" -> {
                ResumenReporte reporte = dashboard.reports();
                List<List<String>> rows = header("Indicador", "Valor");
                rows.add(row("Matrículas", reporte.enrollments()));
                rows.add(row("Finalizaciones", reporte.finalizado()));
                rows.add(row("Tasa de finalización", reporte.completionPercent() + "%"));
                rows.add(row("Ingresos cobrados (S/)", reporte.revenueCents() / 100));
                rows.add(row("Usuarios activos (30 días)", reporte.activoUsers()));
                rows.add(row("Retención", reporte.retentionPercent() + "%"));
                for (TarjetaKpi kpi : dashboard.kpis()) {
                    rows.add(row(kpi.label(), kpi.value()));
                }
                yield rows;
            }
            default -> throw new IllegalArgumentException("El módulo solicitado no se puede exportar.");
        };
    }

    /** El CSV no admite imágenes ni colores: identifica el informe con texto y metadatos. */
    private byte[] csv(String modulo, String generatedAt, List<List<String>> table) {
        StringBuilder text = new StringBuilder("\uFEFF");
        appendCsvFila(text, List.of("Mundo Manitas"));
        appendCsvFila(text, List.of("Informe de " + modulo));
        appendCsvFila(text, List.of("Generado", generatedAt));
        appendCsvFila(text, List.of("Registros", String.valueOf(Math.max(0, table.size() - 1))));
        appendCsvFila(text, List.of(""));
        for (List<String> row : table) appendCsvFila(text, row);
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Las celdas que empiezan con signo de fórmula se neutralizan con apóstrofo. */
    private void appendCsvFila(StringBuilder text, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) text.append(';');
            String value = values.get(index) == null ? "" : values.get(index);
            String trimmed = value.stripLeading();
            if (trimmed.startsWith("=") || trimmed.startsWith("+") || trimmed.startsWith("-")
                    || trimmed.startsWith("@")) {
                value = "'" + value;
            }
            text.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        text.append("\r\n");
    }

    private byte[] xlsx(String module, String generatedAt, List<List<String>> table) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Datos");
            List<String> headers = table.isEmpty() ? List.of() : table.getFirst();
            List<List<String>> data = table.size() > 1
                    ? table.subList(1, table.size()) : List.of();
            int columnCount = Math.max(1, headers.size());
            int bandColumns = Math.max(columnCount + 2, 6);

            buildBrandHeader(sheet, workbook, module, generatedAt, data.size(), bandColumns);
            buildTableHeader(sheet, workbook, headers, columnCount);
            buildTableData(sheet, workbook, data, columnCount);
            adjustColumnWidths(sheet, headers, data, columnCount);
            insertBrandedLogo(sheet, workbook);
            configureSheetLayout(sheet, data.size(), columnCount);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void buildBrandHeader(XSSFSheet sheet, XSSFWorkbook workbook, String module,
                                  String generatedAt, int records, int bandColumns) {
        Font brandFont = font(workbook, "Tanker", 20, true);
        CellStyle brand = style(workbook, brandFont, "FFFFFF", false);
        brand.setVerticalAlignment(VerticalAlignment.CENTER);

        Font subtitleFont = font(workbook, "Open Sans", 10, false);
        CellStyle subtitle = style(workbook, subtitleFont, "FFFFFF", false);
        subtitle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle accentDivider = style(workbook, subtitleFont, "00FF00", false);

        Row brandRow = sheet.createRow(0);
        brandRow.setHeightInPoints(54);
        Row subtitleRow = sheet.createRow(1);
        subtitleRow.setHeightInPoints(26);

        for (int column = 0; column < bandColumns; column++) {
            Cell brandCell = brandRow.createCell(column);
            brandCell.setCellStyle(brand);
            if (column == 3) brandCell.setCellValue("Mundo Manitas");

            Cell subtitleCell = subtitleRow.createCell(column);
            subtitleCell.setCellStyle(subtitle);
            if (column == 3) {
                subtitleCell.setCellValue("Informe de " + module + " · Generado: " + generatedAt
                        + " · " + records + (records == 1 ? " registro" : " registros") + " · Plataforma LSP");
            }
        }
        if (bandColumns > 4) {
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 3, bandColumns - 1));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 3, bandColumns - 1));
        }

        Row dividerRow = sheet.createRow(2);
        dividerRow.setHeightInPoints(6);
        for (int column = 0; column < bandColumns; column++) {
            Cell cell = dividerRow.createCell(column);
            cell.setCellStyle(accentDivider);
        }
    }

    private void buildTableHeader(XSSFSheet sheet, XSSFWorkbook workbook,
                                  List<String> headers, int columnCount) {
        Font headerFont = font(workbook, "Open Sans", 10, true);
        CellStyle header = style(workbook, headerFont, "00FF00", true);
        header.setAlignment(HorizontalAlignment.LEFT);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        header.setBorderBottom(BorderStyle.MEDIUM);
        header.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        header.setBorderTop(BorderStyle.THIN);
        header.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        header.setBorderLeft(BorderStyle.THIN);
        header.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        header.setBorderRight(BorderStyle.THIN);
        header.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());

        Row headerRow = sheet.createRow(3);
        headerRow.setHeightInPoints(28);
        for (int column = 0; column < columnCount; column++) {
            Cell cell = headerRow.createCell(column);
            cell.setCellValue(column < headers.size() ? headers.get(column) : "");
            cell.setCellStyle(header);
        }
    }

    private void buildTableData(XSSFSheet sheet, XSSFWorkbook workbook,
                                List<List<String>> data, int columnCount) {
        Font bodyFont = font(workbook, "Open Sans", 10, false);
        CellStyle shaded = styleDataCell(workbook, bodyFont, "F5F5F3", HorizontalAlignment.LEFT);
        CellStyle plain = styleDataCell(workbook, bodyFont, "FFFFFF", HorizontalAlignment.LEFT);
        CellStyle shadedCenter = styleDataCell(workbook, bodyFont, "F5F5F3", HorizontalAlignment.CENTER);
        CellStyle plainCenter = styleDataCell(workbook, bodyFont, "FFFFFF", HorizontalAlignment.CENTER);
        CellStyle shadedRight = styleDataCell(workbook, bodyFont, "F5F5F3", HorizontalAlignment.RIGHT);
        CellStyle plainRight = styleDataCell(workbook, bodyFont, "FFFFFF", HorizontalAlignment.RIGHT);

        for (int index = 0; index < data.size(); index++) {
            Row excelRow = sheet.createRow(4 + index);
            excelRow.setHeightInPoints(21);
            List<String> values = data.get(index);
            boolean isEven = (index % 2 == 0);
            for (int column = 0; column < columnCount; column++) {
                Cell cell = excelRow.createCell(column);
                String val = column < values.size() ? values.get(column) : "";
                cell.setCellValue(val);
                if (isNumeric(val)) {
                    cell.setCellStyle(isEven ? shadedRight : plainRight);
                } else if (isCentered(val)) {
                    cell.setCellStyle(isEven ? shadedCenter : plainCenter);
                } else {
                    cell.setCellStyle(isEven ? shaded : plain);
                }
            }
        }
        if (data.isEmpty()) {
            Row emptyRow = sheet.createRow(4);
            emptyRow.setHeightInPoints(24);
            Cell cell = emptyRow.createCell(0);
            cell.setCellValue("No hay datos para este informe.");
            cell.setCellStyle(shaded);
        }
    }

    private void adjustColumnWidths(XSSFSheet sheet, List<String> headers,
                                    List<List<String>> data, int columnCount) {
        for (int column = 0; column < columnCount; column++) {
            int longest = column < headers.size() ? headers.get(column).length() : 8;
            for (List<String> values : data) {
                if (column < values.size()) longest = Math.max(longest, values.get(column).length());
            }
            sheet.setColumnWidth(column,
                    Math.min(13000, Math.max(column < 2 ? 4000 : 2800, (longest + 4) * 256)));
        }
    }

    private void insertBrandedLogo(XSSFSheet sheet, XSSFWorkbook workbook) {
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, 0, 0, 2, 2);
        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
        XSSFPicture picture = drawing.createPicture(anchor,
                workbook.addPicture(ExportacionMarca.logoPng(), Workbook.PICTURE_TYPE_PNG));
        picture.resize(0.062);
    }

    private void configureSheetLayout(XSSFSheet sheet, int dataSize, int columnCount) {
        sheet.createFreezePane(0, 4);
        sheet.setAutoFilter(new CellRangeAddress(3, Math.max(4, 3 + dataSize), 0, columnCount - 1));
        sheet.setRepeatingRows(CellRangeAddress.valueOf("$4:$4"));
        sheet.setDisplayGridlines(true);
        sheet.setPrintGridlines(true);
        sheet.getPrintSetup().setLandscape(true);
        sheet.getPrintSetup().setScale((short) 95);
        sheet.setMargin(PageMargin.LEFT, 0.4);
        sheet.setMargin(PageMargin.RIGHT, 0.4);
        sheet.getFooter().setCenter("Mundo Manitas · Plataforma Accesible LSP · Página &P de &N");
    }

    private CellStyle styleDataCell(XSSFWorkbook workbook, Font font, String hexColor, HorizontalAlignment align) {
        CellStyle style = style(workbook, font, hexColor, true);
        style.setAlignment(align);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBorderLeft(BorderStyle.THIN);
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private boolean isNumeric(String val) {
        if (val == null || val.isBlank()) return false;
        return val.matches("^\\d+(\\.\\d+)?%?$") || val.matches("^S/\\s*\\d+(\\.\\d+)?$");
    }

    private boolean isCentered(String val) {
        if (val == null || val.isBlank()) return false;
        return val.matches("^\\d{2}/\\d{2}/\\d{4}$") || val.matches("^[0-9a-fA-F-]{36}$")
                || val.equalsIgnoreCase("gratis") || val.equalsIgnoreCase("premium")
                || val.equalsIgnoreCase("publicado") || val.equalsIgnoreCase("activo");
    }

    private Font font(XSSFWorkbook workbook, String name, int size, boolean bold) {
        Font font = workbook.createFont();
        font.setFontName(name);
        font.setFontHeightInPoints((short) size);
        font.setBold(bold);
        font.setColor(IndexedColors.BLACK.getIndex());
        return font;
    }

    private CellStyle style(XSSFWorkbook workbook, Font font, String rgb, boolean border) {
        CellStyle style = workbook.createCellStyle();
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(new XSSFColor(java.util.HexFormat.of().parseHex(rgb), null));
        style.setFont(font);
        if (border) {
            style.setBorderBottom(BorderStyle.THIN);
            style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        }
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

    private Map<String, String> usuarioNames() {
        return users.findAll().stream().collect(Collectors.toMap(
                usuario -> usuario.getId().toString(), Usuario::getNombreCompleto, (a, b) -> a));
    }

    private Map<String, String> cursoNames() {
        return courses.findAll().stream().collect(Collectors.toMap(
                curso -> curso.getId().toString(), Curso::getTitulo, (a, b) -> a));
    }

    private Map<String, String> moduloNames() {
        return modules.findAll().stream().collect(Collectors.toMap(
                modulo -> modulo.getId().toString(), CursoModulo::getTitulo, (a, b) -> a));
    }
}
