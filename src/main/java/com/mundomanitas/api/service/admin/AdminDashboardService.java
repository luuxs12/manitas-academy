package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.ActivityChart;
import com.mundomanitas.api.dto.admin.DayCount;
import com.mundomanitas.api.dto.admin.IncomeChart;
import com.mundomanitas.api.dto.admin.KpiCard;
import com.mundomanitas.api.dto.admin.PopularCourse;
import com.mundomanitas.api.dto.admin.RecentUser;
import com.mundomanitas.api.dto.admin.ReportSummary;
import com.mundomanitas.api.dto.admin.SystemCheck;
import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.AdminStatsRepository;
import com.mundomanitas.api.repository.CertificateRepository;
import com.mundomanitas.api.repository.CourseRepository;
import com.mundomanitas.api.repository.EnrollmentRepository;
import com.mundomanitas.api.repository.LessonProgressRepository;
import com.mundomanitas.api.repository.PaymentRepository;
import com.mundomanitas.api.repository.UserRepository;
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
import java.util.function.Function;
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

    private final UserRepository users;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final LessonProgressRepository lessons;
    private final CertificateRepository certificates;
    private final PaymentRepository payments;
    private final AdminStatsRepository stats;
    private final AdminFormat format;

    public AdminDashboardService(
            UserRepository users,
            CourseRepository courses,
            EnrollmentRepository enrollments,
            LessonProgressRepository lessons,
            CertificateRepository certificates,
            PaymentRepository payments,
            AdminStatsRepository stats,
            AdminFormat format
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
    public List<KpiCard> kpis() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime from = now.minusDays(30);
        OffsetDateTime previousFrom = from.minusDays(30);

        long userCount = users.count();
        long courseCount = courses.count();
        long lessonCount = lessons.count();
        long certificateCount = certificates.countByRevokedAtIsNull();

        return List.of(
                new KpiCard("Usuarios registrados", String.valueOf(userCount),
                        trend(users.countByCreatedAtBetween(from, now),
                                users.countByCreatedAtBetween(previousFrom, from)),
                        ICON_USERS, "Altas en los últimos 30 días"),
                new KpiCard("Cursos publicados", String.valueOf(courseCount),
                        trend(courses.countByCreatedAtBetween(from, now),
                                courses.countByCreatedAtBetween(previousFrom, from)),
                        ICON_COURSES, "Fichas activas del catálogo"),
                new KpiCard("Lecciones vistas", String.valueOf(lessonCount),
                        trend(lessons.countByCompletedAtBetween(from, now),
                                lessons.countByCompletedAtBetween(previousFrom, from)),
                        ICON_LESSONS, "Lecciones completadas por el alumnado"),
                new KpiCard("Certificados emitidos", String.valueOf(certificateCount),
                        trend(certificates.countByIssuedAtBetween(from, now),
                                certificates.countByIssuedAtBetween(previousFrom, from)),
                        ICON_CERTIFICATES, "Emitidos y vigentes")
        );
    }

    @Transactional(readOnly = true)
    public ActivityChart activity(int requestedDays) {
        int days = requestedDays == 7 || requestedDays == 90 ? requestedDays : 30;
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1L);
        OffsetDateTime fromTime = from.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());

        Map<LocalDate, Long> userSeries = toMap(stats.usersByDay(fromTime));
        Map<LocalDate, Long> courseSeries = toMap(stats.coursesByDay(fromTime));

        int max = 1;
        for (long value : userSeries.values()) max = (int) Math.max(max, value);
        for (long value : courseSeries.values()) max = (int) Math.max(max, value);

        List<String> labels = new ArrayList<>();
        List<String> userPoints = new ArrayList<>();
        List<String> coursePoints = new ArrayList<>();
        int step = days == 7 ? 1 : days == 30 ? 5 : 15;
        int last = days - 1;
        for (int index = 0; index < days; index++) {
            LocalDate day = from.plusDays(index);
            labels.add(index % step == 0 || index == last ? day.format(LABEL) : "");
            userPoints.add(point(index, userSeries.getOrDefault(day, 0L), max, last));
            coursePoints.add(point(index, courseSeries.getOrDefault(day, 0L), max, last));
        }
        return new ActivityChart(days, from, today, labels, userPoints, coursePoints, max,
                userSeries.values().stream().mapToLong(Long::longValue).sum(),
                courseSeries.values().stream().mapToLong(Long::longValue).sum());
    }

    @Transactional(readOnly = true)
    public IncomeChart income(int requestedDays) {
        int days = requestedDays == 7 || requestedDays == 90 ? requestedDays : 30;
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1L);
        OffsetDateTime fromTime = from.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());

        Map<LocalDate, Long> values = toMap(stats.revenueByDay(fromTime));
        long max = 1;
        for (long value : values.values()) max = Math.max(max, value);
        long total = values.values().stream().mapToLong(Long::longValue).sum();

        int step = days == 7 ? 1 : days == 30 ? 5 : 15;
        int last = days - 1;
        List<String> labels = new ArrayList<>();
        List<String> points = new ArrayList<>();
        for (int index = 0; index < days; index++) {
            LocalDate day = from.plusDays(index);
            labels.add(index % step == 0 || index == last ? day.format(LABEL) : "");
            long value = values.getOrDefault(day, 0L);
            int x = last == 0 ? 40 : 40 + (int) Math.round(index * 820.0 / last);
            int y = 210 - (int) Math.round(value * 170.0 / max);
            points.add(x + "," + y);
        }
        return new IncomeChart(from, today, labels, points, total, max);
    }

    @Transactional(readOnly = true)
    public List<PopularCourse> popular() {
        Map<Course, List<Enrollment>> grouped = enrollments.findAll().stream()
                .collect(Collectors.groupingBy(Enrollment::getCourse, LinkedHashMap::new,
                        Collectors.toList()));
        return grouped.entrySet().stream()
                .map(entry -> {
                    Course course = entry.getKey();
                    long count = entry.getValue().size();
                    long completedLessons = entry.getValue().stream()
                            .mapToLong(enrollment -> lessons.countByEnrollmentId(enrollment.getId()))
                            .sum();
                    long possible = (long) course.getLessons() * count;
                    int percent = possible == 0 ? 0
                            : (int) Math.min(100, Math.round(completedLessons * 100.0 / possible));
                    return new PopularCourse(course.getId(), course.getSlug(), course.getTitle(),
                            course.getImage(), count, percent);
                })
                .sorted(Comparator.comparingLong(PopularCourse::enrolled).reversed())
                .limit(3)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecentUser> recentUsers() {
        return users.findTop5ByOrderByCreatedAtDesc().stream()
                .map(user -> new RecentUser(user.getId(), user.getFullName(), user.getEmail(),
                        user.getRole(), format.initials(user.getFullName()),
                        format.relative(user.getCreatedAt())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SystemCheck> system() {
        List<SystemCheck> checks = new ArrayList<>();
        try {
            long total = users.count();
            checks.add(new SystemCheck("Base de datos", total + " cuentas respondiendo", true));
        } catch (Exception exception) {
            checks.add(new SystemCheck("Base de datos", "Sin respuesta de PostgreSQL", false));
        }
        long hours = ManagementFactory.getRuntimeMXBean().getUptime() / 3_600_000;
        checks.add(new SystemCheck("Servidor", "activo desde hace " + hours + " h", true));
        checks.add(new SystemCheck("Plataforma", "Spring Boot " + SpringBootVersion.getVersion()
                + " · Java " + System.getProperty("java.version"), true));

        String lastUpdate = users.findAll().stream()
                .map(User::getUpdatedAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(format::full)
                .orElse("Sin actualizaciones registradas");
        checks.add(new SystemCheck("Última actualización", lastUpdate, true));
        return checks;
    }

    @Transactional(readOnly = true)
    public ReportSummary reports() {
        long total = enrollments.count();
        long completed = enrollments.countByCompletedAtIsNotNull();
        long active = stats.activeUsersSince(OffsetDateTime.now().minusDays(30));
        long allUsers = users.count();
        return new ReportSummary(
                total,
                completed,
                percent(completed, total),
                payments.paidTotalCents(),
                active,
                allUsers,
                percent(active, allUsers));
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

    private Map<LocalDate, Long> toMap(List<DayCount> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        for (DayCount row : rows) map.put(row.day(), row.total());
        return map;
    }

    private int percent(long part, long total) {
        return total == 0 ? 0 : (int) Math.min(100, Math.round(part * 100.0 / total));
    }

    private String trend(long current, long previous) {
        if (previous == 0) return current == 0 ? "0%" : "nuevo";
        long change = Math.round((current - previous) * 100.0 / previous);
        return (change > 0 ? "+" : "") + change + "%";
    }
}
