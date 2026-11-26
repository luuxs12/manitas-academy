package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.ConteoDia;
import com.mundomanitas.api.dto.admin.ConteoMonitor;
import com.mundomanitas.api.dto.admin.MonitorMuestra;
import com.mundomanitas.api.dto.admin.MonitorSnapshot;
import com.mundomanitas.api.repository.AdminStatsRepository;
import com.mundomanitas.api.service.CorreoService;
import com.mundomanitas.api.service.PagoService;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Convierte peticiones reales y datos de PostgreSQL en el estado que muestra
 * el panel de monitoreo. El buffer es acotado: no crece con el tiempo ni
 * guarda cabeceras, cuerpos ni datos personales.
 */
@Service
public class MonitorService {

    private static final int VENTANA_MINUTOS = 30;
    private static final int MAX_MUESTRAS = 5000;
    private static final int CACHE_SEGUNDOS = 10;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter ETIQUETA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");

    private final AdminStatsRepository stats;
    private final PagoService pagos;
    private final CorreoService correo;
    private final ConcurrentLinkedQueue<MonitorMuestra> muestras = new ConcurrentLinkedQueue<>();

    private volatile DatosBase cache;
    private volatile Instant cacheHasta = Instant.EPOCH;

    public MonitorService(AdminStatsRepository stats, PagoService pagos, CorreoService correo) {
        this.stats = stats;
        this.pagos = pagos;
        this.correo = correo;
    }

    /** Lo llama el filtro de peticiones al terminar cada respuesta. */
    public void registrar(MonitorMuestra muestra) {
        if (muestra == null) return;
        muestras.add(muestra);
        recortar(Instant.now());
    }

    public MonitorSnapshot snapshot() {
        Instant ahora = Instant.now();
        recortar(ahora);

        // Media ventana alineada al minuto: cada barra del gráfico es un minuto real.
        Instant desde = ahora.truncatedTo(ChronoUnit.MINUTES)
                .minus(VENTANA_MINUTOS - 1, ChronoUnit.MINUTES);
        List<MonitorMuestra> ventana = muestras.stream()
                .filter(muestra -> !muestra.tiempo().isBefore(desde))
                .toList();

        long peticiones = ventana.size();
        long exitosas = contar(ventana, 200, 300);
        long redirecciones = contar(ventana, 300, 400);
        long erroresCliente = contar(ventana, 400, 500);
        long erroresServidor = contar(ventana, 500, 600);

        List<Long> duraciones = ventana.stream().map(MonitorMuestra::duracionMs).toList();
        long p50 = percentil(duraciones, .50);
        long p95 = percentil(duraciones, .95);
        long p99 = percentil(duraciones, .99);

        long[] seriePeticiones = new long[VENTANA_MINUTOS];
        long[] serieExitosas = new long[VENTANA_MINUTOS];
        long[] serieRedirecciones = new long[VENTANA_MINUTOS];
        long[] serieErroresCliente = new long[VENTANA_MINUTOS];
        long[] serieErroresServidor = new long[VENTANA_MINUTOS];
        List<List<Long>> latenciasPorMinuto = new ArrayList<>();
        for (int index = 0; index < VENTANA_MINUTOS; index++) latenciasPorMinuto.add(new ArrayList<>());
        List<String> etiquetas = new ArrayList<>();
        for (int index = 0; index < VENTANA_MINUTOS; index++) {
            etiquetas.add(hora(desde.plus(index, ChronoUnit.MINUTES), ETIQUETA));
        }
        for (MonitorMuestra muestra : ventana) {
            int index = (int) ((muestra.tiempo().truncatedTo(ChronoUnit.MINUTES).toEpochMilli()
                    - desde.toEpochMilli()) / 60_000L);
            if (index < 0 || index >= VENTANA_MINUTOS) continue;
            seriePeticiones[index]++;
            if (muestra.estado() >= 200 && muestra.estado() < 300) serieExitosas[index]++;
            else if (muestra.estado() >= 300 && muestra.estado() < 400) serieRedirecciones[index]++;
            else if (muestra.estado() >= 400 && muestra.estado() < 500) serieErroresCliente[index]++;
            else if (muestra.estado() >= 500) serieErroresServidor[index]++;
            latenciasPorMinuto.get(index).add(muestra.duracionMs());
        }
        List<Long> serieLatencia = latenciasPorMinuto.stream().map(this::p95).toList();
        List<Long> serieLatenciaP50 = latenciasPorMinuto.stream().map(valores -> percentil(valores, .50)).toList();
        List<Long> serieLatenciaP99 = latenciasPorMinuto.stream().map(valores -> percentil(valores, .99)).toList();

        boolean baseOk = true;
        DatosBase base;
        try {
            base = base(ahora);
        } catch (Exception exception) {
            base = DatosBase.vacio();
            baseOk = false;
        }

        boolean dbOk = false;
        try {
            dbOk = stats.ping();
        } catch (Exception exception) {
            dbOk = false;
        }
        if (!baseOk) {
            // Si falló una consulta real, PostgreSQL no puede verse como sano.
            dbOk = false;
        }

        int disponibilidad = peticiones == 0 || erroresServidor == 0
                ? 100
                : (int) Math.round((peticiones - erroresServidor) * 100.0 / peticiones);
        int tasaErrores = porcentaje(erroresServidor, peticiones);
        double porMinuto = Math.round(peticiones * 10.0 / VENTANA_MINUTOS) / 10.0;
        long ultimoMinuto = ventana.stream()
                .filter(muestra -> !muestra.tiempo().isBefore(ahora.minus(1, ChronoUnit.MINUTES)))
                .count();

        List<MonitorSnapshot.Incidente> incidentes = incidentes(
                ahora, dbOk, baseOk, peticiones, tasaErrores, p95, base.conteos());
        String estado = incidentes.stream().anyMatch(item -> "critico".equals(item.nivel()))
                ? "caido"
                : incidentes.isEmpty() ? "operativo" : "degradado";

        return new MonitorSnapshot(
                hora(ahora, HORA),
                estado,
                peticiones,
                porMinuto,
                ultimoMinuto,
                tendencia(seriePeticiones),
                disponibilidad,
                tasaErrores,
                exitosas,
                redirecciones,
                erroresCliente,
                erroresServidor,
                p50,
                p95,
                p99,
                etiquetas,
                aLista(seriePeticiones),
                aLista(serieExitosas),
                aLista(serieRedirecciones),
                aLista(serieErroresCliente),
                aLista(serieErroresServidor),
                aLista(serieErroresServidor),
                serieLatencia,
                serieLatenciaP50,
                serieLatenciaP99,
                rutas(ventana),
                incidentes,
                servicios(ahora, dbOk),
                eventos(ventana),
                recientes(ventana),
                new MonitorSnapshot.Actividad(base.activos(),
                        base.conteos().altas(), base.conteos().matriculas(), base.conteos().lecciones(),
                        base.conteos().cuestionariosAprobados(), base.conteos().certificados(),
                        base.conteos().pagos(), base.conteos().pagosFallidos()),
                base.diaria());
    }

    private DatosBase base(Instant ahora) {
        if (cache != null && ahora.isBefore(cacheHasta)) return cache;
        synchronized (this) {
            if (cache != null && ahora.isBefore(cacheHasta)) return cache;

            OffsetDateTime ahoraDb = OffsetDateTime.ofInstant(ahora, ZoneId.systemDefault());
            OffsetDateTime desde24 = ahoraDb.minusHours(24);
            ConteoMonitor conteos = stats.conteosMonitor(desde24);
            long activos = stats.activoUsersSince(desde24);

            LocalDate hoy = LocalDate.now();
            LocalDate desdeSemana = hoy.minusDays(6);
            OffsetDateTime desdeDia = desdeSemana.atStartOfDay().atOffset(ahoraDb.getOffset());
            Map<LocalDate, Long> inscripciones = porDia(stats.inscripcionesPorDia(desdeDia));
            Map<LocalDate, Long> pagosPorDia = porDia(stats.pagosAprobadosPorDia(desdeDia));
            Map<LocalDate, Long> certificados = porDia(stats.certificadosPorDia(desdeDia));

            List<MonitorSnapshot.ActividadDia> diaria = new ArrayList<>();
            for (int index = 0; index < 7; index++) {
                LocalDate dia = desdeSemana.plusDays(index);
                diaria.add(new MonitorSnapshot.ActividadDia(
                        dia.format(DIA),
                        inscripciones.getOrDefault(dia, 0L),
                        pagosPorDia.getOrDefault(dia, 0L),
                        certificados.getOrDefault(dia, 0L)));
            }

            cache = new DatosBase(conteos, activos, diaria);
            cacheHasta = ahora.plusSeconds(CACHE_SEGUNDOS);
            return cache;
        }
    }

    private List<MonitorSnapshot.Incidente> incidentes(Instant ahora, boolean dbOk, boolean baseOk,
                                                        long peticiones, int tasaErrores, long p95,
                                                        ConteoMonitor conteos) {
        List<MonitorSnapshot.Incidente> resultado = new ArrayList<>();
        String cuando = hora(ahora, HORA);

        if (!dbOk || !baseOk) {
            resultado.add(new MonitorSnapshot.Incidente("critico", "Base de datos sin respuesta",
                    "PostgreSQL no respondió a la comprobación del monitor", cuando));
        }
        long erroresRecientes = muestras.stream()
                .filter(muestra -> muestra.estado() >= 500
                        && !muestra.tiempo().isBefore(ahora.minus(300, ChronoUnit.SECONDS)))
                .count();
        if (erroresRecientes >= 3 || (peticiones >= 4 && tasaErrores >= 20)) {
            resultado.add(new MonitorSnapshot.Incidente("critico", "Caída de respuestas",
                    erroresRecientes + " respuestas 5xx en los últimos 5 minutos", cuando));
        }
        if (peticiones >= 20 && p95 > 2000) {
            resultado.add(new MonitorSnapshot.Incidente("prevencion", "Latencia alta",
                    "La p95 superó los 2 segundos en la ventana analizada", cuando));
        }
        if (conteos.pagosFallidos() >= 3) {
            resultado.add(new MonitorSnapshot.Incidente("prevencion", "Fallos de pago",
                    conteos.pagosFallidos() + " cobros rechazados en 24 horas", cuando));
        }
        return resultado;
    }

    private List<MonitorSnapshot.Servicio> servicios(Instant ahora, boolean dbOk) {
        Runtime runtime = Runtime.getRuntime();
        long maxMb = runtime.maxMemory() / (1024 * 1024);
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        int usoMemoria = maxMb == 0 ? 0 : (int) Math.min(100, Math.round(usedMb * 100.0 / maxMb));
        long horas = ManagementFactory.getRuntimeMXBean().getUptime() / 3_600_000;

        return List.of(
                new MonitorSnapshot.Servicio("Base de datos",
                        dbOk ? "PostgreSQL responde" : "PostgreSQL no responde", dbOk),
                new MonitorSnapshot.Servicio("Checkout",
                        pagos.isEnabled() ? "Stripe configurado" : "Stripe sin configurar", true),
                new MonitorSnapshot.Servicio("Correo",
                        correo.isEnabled() ? "Resend configurado" : "Resend sin configurar", true),
                new MonitorSnapshot.Servicio("Memoria",
                        usoMemoria + "% de " + maxMb + " MB en uso", usoMemoria < 90),
                new MonitorSnapshot.Servicio("Servidor", "Activo desde hace " + horas + " h", true));
    }

    private List<MonitorSnapshot.Ruta> rutas(List<MonitorMuestra> ventana) {
        Map<Clave, List<MonitorMuestra>> agrupadas = ventana.stream()
                .collect(Collectors.groupingBy(muestra -> new Clave(muestra.metodo(), muestra.ruta())));
        return agrupadas.entrySet().stream()
                .map(entry -> {
                    List<MonitorMuestra> grupo = entry.getValue();
                    List<Long> duraciones = grupo.stream().map(MonitorMuestra::duracionMs).toList();
                    long errores = grupo.stream().filter(muestra -> muestra.estado() >= 500).count();
                    return new MonitorSnapshot.Ruta(entry.getKey().metodo(), entry.getKey().ruta(),
                            grupo.size(), errores, percentil(duraciones, .95));
                })
                .sorted(Comparator.comparingLong(MonitorSnapshot.Ruta::peticiones).reversed())
                .limit(6)
                .toList();
    }

    private List<MonitorSnapshot.Evento> eventos(List<MonitorMuestra> ventana) {
        return ventana.stream()
                .filter(muestra -> muestra.estado() >= 400)
                .sorted(Comparator.comparing(MonitorMuestra::tiempo).reversed())
                .limit(8)
                .map(this::evento)
                .toList();
    }

    /** Últimas respuestas, también las correctas, para el feed en vivo. */
    private List<MonitorSnapshot.Evento> recientes(List<MonitorMuestra> ventana) {
        return ventana.stream()
                .sorted(Comparator.comparing(MonitorMuestra::tiempo).reversed())
                .limit(8)
                .map(this::evento)
                .toList();
    }

    private MonitorSnapshot.Evento evento(MonitorMuestra muestra) {
        return new MonitorSnapshot.Evento(
                muestra.requestId() == null ? "" : muestra.requestId(),
                muestra.metodo(), muestra.ruta(), muestra.estado(),
                muestra.duracionMs(), hora(muestra.tiempo(), HORA));
    }

    /** Compara la última media ventana con la anterior; 0 cuando no hay base. */
    private int tendencia(long[] serie) {
        int mitad = serie.length / 2;
        long anterior = 0;
        long actual = 0;
        for (int index = 0; index < serie.length; index++) {
            if (index < mitad) anterior += serie[index];
            else actual += serie[index];
        }
        if (anterior == 0) return actual == 0 ? 0 : 100;
        return (int) Math.max(-100, Math.min(100,
                Math.round((actual - anterior) * 100.0 / anterior)));
    }

    private void recortar(Instant ahora) {
        Instant limite = ahora.minus(2, ChronoUnit.HOURS);
        muestras.removeIf(muestra -> muestra.tiempo().isBefore(limite));
        while (muestras.size() > MAX_MUESTRAS && muestras.poll() != null) {
            // Se descarta la muestra más antigua para no crecer sin límite.
        }
    }

    private long contar(List<MonitorMuestra> muestras, int desde, int hasta) {
        return muestras.stream()
                .filter(muestra -> muestra.estado() >= desde && muestra.estado() < hasta)
                .count();
    }

    private long p95(List<Long> valores) {
        return percentil(valores, .95);
    }

    private long percentil(List<Long> valores, double fraccion) {
        if (valores.isEmpty()) return 0L;
        List<Long> ordenados = valores.stream().sorted().toList();
        int indice = (int) Math.ceil(fraccion * ordenados.size()) - 1;
        return ordenados.get(Math.max(0, Math.min(indice, ordenados.size() - 1)));
    }

    private int porcentaje(long parte, long total) {
        return total == 0 ? 0 : (int) Math.min(100, Math.round(parte * 100.0 / total));
    }

    private Map<LocalDate, Long> porDia(List<ConteoDia> filas) {
        Map<LocalDate, Long> resultado = new HashMap<>();
        for (ConteoDia fila : filas) resultado.put(fila.dia(), fila.total());
        return resultado;
    }

    private List<Long> aLista(long[] valores) {
        return java.util.Arrays.stream(valores).boxed().toList();
    }

    private String hora(Instant instante, DateTimeFormatter formato) {
        return OffsetDateTime.ofInstant(instante, ZoneId.systemDefault()).format(formato);
    }

    private record Clave(String metodo, String ruta) {
    }

    private record DatosBase(ConteoMonitor conteos, long activos,
                             List<MonitorSnapshot.ActividadDia> diaria) {
        static DatosBase vacio() {
            return new DatosBase(
                    new ConteoMonitor(0, 0, 0, 0, 0, 0, 0),
                    0,
                    List.of());
        }
    }
}
