package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.GraficoActividad;
import com.mundomanitas.api.dto.admin.ConteoDia;
import com.mundomanitas.api.dto.admin.GraficoIngresos;
import com.mundomanitas.api.dto.admin.TarjetaKpi;
import com.mundomanitas.api.dto.admin.CursoPopular;
import com.mundomanitas.api.dto.admin.UsuarioReciente;
import com.mundomanitas.api.dto.admin.ResumenReporte;
import com.mundomanitas.api.dto.admin.VerificacionSistema;
import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.AdminStatsRepository;
import com.mundomanitas.api.repository.CertificadoRepository;
import com.mundomanitas.api.repository.CursoRepository;
import com.mundomanitas.api.repository.InscripcionRepository;
import com.mundomanitas.api.repository.LeccionProgresoRepository;
import com.mundomanitas.api.repository.PagoRepository;
import com.mundomanitas.api.repository.UsuarioRepository;
import org.springframework.boot.SpringBootVersion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Indicadores del panel y de reportes; todo se lee de PostgreSQL. */
@Service
public class AdminDashboardService {

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("dd/MM");
    private static final String ICON_USERS =
            "M16 20v-1.5a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4V20M9.5 10.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7M17 4a3 3 0 0 1 0 6";
    private static final String ICON_COURSES =
            "M4 5.5A2.5 2.5 0 0 1 6.5 3H20v15H6.5A2.5 2.5 0 0 0 4 20.5zM4 20.5A2.5 2.5 0 0 1 6.5 18H20v3H6.5A2.5 2.5 0 0 1 4 20.5z";
    private static final String ICON_LESSONS =
            "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18M10 8.5l6 3.5-6 3.5z";
    private static final String ICON_CERTIFICATES =
            "M12 4a5 5 0 1 0 0 10 5 5 0 0 0 0-10M8.5 14l-1 7 4.5-2.5 4.5 2.5-1-7";

    private final UsuarioRepository users;
    private final CursoRepository courses;
    private final InscripcionRepository enrollments;
    private final LeccionProgresoRepository lessons;
    private final CertificadoRepository certificates;
    private final PagoRepository payments;
    private final AdminStatsRepository stats;
    private final AdminFormato format;

    public AdminDashboardService(
            UsuarioRepository users,
            CursoRepository courses,
            InscripcionRepository enrollments,
            LeccionProgresoRepository lessons,
            CertificadoRepository certificates,
            PagoRepository payments,
            AdminStatsRepository stats,
            AdminFormato format
    ) {
        this.users = users;
        this.courses = courses;
        this.enrollments = enrollments;
        this.lessons = lessons;
        this.certificates = certificates;
        this.payments = payments;
        this.stats = stats;
        this.format = format;
    }

    @Transactional(readOnly = true)
    public List<TarjetaKpi> kpis() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime from = now.minusDays(30);
        OffsetDateTime previousFrom = from.minusDays(30);

        long usuarioContar = users.count();
        long cursoContar = courses.count();
        long leccionContar = lessons.count();
        long certificadoContar = certificates.contarPorFechaRevocacionEsNulo();

        return List.of(
                new TarjetaKpi("Usuarios registrados", String.valueOf(usuarioContar),
                        trend(users.contarPorFechaCreacionEntre(from, now),
                                users.contarPorFechaCreacionEntre(previousFrom, from)),
                        ICON_USERS, "Altas en los últimos 30 días"),
                new TarjetaKpi("Cursos publicados", String.valueOf(cursoContar),
                        trend(courses.contarPorFechaCreacionEntre(from, now),
                                courses.contarPorFechaCreacionEntre(previousFrom, from)),
                        ICON_COURSES, "Fichas activas del catálogo"),
                new TarjetaKpi("Lecciones vistas", String.valueOf(leccionContar),
                        trend(lessons.contarPorFechaFinalizacionEntre(from, now),
                                lessons.contarPorFechaFinalizacionEntre(previousFrom, from)),
                        ICON_LESSONS, "Lecciones completadas por el alumnado"),
                new TarjetaKpi("Certificados emitidos", String.valueOf(certificadoContar),
                        trend(certificates.contarPorFechaEmisionEntre(from, now),
                                certificates.contarPorFechaEmisionEntre(previousFrom, from)),
                        ICON_CERTIFICATES, "Emitidos y vigentes")
        );
    }

    @Transactional(readOnly = true)
    public GraficoActividad actividad(int requestedDays) {
        int days = requestedDays == 7 || requestedDays == 90 ? requestedDays : 30;
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1L);
        OffsetDateTime fromTime = from.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());

        Map<LocalDate, Long> usuarioSeries = toMap(stats.usersPorDia(fromTime));
        Map<LocalDate, Long> cursoSeries = toMap(stats.coursesPorDia(fromTime));

        int max = 1;
        for (long value : usuarioSeries.values()) max = (int) Math.max(max, value);
        for (long value : cursoSeries.values()) max = (int) Math.max(max, value);

        List<String> etiquetas = new ArrayList<>();
        List<String> usuarioPoints = new ArrayList<>();
        List<String> cursoPoints = new ArrayList<>();
        int step = days == 7 ? 1 : days == 30 ? 5 : 15;
        int last = days - 1;
        for (int index = 0; index < days; index++) {
            LocalDate dia = from.plusDays(index);
            etiquetas.add(index % step == 0 || index == last ? dia.format(LABEL) : "");
            usuarioPoints.add(point(index, usuarioSeries.getOrDefault(dia, 0L), max, last));
            cursoPoints.add(point(index, cursoSeries.getOrDefault(dia, 0L), max, last));
        }
        return new GraficoActividad(days, from, today, etiquetas, usuarioPoints, cursoPoints, max,
                usuarioSeries.values().stream().mapToLong(Long::longValue).sum(),
                cursoSeries.values().stream().mapToLong(Long::longValue).sum());
    }

    @Transactional(readOnly = true)
    public GraficoIngresos ingresos(int requestedDays) {
        int days = requestedDays == 7 || requestedDays == 90 ? requestedDays : 30;
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1L);
        OffsetDateTime fromTime = from.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());

        Map<LocalDate, Long> values = toMap(stats.revenuePorDia(fromTime));
        long max = 1;
        for (long value : values.values()) max = Math.max(max, value);
        long total = values.values().stream().mapToLong(Long::longValue).sum();

        int step = days == 7 ? 1 : days == 30 ? 5 : 15;
        int last = days - 1;
        List<String> etiquetas = new ArrayList<>();
        List<String> points = new ArrayList<>();
        for (int index = 0; index < days; index++) {
            LocalDate dia = from.plusDays(index);
            etiquetas.add(index % step == 0 || index == last ? dia.format(LABEL) : "");
            long value = values.getOrDefault(dia, 0L);
            int x = last == 0 ? 40 : 40 + (int) Math.round(index * 820.0 / last);
            int y = 210 - (int) Math.round(value * 170.0 / max);
            points.add(x + "," + y);
        }
        return new GraficoIngresos(from, today, etiquetas, points, total, max);
    }

    @Transactional(readOnly = true)
    public List<CursoPopular> popular() {
        Map<Curso, List<Inscripcion>> grouped = enrollments.findAll().stream()
                .collect(Collectors.groupingBy(Inscripcion::getCurso, LinkedHashMap::new,
                        Collectors.toList()));
        return grouped.entrySet().stream()
                .map(entry -> {
                    Curso curso = entry.getKey();
                    long count = entry.getValue().size();
                    long finalizadoLessons = entry.getValue().stream()
                            .mapToLong(inscripcion -> lessons.contarPorInscripcionId(inscripcion.getId()))
                            .sum();
                    long possible = (long) curso.getLessons() * count;
                    int percent = possible == 0 ? 0
                            : (int) Math.min(100, Math.round(finalizadoLessons * 100.0 / possible));
                    return new CursoPopular(curso.getId(), curso.getSlug(), curso.getTitulo(),
                            curso.getImagen(), count, percent);
                })
                .sorted(Comparator.comparingLong(CursoPopular::inscripto).reversed())
                .limit(3)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioReciente> recienteUsers() {
        return users.buscarTop5PorOrdenarPorFechaCreacionDesc().stream()
                .map(usuario -> new UsuarioReciente(usuario.getId(), usuario.getNombreCompleto(), usuario.getEmail(),
                        usuario.getRol(), format.initials(usuario.getNombreCompleto()),
                        format.relative(usuario.getFechaCreacion())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VerificacionSistema> sistema() {
        List<VerificacionSistema> checks = new ArrayList<>();
        try {
            long total = users.count();
            checks.add(new VerificacionSistema("Base de datos", total + " cuentas respondiendo", true));
        } catch (Exception exception) {
            checks.add(new VerificacionSistema("Base de datos", "Sin respuesta de PostgreSQL", false));
        }
        long hours = ManagementFactory.getRuntimeMXBean().getUptime() / 3_600_000;
        checks.add(new VerificacionSistema("Servidor", "activo desde hace " + hours + " h", true));
        checks.add(new VerificacionSistema("Plataforma", "Spring Boot " + SpringBootVersion.getVersion()
                + " · Java " + System.getProperty("java.version"), true));

        String lastUpdate = users.findAll().stream()
                .map(Usuario::getFechaActualizacion)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(format::completo)
                .orElse("Sin actualizaciones registradas");
        checks.add(new VerificacionSistema("Última actualización", lastUpdate, true));
        return checks;
    }

    @Transactional(readOnly = true)
    public ResumenReporte reports() {
        long total = enrollments.count();
        long finalizado = enrollments.contarPorFechaFinalizacionNoEsNulo();
        long activo = stats.activoUsersSince(OffsetDateTime.now().minusDays(30));
        long allUsers = users.count();
        return new ResumenReporte(
                total,
                finalizado,
                percent(finalizado, total),
                payments.pagadoTotalCents(),
                activo,
                allUsers,
                percent(activo, allUsers));
    }

    public int days(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed == 7 || parsed == 90 ? parsed : 30;
        } catch (Exception exception) {
            return 30;
        }
    }

    private String point(int index, long value, int max, int last) {
        int x = last == 0 ? 40 : 40 + (int) Math.round(index * 820.0 / last);
        int y = 210 - (int) Math.round(value * 170.0 / max);
        return x + "," + y;
    }

    private Map<LocalDate, Long> toMap(List<ConteoDia> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        for (ConteoDia row : rows) map.put(row.dia(), row.total());
        return map;
    }

    private int percent(long part, long total) {
        return total == 0 ? 0 : (int) Math.min(100, Math.round(part * 100.0 / total));
    }

    private String trend(long actual, long previous) {
        if (previous == 0) return actual == 0 ? "0%" : "nuevo";
        long change = Math.round((actual - previous) * 100.0 / previous);
        return (change > 0 ? "+" : "") + change + "%";
    }
}
