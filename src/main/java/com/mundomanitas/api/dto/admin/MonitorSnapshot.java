package com.mundomanitas.api.dto.admin;

import java.util.List;

/**
 * Estado del momento. Las series vienen del buffer de peticiones y los
 * conteos de negocio de PostgreSQL; nada se inventa ni se deja fijo.
 */
public record MonitorSnapshot(
        String servidor,
        String estado,
        long peticiones,
        double peticionesPorMinuto,
        long peticionesUltimoMinuto,
        int tendenciaPeticiones,
        int disponibilidad,
        int tasaErrores,
        long exitosas,
        long redirecciones,
        long erroresCliente,
        long erroresServidor,
        long latenciaP50,
        long latenciaP95,
        long latenciaP99,
        List<String> etiquetas,
        List<Long> seriePeticiones,
        List<Long> serieExitosas,
        List<Long> serieRedirecciones,
        List<Long> serieErroresCliente,
        List<Long> serieErroresServidor,
        List<Long> serieErrores,
        List<Long> serieLatencia,
        List<Long> serieLatenciaP50,
        List<Long> serieLatenciaP99,
        List<Ruta> rutas,
        List<Incidente> incidentes,
        List<Servicio> servicios,
        List<Evento> eventos,
        List<Evento> recientes,
        double cpu,
        double memoria,
        List<Double> serieCpu,
        List<Double> serieMemoria,
        Actividad actividad,
        List<ActividadDia> actividadDiaria
) {

    public record Ruta(String metodo, String ruta, long peticiones, long errores, long p95) {
    }

    public record Incidente(String nivel, String titulo, String detalle, String cuando) {
    }

    public record Servicio(String nombre, String detalle, boolean ok) {
    }

    public record Evento(String requestId, String metodo, String ruta,
                         int estado, long duracionMs, String cuando) {
    }

    public record Actividad(long activos, long altas, long matriculas, long lecciones,
                            long cuestionariosAprobados, long certificados,
                            long pagos, long pagosFallidos) {
    }

    public record ActividadDia(String etiqueta, long inscripciones, long pagos, long certificados) {
    }
}
