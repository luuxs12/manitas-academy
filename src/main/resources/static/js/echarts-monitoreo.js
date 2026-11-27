/* Comparativas rápidas del monitor con la copia local de ECharts.
   Los datos llegan en el evento que publica monitoreo.js tras consultar
   /admin/api/monitoreo; aquí no se inventa ni se guarda ningún valor. */

(() => {
    if (typeof echarts === "undefined") return;

    const SELECTORES = {
        recursos: "[data-echarts='recursos']",
        respuestas: "[data-echarts='respuestas']",
        actividad: "[data-echarts='actividad']"
    };
    const graficos = {};
    const movimiento = window.matchMedia("(prefers-reduced-motion: reduce)");
    let ultimos = null;

    const numero = (valor) => {
        const valorNumerico = Number(valor);
        return Number.isFinite(valorNumerico) ? valorNumerico : 0;
    };

    const serie = (valor) => (Array.isArray(valor) ? valor.map(numero) : []);

    const alinear = (valor, total) => Array.from(
            { length: total }, (_, indice) => numero(Array.isArray(valor) ? valor[indice] : 0));

    const colores = () => {
        const estilo = getComputedStyle(document.documentElement);
        const leer = (nombre) => estilo.getPropertyValue(nombre).trim();
        return {
            tinta: leer("--ink"),
            apagado: leer("--muted"),
            linea: leer("--line"),
            superficie: leer("--surface"),
            acento: leer("--accent"),
            ok: leer("--ok"),
            aviso: leer("--warn"),
            peligro: leer("--danger")
        };
    };

    const comun = (paleta, descripcion) => ({
        animation: !movimiento.matches,
        backgroundColor: "transparent",
        color: [paleta.acento, paleta.ok, paleta.aviso, paleta.peligro],
        textStyle: { color: paleta.tinta, fontFamily: "system-ui, sans-serif" },
        aria: { enabled: true, description: descripcion },
        grid: { left: 48, right: 16, top: 42, bottom: 32 },
        tooltip: {
            trigger: "axis",
            confine: true,
            backgroundColor: paleta.superficie,
            borderColor: paleta.linea,
            textStyle: { color: paleta.tinta }
        },
        legend: {
            top: 4,
            icon: "roundRect",
            itemWidth: 10,
            itemHeight: 10,
            textStyle: { color: paleta.apagado }
        },
        xAxis: {
            type: "category",
            boundaryGap: false,
            axisTick: { show: false },
            axisLine: { lineStyle: { color: paleta.linea } },
            axisLabel: { color: paleta.apagado, hideOverlap: true }
        },
        yAxis: {
            type: "value",
            minInterval: 1,
            axisLine: { show: false },
            axisTick: { show: false },
            splitLine: { lineStyle: { color: paleta.linea } },
            axisLabel: { color: paleta.apagado }
        }
    });

    const linea = (nombre, datos, color) => ({
        name: nombre,
        type: "line",
        smooth: 0.25,
        showSymbol: false,
        data: datos,
        lineStyle: { color: color, width: 2 },
        itemStyle: { color: color },
        areaStyle: { color: color, opacity: 0.1 }
    });

    const barra = (nombre, datos, color) => ({
        name: nombre,
        type: "bar",
        data: datos,
        barMaxWidth: 18,
        itemStyle: { color: color, borderRadius: [4, 4, 0, 0] }
    });

    const opcionRecursos = (datos, paleta) => {
        const cpu = serie(datos.serieCpu);
        const memoria = serie(datos.serieMemoria);
        const total = Math.max(cpu.length, memoria.length);
        const etiquetas = Array.from({ length: total }, (_, indice) => indice === total - 1
                ? "ahora" : "hace " + ((total - 1 - indice) * 2) + " s");
        const opcion = comun(paleta, "Uso de CPU y memoria durante el último minuto");

        opcion.xAxis.data = etiquetas;
        opcion.yAxis.min = 0;
        opcion.yAxis.max = 100;
        opcion.yAxis.axisLabel.formatter = "{value}%";
        opcion.tooltip.valueFormatter = (valor) => numero(valor).toFixed(1) + "%";
        opcion.series = [
            linea("CPU", cpu.map((valor) => numero(valor.toFixed(1))), paleta.acento),
            linea("Memoria", memoria.map((valor) => numero(valor.toFixed(1))), paleta.ok)
        ];
        return opcion;
    };

    const opcionRespuestas = (datos, paleta) => {
        const etiquetas = Array.isArray(datos.etiquetas) ? datos.etiquetas : [];
        const opcion = comun(paleta, "Peticiones por minuto según el tipo de respuesta");
        opcion.xAxis.data = etiquetas;

        opcion.series = [
            ["2xx correctas", "serieExitosas", paleta.ok],
            ["3xx", "serieRedirecciones", paleta.acento],
            ["4xx", "serieErroresCliente", paleta.aviso],
            ["5xx", "serieErroresServidor", paleta.peligro]
        ].map(([nombre, campo, color]) => {
            const trazo = linea(nombre, alinear(datos[campo], etiquetas.length), color);
            trazo.stack = "respuestas";
            trazo.areaStyle.opacity = 0.14;
            return trazo;
        });
        return opcion;
    };

    const opcionActividad = (datos, paleta) => {
        const dias = Array.isArray(datos.actividadDiaria) ? datos.actividadDiaria : [];
        const opcion = comun(paleta, "Matrículas, pagos y certificados de los últimos siete días");
        opcion.xAxis.boundaryGap = true;
        opcion.xAxis.data = dias.map((dia) => dia.etiqueta || "");
        opcion.series = [
            barra("Matrículas", dias.map((dia) => numero(dia.inscripciones)), paleta.acento),
            barra("Pagos", dias.map((dia) => numero(dia.pagos)), paleta.aviso),
            barra("Certificados", dias.map((dia) => numero(dia.certificados)), paleta.ok)
        ];
        return opcion;
    };

    const pintar = () => {
        if (!ultimos) return;
        const paleta = colores();
        if (!paleta.tinta || !paleta.superficie) return;

        const opciones = {
            recursos: opcionRecursos(ultimos, paleta),
            respuestas: opcionRespuestas(ultimos, paleta),
            actividad: opcionActividad(ultimos, paleta)
        };

        for (const [clave, selector] of Object.entries(SELECTORES)) {
            const nodo = document.querySelector(selector);
            if (!nodo) continue;
            if (!graficos[clave]) graficos[clave] = echarts.init(nodo, null, { renderer: "canvas" });
            graficos[clave].setOption(opciones[clave], { notMerge: true });
        }
    };

    document.addEventListener("monitoreo:actualizado", (evento) => {
        ultimos = evento.detail;
        pintar();
    });

    document.addEventListener("click", (evento) => {
        if (evento.target.closest("[data-theme-toggle]")) requestAnimationFrame(pintar);
    });

    window.addEventListener("resize", () => {
        Object.values(graficos).forEach((grafico) => grafico.resize());
    });

    if (typeof movimiento.addEventListener === "function") {
        movimiento.addEventListener("change", pintar);
    }
})();
