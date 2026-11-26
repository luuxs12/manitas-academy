package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.ConteoDia;
import com.mundomanitas.api.dto.admin.ConteoMonitor;
import com.mundomanitas.api.dto.admin.MonitorMuestra;
import com.mundomanitas.api.dto.admin.MonitorSnapshot;
import com.mundomanitas.api.repository.AdminStatsRepository;
import com.mundomanitas.api.service.CorreoService;
import com.mundomanitas.api.service.PagoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Monitoreo en tiempo real")
class MonitorServiceTest {

    private AdminStatsRepository stats;
    private PagoService pagos;
    private CorreoService correo;
    private MonitorService service;

    @BeforeEach
    void setUp() {
        stats = mock(AdminStatsRepository.class);
        pagos = mock(PagoService.class);
        correo = mock(CorreoService.class);
        service = new MonitorService(stats, pagos, correo);

        when(stats.ping()).thenReturn(true);
        when(stats.conteosMonitor(any())).thenReturn(new ConteoMonitor(3, 5, 7, 2, 1, 4, 1));
        when(stats.activoUsersSince(any())).thenReturn(6L);
        when(stats.inscripcionesPorDia(any()))
                .thenReturn(List.of(new ConteoDia(LocalDate.now(), 5L)));
        when(stats.pagosAprobadosPorDia(any()))
                .thenReturn(List.of(new ConteoDia(LocalDate.now(), 4L)));
        when(stats.certificadosPorDia(any()))
                .thenReturn(List.of(new ConteoDia(LocalDate.now(), 1L)));
        when(pagos.isEnabled()).thenReturn(true);
        when(correo.isEnabled()).thenReturn(false);
    }

    @Test
    @DisplayName("resume peticiones reales, errores, latencia y actividad de PostgreSQL")
    void resumeDatosReales() {
        Instant ahora = Instant.now();
        service.registrar(muestra(ahora.minus(4, ChronoUnit.MINUTES),
                "GET", "/panel/cursos/{}", 200, 40, "uno"));
        service.registrar(muestra(ahora.minus(3, ChronoUnit.MINUTES),
                "POST", "/panel/cursos/{}", 200, 60, "dos"));
        service.registrar(muestra(ahora.minus(2, ChronoUnit.MINUTES),
                "GET", "/admin/reportes", 500, 1200, "tres"));
        service.registrar(muestra(ahora.minus(1, ChronoUnit.MINUTES),
                "GET", "/admin/reportes", 503, 900, "cuatro"));

        MonitorSnapshot snapshot = service.snapshot();

        assertThat(snapshot.peticiones()).isEqualTo(4);
        assertThat(snapshot.exitosas()).isEqualTo(2);
        assertThat(snapshot.erroresServidor()).isEqualTo(2);
        assertThat(snapshot.disponibilidad()).isEqualTo(50);
        assertThat(snapshot.tasaErrores()).isEqualTo(50);
        assertThat(snapshot.latenciaP95()).isEqualTo(1200);
        assertThat(snapshot.peticionesPorMinuto()).isGreaterThan(0);
        assertThat(snapshot.estado()).isEqualTo("caido");
        assertThat(snapshot.etiquetas()).hasSize(30);
        assertThat(snapshot.seriePeticiones()).hasSize(30);
        assertThat(snapshot.serieExitosas().stream().mapToLong(Long::longValue).sum()).isEqualTo(2);
        assertThat(snapshot.serieErroresCliente()).containsOnly(0L);
        assertThat(snapshot.serieErrores().stream().mapToLong(Long::longValue).sum()).isEqualTo(2);
        assertThat(snapshot.serieLatencia()).hasSize(30);
        assertThat(snapshot.serieLatenciaP50()).hasSize(30);
        assertThat(snapshot.serieLatenciaP99()).hasSize(30);
        assertThat(snapshot.recientes()).hasSize(4);
        assertThat(snapshot.recientes()).allSatisfy(evento ->
                assertThat(evento.requestId()).isNotBlank());
        assertThat(snapshot.incidentes())
                .anySatisfy(incidente -> assertThat(incidente.titulo()).isEqualTo("Caída de respuestas"));
        assertThat(snapshot.eventos()).hasSize(2);
        assertThat(snapshot.eventos()).allSatisfy(evento -> assertThat(evento.requestId()).isNotBlank());
        assertThat(snapshot.rutas()).anySatisfy(ruta -> {
            assertThat(ruta.ruta()).contains("{}");
            assertThat(ruta.peticiones()).isEqualTo(1);
        });
        assertThat(snapshot.rutas()).anySatisfy(ruta -> {
            assertThat(ruta.metodo()).isEqualTo("GET");
            assertThat(ruta.ruta()).isEqualTo("/admin/reportes");
            assertThat(ruta.peticiones()).isEqualTo(2);
            assertThat(ruta.errores()).isEqualTo(2);
        });
        assertThat(snapshot.actividad().activos()).isEqualTo(6);
        assertThat(snapshot.actividad().matriculas()).isEqualTo(5);
        assertThat(snapshot.actividad().cuestionariosAprobados()).isEqualTo(2);
        assertThat(snapshot.actividadDiaria()).hasSize(7);
        assertThat(snapshot.servicios()).anySatisfy(servicio ->
                assertThat(servicio.nombre()).isEqualTo("Base de datos"));
        verify(stats).ping();
    }

    @Test
    @DisplayName("sin peticiones ni incidentes no muestra cifras inventadas")
    void sinPeticiones() {
        MonitorSnapshot snapshot = service.snapshot();

        assertThat(snapshot.peticiones()).isZero();
        assertThat(snapshot.erroresServidor()).isZero();
        assertThat(snapshot.disponibilidad()).isEqualTo(100);
        assertThat(snapshot.estado()).isEqualTo("operativo");
        assertThat(snapshot.incidentes()).isEmpty();
        assertThat(snapshot.eventos()).isEmpty();
        assertThat(snapshot.rutas()).isEmpty();
        assertThat(snapshot.seriePeticiones()).containsOnly(0L);
    }

    @Test
    @DisplayName("una falla de PostgreSQL se marca como caída crítica")
    void baseDeDatosCaida() {
        when(stats.conteosMonitor(any())).thenThrow(new IllegalStateException("sin BD"));

        MonitorSnapshot snapshot = service.snapshot();

        assertThat(snapshot.estado()).isEqualTo("caido");
        assertThat(snapshot.servicios()).anySatisfy(servicio -> {
            assertThat(servicio.nombre()).isEqualTo("Base de datos");
            assertThat(servicio.ok()).isFalse();
        });
        assertThat(snapshot.incidentes())
                .anySatisfy(incidente -> assertThat(incidente.titulo()).isEqualTo("Base de datos sin respuesta"));
        assertThat(snapshot.actividad().altas()).isZero();
    }

    private MonitorMuestra muestra(Instant tiempo, String metodo, String ruta,
                                   int estado, long duracionMs, String requestId) {
        return new MonitorMuestra(tiempo, metodo, ruta, estado, duracionMs, requestId);
    }
}
