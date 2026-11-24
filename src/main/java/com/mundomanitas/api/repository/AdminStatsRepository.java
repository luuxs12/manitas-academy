package com.mundomanitas.api.repository;

import com.mundomanitas.api.dto.admin.DayCount;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Consultas de agregación para el panel. Las fechas se agrupan en PostgreSQL;
 * Java solo arma la serie continua de días para el gráfico.
 */
@Repository
public class AdminStatsRepository {

    private static final String USERS =
            "select cast(date_trunc('day', created_at) as date) as day, count(*) as total "
                    + "from users where created_at >= :from group by 1 order by 1";
    private static final String COURSES =
            "select cast(date_trunc('day', created_at) as date) as day, count(*) as total "
                    + "from courses where created_at >= :from group by 1 order by 1";

    private final EntityManager entityManager;

    public AdminStatsRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<DayCount> usersByDay(OffsetDateTime from) {
        return daily(USERS, from);
    }

    public List<DayCount> coursesByDay(OffsetDateTime from) {
        return daily(COURSES, from);
    }

    public List<DayCount> revenueByDay(OffsetDateTime from) {
        String sql = """
                select cast(date_trunc('day', paid_at) as date) as day,
                       sum(amount_cents) as total
                  from payments
                 where status = 'PAID' and paid_at >= :from
                 group by 1 order by 1
                """;
        return daily(sql, from);
    }

    /** Personas con movimiento real en el período (matrícula o avance). */
    public long activeUsersSince(OffsetDateTime from) {
        String sql = """
                select count(distinct user_id) from (
                    select user_id from enrollments where enrolled_at >= :from
                    union
                    select e.user_id from lesson_progress lp
                    join enrollments e on e.id = lp.enrollment_id
                    where lp.completed_at >= :from
                ) activity
                """;
        Object value = entityManager.createNativeQuery(sql)
                .setParameter("from", from)
                .getSingleResult();
        return value instanceof Number number ? number.longValue() : 0L;
    }

    @SuppressWarnings("unchecked")
    private List<DayCount> daily(String sql, OffsetDateTime from) {
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("from", from)
                .getResultList();
        return rows.stream()
                .map(row -> new DayCount(
                        toDate(row[0]),
                        row[1] instanceof Number number ? number.longValue() : 0L))
                .toList();
    }

    private LocalDate toDate(Object value) {
        if (value instanceof Date date) return date.toLocalDate();
        if (value instanceof LocalDate date) return date;
        return LocalDate.parse(value.toString());
    }
}
