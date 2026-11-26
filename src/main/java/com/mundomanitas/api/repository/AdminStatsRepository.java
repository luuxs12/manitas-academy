package com.mundomanitas.api.repository;

import com.mundomanitas.api.dto.admin.ConteoDia;
import com.mundomanitas.api.dto.admin.ConteoMonitor;
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
    private static final String ENROLLMENTS =
            "select cast(date_trunc('day', enrolled_at) as date) as day, count(*) as total "
                    + "from enrollments where enrolled_at >= :from group by 1 order by 1";
    private static final String PAID_PAYMENTS =
            "select cast(date_trunc('day', paid_at) as date) as day, count(*) as total "
                    + "from payments where status = 'PAID' and paid_at >= :from group by 1 order by 1";
    private static final String CERTIFICATES =
            "select cast(date_trunc('day', issued_at) as date) as day, count(*) as total "
                    + "from certificates where issued_at >= :from group by 1 order by 1";

    private final EntityManager entityManager;

    public AdminStatsRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<ConteoDia> usersPorDia(OffsetDateTime from) {
        return daily(USERS, from);
    }

    public List<ConteoDia> coursesPorDia(OffsetDateTime from) {
        return daily(COURSES, from);
    }

    public List<ConteoDia> revenuePorDia(OffsetDateTime from) {
        String sql = """
                select cast(date_trunc('day', paid_at) as date) as day,
                       sum(amount_cents) as total
                  from payments
                 where status = 'PAID' and paid_at >= :from
                 group by 1 order by 1
                """;
        return daily(sql, from);
    }

    /** Comprobación mínima para el monitor; falla si PostgreSQL no responde. */
    public boolean ping() {
        Object value = entityManager.createNativeQuery("select 1").getSingleResult();
        return value instanceof Number number && number.intValue() == 1;
    }

    /** Conteos recientes del negocio para el panel de monitoreo. */
    @SuppressWarnings("unchecked")
    public ConteoMonitor conteosMonitor(OffsetDateTime from) {
        String sql = """
                select
                  (select count(*) from users where created_at >= :from),
                  (select count(*) from enrollments where enrolled_at >= :from),
                  (select count(*) from lesson_progress where completed_at >= :from),
                  (select count(*) from quiz_attempts where submitted_at >= :from and score = 20),
                  (select count(*) from certificates where issued_at >= :from and revoked_at is null),
                  (select count(*) from payments where status = 'PAID' and paid_at >= :from),
                  (select count(*) from payments where status = 'FAILED' and created_at >= :from)
                """;
        Object[] row = (Object[]) entityManager.createNativeQuery(sql)
                .setParameter("from", from)
                .getSingleResult();
        return new ConteoMonitor(
                number(row[0]), number(row[1]), number(row[2]), number(row[3]),
                number(row[4]), number(row[5]), number(row[6]));
    }

    public List<ConteoDia> inscripcionesPorDia(OffsetDateTime from) {
        return daily(ENROLLMENTS, from);
    }

    public List<ConteoDia> pagosAprobadosPorDia(OffsetDateTime from) {
        return daily(PAID_PAYMENTS, from);
    }

    public List<ConteoDia> certificadosPorDia(OffsetDateTime from) {
        return daily(CERTIFICATES, from);
    }

    /** Personas con movimiento real en el período (matrícula o avance). */
    public long activoUsersSince(OffsetDateTime from) {
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
    private List<ConteoDia> daily(String sql, OffsetDateTime from) {
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("from", from)
                .getResultList();
        return rows.stream()
                .map(row -> new ConteoDia(
                        toDate(row[0]),
                        row[1] instanceof Number number ? number.longValue() : 0L))
                .toList();
    }

    private LocalDate toDate(Object value) {
        if (value instanceof Date date) return date.toLocalDate();
        if (value instanceof LocalDate date) return date;
        return LocalDate.parse(value.toString());
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
