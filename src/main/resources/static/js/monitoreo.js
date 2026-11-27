/* Consola de monitoreo: solo pinta lo que devuelve /admin/api/monitoreo.
   Cada gráfico usa su propia unidad y se actualiza con datos reales. */

(() => {
    const root = document.querySelector("[data-monitoreo]");
    if (!root) return;

    const SVG = "http://www.w3.org/2000/svg";
    const CONFIG = { izquierda: 48, derecha: 930, arriba: 30, abajo: 210 };
    const CONFIG_ACTIVIDAD = { izquierda: 40, derecha: 860, arriba: 40, abajo: 210 };
    const CONFIG_RECURSO = { izquierda: 8, derecha: 292, arriba: 12, abajo: 112 };
    const MUESTRAS_RECURSO = 60;
    const SELECTORES_RECURSO = {
        cpu: {
            area: "[data-area-cpu]", linea: "[data-linea-cpu]",
            grafico: "[data-grafico-cpu]", crosshair: "[data-crosshair-cpu]",
            campo: "serieCpu", nombre: "CPU del proceso"
        },
        memoria: {
            area: "[data-area-memoria]", linea: "[data-linea-memoria]",
            grafico: "[data-grafico-memoria]", crosshair: "[data-crosshair-memoria]",
            campo: "serieMemoria", nombre: "Memoria JVM"
        }
    };
    const uno = (selector) => root.querySelector(selector);
    const f = (valor) => numero(valor).toFixed(1);
    let busy = false;
    let ultimaCarga = 0;
    let instantanea = null;

    const numero = (valor) => {
        const parsed = Number(valor);
        return Number.isFinite(parsed) ? parsed : 0;
    };

    const serie = (valor) => (Array.isArray(valor) ? valor.map(numero) : []);

    const texto = (selector, valor) => {
        const elemento = uno(selector);
        if (elemento) elemento.textContent = String(valor);
    };

    const svg = (etiqueta, atributos) => {
        const elemento = document.createElementNS(SVG, etiqueta);
        for (const [clave, valor] of Object.entries(atributos)) {
            elemento.setAttribute(clave, String(valor));
        }
        return elemento;
    };

    const titulo = (contenido) => {
        const elemento = svg("title", {});
        elemento.textContent = String(contenido);
        return elemento;
    };

    const celda = (contenido, clase) => {
        const td = document.createElement("td");
        if (clase) td.className = clase;
        td.textContent = String(contenido);
        return td;
    };

    const claseEstado = (estado) => {
        const codigo = numero(estado);
        if (codigo >= 500) return "bad";
        if (codigo >= 400) return "warn";
        if (codigo >= 300) return "redirect";
        return "ok";
    };

    const estado = (valor) => {
        const badge = uno("[data-monitoreo-estado]");
        if (!badge) return;
        const normal = String(valor || "operativo").toLowerCase();
        const sinConexion = normal.includes("conexión") || normal.includes("conexion");
        const nivel = normal === "caido" || sinConexion ? "bad"
                : normal === "degradado" ? "warn" : "ok";
        badge.textContent = normal;
        badge.className = "monitor-live-badge is-" + nivel;
    };

    const alinear = (valores, total) => Array.from(
            { length: total }, (_, indice) => numero(Array.isArray(valores) ? valores[indice] : 0));

    const xEn = (indice, total, config) => config.izquierda
            + indice * (config.derecha - config.izquierda) / Math.max(1, total - 1);

    const coordsDe = (valores, tope, config) => valores.map((valor, indice) => ({
        x: xEn(indice, valores.length, config),
        y: config.abajo - valor * (config.abajo - config.arriba) / tope
    }));

    const curva = (puntos, inicial = "M") => {
        if (!puntos.length) return "";
        if (puntos.length === 1) return inicial + " " + f(puntos[0].x) + " " + f(puntos[0].y);
        let trazo = inicial + " " + f(puntos[0].x) + " " + f(puntos[0].y);
        for (let indice = 0; indice < puntos.length - 1; indice++) {
            const anterior = puntos[indice - 1] || puntos[indice];
            const actual = puntos[indice];
            const siguiente = puntos[indice + 1];
            const posterior = puntos[indice + 2] || siguiente;
            trazo += " C "
                    + f(actual.x + (siguiente.x - anterior.x) / 6) + " "
                    + f(actual.y + (siguiente.y - anterior.y) / 6) + " "
                    + f(siguiente.x - (posterior.x - actual.x) / 6) + " "
                    + f(siguiente.y - (posterior.y - actual.y) / 6) + " "
                    + f(siguiente.x) + " " + f(siguiente.y);
        }
        return trazo;
    };

    const areaDe = (puntos, config) => {
        if (!puntos.length) return "";
        const primero = puntos[0];
        const ultimo = puntos[puntos.length - 1];
        return curva(puntos) + " L " + f(ultimo.x) + " " + config.abajo
                + " L " + f(primero.x) + " " + config.abajo + " Z";
    };

    const bandaDe = (superior, inferior) => superior.length
            ? curva(superior) + " " + curva([...inferior].reverse(), "L") + " Z"
            : "";

    const ejeX = (grupo, etiquetas, config = CONFIG) => {
        if (!grupo) return;
        grupo.replaceChildren();
        etiquetas.forEach((etiqueta, indice) => {
            if (indice % 5 !== 0 && indice !== etiquetas.length - 1) return;
            const textoEje = svg("text", {
                x: f(xEn(indice, etiquetas.length, config)),
                y: config === CONFIG_ACTIVIDAD ? 235 : 238
            });
            textoEje.textContent = etiqueta;
            grupo.append(textoEje);
        });
    };

    const spark = (clave, valores) => {
        const linea = uno('[data-spark="' + clave + '"] [data-spark-line]');
        if (!linea) return;
        const datos = serie(valores);
        if (!datos.length) {
            linea.setAttribute("points", "");
            return;
        }
        const tope = Math.max(1, ...datos);
        linea.setAttribute("points", datos.map((valor, indice) =>
                (indice * 160 / Math.max(1, datos.length - 1)).toFixed(1) + ","
                + (40 - valor * 34 / tope).toFixed(1)).join(" "));
    };

    const trafico = (datos) => {
        const etiquetas = Array.isArray(datos.etiquetas) ? datos.etiquetas : [];
        const total = etiquetas.length;
        const exito = alinear(datos.serieExitosas, total);
        const redirect = alinear(datos.serieRedirecciones, total);
        const cliente = alinear(datos.serieErroresCliente, total);
        const servidor = alinear(datos.serieErroresServidor, total);
        const tope = Math.max(1, ...exito, ...redirect, ...cliente, ...servidor);

        let inferior = new Array(total).fill(0);
        for (const capa of [
            ["ok", exito], ["redirect", redirect], ["warn", cliente], ["bad", servidor]
        ]) {
            const superior = inferior.map((base, indice) => base + capa[1][indice]);
            const path = uno('[data-capa-trafico="' + capa[0] + '"]');
            if (path) {
                path.setAttribute("d", bandaDe(
                        coordsDe(superior, tope, CONFIG), coordsDe(inferior, tope, CONFIG)));
            }
            inferior = superior;
        }
        const totales = inferior;
        const puntosCurva = coordsDe(totales, tope, CONFIG);
        const linea = uno("[data-linea-trafico]");
        if (linea) linea.setAttribute("d", curva(puntosCurva));

        const puntos = uno("[data-puntos-trafico]");
        if (puntos) {
            puntos.replaceChildren();
            puntosCurva.forEach((punto, indice) => {
                if (totales[indice] <= 0) return;
                const circulo = svg("circle", {
                    cx: f(punto.x), cy: f(punto.y), r: 3, "class": "monitor-punto"
                });
                circulo.append(titulo((etiquetas[indice] || "") + " · " + totales[indice] + " peticiones"));
                puntos.append(circulo);
            });
        }

        texto("[data-eje-trafico-max]", tope + " req/min");
        ejeX(uno("[data-eje-trafico]"), etiquetas);
        const grafico = uno("[data-grafico-trafico]");
        if (grafico) {
            grafico.setAttribute("aria-label", "Flujo de peticiones por minuto en los últimos 30 minutos. Máximo "
                    + tope + " por minuto.");
        }
    };

    const latencia = (datos) => {
        const etiquetas = Array.isArray(datos.etiquetas) ? datos.etiquetas : [];
        const total = etiquetas.length;
        const p50 = alinear(datos.serieLatenciaP50, total);
        const p95 = alinear(datos.serieLatencia, total);
        const p99 = alinear(datos.serieLatenciaP99, total);
        const tope = Math.max(1, ...p50, ...p95, ...p99);
        const curvas = {
            p50: coordsDe(p50, tope, CONFIG),
            p95: coordsDe(p95, tope, CONFIG),
            p99: coordsDe(p99, tope, CONFIG)
        };

        for (const [selector, nombre] of [
            ["[data-banda-p50-p95]", "p50-p95"], ["[data-banda-p95-p99]", "p95-p99"]
        ]) {
            const banda = uno(selector);
            if (banda) {
                const [inferior, superior] = nombre.split("-");
                banda.setAttribute("d", bandaDe(curvas[superior], curvas[inferior]));
            }
        }
        for (const [selector, nombre] of [
            ["[data-linea-latencia-p50]", "p50"],
            ["[data-linea-latencia]", "p95"],
            ["[data-linea-latencia-p99]", "p99"]
        ]) {
            const linea = uno(selector);
            if (linea) linea.setAttribute("d", curva(curvas[nombre]));
        }

        const puntos = uno("[data-puntos-latencia]");
        if (puntos) {
            puntos.replaceChildren();
            curvas.p95.forEach((punto, indice) => {
                if (p95[indice] <= 0) return;
                const circulo = svg("circle", {
                    cx: f(punto.x), cy: f(punto.y), r: 3.1,
                    "class": "monitor-punto monitor-punto--latencia"
                });
                circulo.append(titulo((etiquetas[indice] || "") + " · p95 " + p95[indice] + " ms"));
                puntos.append(circulo);
            });
        }

        texto("[data-eje-latencia-max]", tope + " ms");
        ejeX(uno("[data-eje-latencia]"), etiquetas);
        const grafico = uno("[data-grafico-latencia]");
        if (grafico) {
            grafico.setAttribute("aria-label", "Dispersión de latencia por minuto en los últimos 30 minutos. Máximo "
                    + tope + " milisegundos.");
        }
    };

    const franjas = (datos) => {
        const contenedor = uno("[data-monitoreo-franjas]");
        if (!contenedor) return;
        const etiquetas = Array.isArray(datos.etiquetas) ? datos.etiquetas : [];
        const peticiones = serie(datos.seriePeticiones);
        const errores = serie(datos.serieErroresServidor);
        const latencias = serie(datos.serieLatencia);
        const tope = Math.max(1, ...peticiones);

        contenedor.replaceChildren();
        etiquetas.forEach((etiqueta, indice) => {
            const cantidad = numero(peticiones[indice]);
            const fallos = numero(errores[indice]);
            const latencia = numero(latencias[indice]);
            const nivel = fallos > 0 ? "bad" : latencia > 2000 ? "warn" : cantidad > 0 ? "ok" : "idle";
            const intensidad = Math.min(5, Math.max(1, Math.ceil(cantidad * 5 / tope)));
            const franja = document.createElement("span");
            franja.className = "admin-franja is-" + nivel
                    + (nivel === "idle" ? "" : " is-n" + intensidad);
            franja.textContent = cantidad > 0 ? String(cantidad) : "·";
            franja.title = etiqueta + ": " + cantidad + " peticiones, " + fallos
                    + " errores 5xx, p95 " + latencia + " ms";
            contenedor.append(franja);
        });
        contenedor.setAttribute("aria-label", "Intensidad por minuto durante los últimos "
                + etiquetas.length + " minutos. Máximo " + tope + " peticiones por minuto.");
    };

    const incidentes = (listaDatos) => {
        const lista = uno("[data-monitoreo-incidentes]");
        const vacio = uno("[data-monitoreo-sin-incidentes]");
        if (!lista) return;
        lista.replaceChildren();
        for (const dato of Array.isArray(listaDatos) ? listaDatos : []) {
            const item = document.createElement("li");
            item.className = "monitor-incident is-" + (dato.nivel || "prevencion");
            const tituloItem = document.createElement("strong");
            tituloItem.textContent = dato.titulo || "Aviso";
            const detalle = document.createElement("span");
            detalle.textContent = dato.detalle || "";
            const cuando = document.createElement("time");
            cuando.textContent = dato.cuando || "";
            item.append(tituloItem, detalle, cuando);
            lista.append(item);
        }
        if (vacio) vacio.hidden = lista.childElementCount > 0;
    };

    const filaVacia = (tbody, columnas, mensaje) => {
        const fila = document.createElement("tr");
        const celdaVacia = celda(mensaje, "monitor-empty");
        celdaVacia.colSpan = columnas;
        fila.append(celdaVacia);
        tbody.append(fila);
    };

    const celdaEstado = (valor) => {
        const td = document.createElement("td");
        const badge = document.createElement("span");
        badge.className = "monitor-status is-" + claseEstado(valor);
        badge.textContent = String(valor ?? "—");
        td.append(badge);
        return td;
    };

    const eventosEn = (selector, listaDatos, mensaje, animar) => {
        const tbody = uno(selector);
        if (!tbody) return;
        tbody.replaceChildren();
        const datos = Array.isArray(listaDatos) ? listaDatos : [];
        if (!datos.length) {
            filaVacia(tbody, 5, mensaje);
            return;
        }
        datos.forEach((dato, indice) => {
            const fila = document.createElement("tr");
            if (animar && indice === 0) fila.className = "monitor-feed-row--nuevo";
            fila.append(
                    celda(dato.cuando || "—"),
                    celda((dato.metodo || "") + " " + (dato.ruta || "")),
                    celdaEstado(dato.estado),
                    celda(numero(dato.duracionMs) + " ms"),
                    celda(dato.requestId || "—", "monitor-request")
            );
            tbody.append(fila);
        });
    };

    const servicios = (listaDatos) => {
        const lista = uno("[data-monitoreo-servicios]");
        if (!lista) return;
        lista.replaceChildren();
        for (const dato of Array.isArray(listaDatos) ? listaDatos : []) {
            const item = document.createElement("li");
            item.className = "monitor-service " + (dato.ok ? "is-ok" : "is-bad");
            const dot = document.createElement("span");
            dot.className = "monitor-service-dot";
            dot.setAttribute("aria-hidden", "true");
            const nombre = document.createElement("strong");
            nombre.textContent = dato.nombre || "";
            const detalle = document.createElement("span");
            detalle.textContent = dato.detalle || "";
            item.append(dot, nombre, detalle);
            lista.append(item);
        }
    };

    const actividad = (datos) => {
        const valores = [
            ["altas", numero(datos && datos.altas)],
            ["matriculas", numero(datos && datos.matriculas)],
            ["lecciones", numero(datos && datos.lecciones)],
            ["cuestionarios", numero(datos && datos.cuestionariosAprobados)],
            ["certificados", numero(datos && datos.certificados)],
            ["pagos", numero(datos && datos.pagos)],
            ["pagos-fallidos", numero(datos && datos.pagosFallidos)]
        ];
        const tope = Math.max(1, ...valores.map((entrada) => entrada[1]));
        for (const [clave, valor] of valores) {
            const fila = root.querySelector('[data-funnel="' + clave + '"]');
            if (!fila) continue;
            const relleno = fila.querySelector(".monitor-funnel-track > span");
            if (relleno) relleno.style.width = (valor * 100 / tope).toFixed(1) + "%";
            const cantidad = fila.querySelector("strong");
            if (cantidad) cantidad.textContent = String(valor);
        }
    };

    const actividadDiaria = (dias) => {
        const eje = uno("[data-eje-actividad]");
        const maximo = uno("[data-eje-actividad-max]");
        if (!Array.isArray(dias)) return;

        const series = [
            ["inscripciones", "Matrículas"],
            ["pagos", "Pagos"],
            ["certificados", "Certificados"]
        ];
        const tope = Math.max(1, ...dias.flatMap((dia) =>
                series.map((serieDia) => numero(dia[serieDia[0]]))));
        if (maximo) maximo.textContent = String(tope);

        for (const [clave, nombre] of series) {
            const valores = alinear(dias.map((dia) => dia[clave]), dias.length);
            const puntos = coordsDe(valores, tope, CONFIG_ACTIVIDAD);
            const area = uno('[data-area-actividad="' + clave + '"]');
            const linea = uno('[data-curva-actividad="' + clave + '"]');
            if (area) area.setAttribute("d", areaDe(puntos, CONFIG_ACTIVIDAD));
            if (linea) linea.setAttribute("d", curva(puntos));
        }

        const puntos = uno("[data-puntos-actividad]");
        if (puntos) {
            puntos.replaceChildren();
            dias.forEach((dia, indice) => {
                series.forEach(([clave, nombre]) => {
                    const valor = numero(dia[clave]);
                    const punto = coordsDe([valor], tope, CONFIG_ACTIVIDAD)[0];
                    const circulo = svg("circle", {
                        cx: f(xEn(indice, dias.length, CONFIG_ACTIVIDAD)),
                        cy: f(punto.y),
                        r: 3.2,
                        "class": "monitor-punto monitor-punto--actividad is-" + clave
                    });
                    circulo.append(titulo((dia.etiqueta || "") + " · " + nombre + ": " + valor));
                    puntos.append(circulo);
                });
            });
        }

        if (eje) {
            eje.replaceChildren();
            dias.forEach((dia, indice) => {
                const etiqueta = svg("text", {
                    x: f(xEn(indice, dias.length, CONFIG_ACTIVIDAD)), y: 235
                });
                etiqueta.textContent = dia.etiqueta || "";
                eje.append(etiqueta);
            });
        }
        const grafico = uno("[data-grafico-actividad]");
        if (grafico) {
            grafico.setAttribute("aria-label", "Tendencia de matrículas, pagos y certificados durante los últimos "
                    + dias.length + " días.");
        }
    };

    const rutas = (listaDatos) => {
        const lista = uno("[data-monitoreo-rutas]");
        const vacio = uno("[data-monitoreo-sin-rutas]");
        if (!lista) return;
        lista.replaceChildren();
        const datos = Array.isArray(listaDatos) ? listaDatos : [];
        if (vacio) vacio.hidden = datos.length > 0;
        if (!datos.length) return;

        const tope = Math.max(1, ...datos.map((dato) => numero(dato.peticiones)));
        for (const dato of datos) {
            const item = document.createElement("li");
            const top = document.createElement("div");
            top.className = "monitor-route-top";
            const nombre = document.createElement("span");
            nombre.textContent = (dato.metodo || "") + " " + (dato.ruta || "");
            const resumen = document.createElement("strong");
            resumen.textContent = numero(dato.peticiones) + " · " + numero(dato.p95) + " ms";
            top.append(nombre, resumen);

            const pista = document.createElement("span");
            pista.className = "monitor-route-track";
            pista.setAttribute("aria-hidden", "true");
            const relleno = document.createElement("span");
            relleno.style.width = (numero(dato.peticiones) * 100 / tope).toFixed(1) + "%";
            pista.append(relleno);
            item.append(top, pista);
            lista.append(item);
        }
    };

    const porcentaje = (valor) => Math.round(numero(valor)) + "%";

    const recurso = (clave, valores, actual, nombre) => {
        const reales = serie(valores);
        const ultimos = reales.slice(-MUESTRAS_RECURSO);
        const alineados = Array.from({ length: MUESTRAS_RECURSO }, (_, indice) => {
            const posicion = indice - (MUESTRAS_RECURSO - ultimos.length);
            return posicion < 0 ? 0 : ultimos[posicion];
        });
        const selectores = SELECTORES_RECURSO[clave];
        const puntos = coordsDe(alineados, 100, CONFIG_RECURSO);
        const area = uno(selectores.area);
        const linea = uno(selectores.linea);
        if (area) area.setAttribute("d", areaDe(puntos, CONFIG_RECURSO));
        if (linea) linea.setAttribute("d", curva(puntos));

        texto("[data-monitoreo-" + clave + "]", porcentaje(actual));
        texto("[data-monitoreo-" + clave + "-min]", porcentaje(reales.length ? Math.min(...reales) : actual));
        texto("[data-monitoreo-" + clave + "-max]", porcentaje(reales.length ? Math.max(...reales) : actual));
        texto("[data-monitoreo-" + clave + "-prom]",
                porcentaje(reales.length ? reales.reduce((suma, valor) => suma + valor, 0) / reales.length : actual));
        const grafico = uno(selectores.grafico);
        if (grafico) {
            grafico.setAttribute("aria-label", nombre + " durante el último minuto. Máximo "
                    + Math.round(numero(actual)) + "%.");
        }
    };

    const recursos = (datos) => {
        recurso("cpu", datos.serieCpu, datos.cpu, "Uso de CPU del proceso");
        recurso("memoria", datos.serieMemoria, datos.memoria, "Uso de memoria JVM");
    };

    const indicePuntero = (evento, grafico, anchoVista, config, total) => {
        if (!total || !grafico) return -1;
        const rect = grafico.getBoundingClientRect();
        if (!rect.width) return -1;
        const x = (evento.clientX - rect.left) * anchoVista / rect.width;
        return Math.max(0, Math.min(total - 1, Math.round(
                (x - config.izquierda) / (config.derecha - config.izquierda) * (total - 1))));
    };

    const crosshair = (selector, indice, total, config) => {
        const linea = uno(selector);
        if (!linea || indice < 0) return;
        const x = f(xEn(indice, total, config));
        linea.setAttribute("x1", x);
        linea.setAttribute("x2", x);
        linea.classList.add("is-visible");
    };

    const ocultarCrosshair = (selector) => {
        const linea = uno(selector);
        if (linea) linea.classList.remove("is-visible");
    };

    const mostrarTooltip = (evento, lineas) => {
        const tooltip = uno("[data-monitoreo-tooltip]");
        if (!tooltip) return;
        tooltip.textContent = lineas.filter(Boolean).join("\n");
        tooltip.hidden = false;
        const ancho = tooltip.offsetWidth || 240;
        const alto = tooltip.offsetHeight || 96;
        tooltip.style.left = Math.max(8, Math.min(window.innerWidth - ancho - 8, evento.clientX + 16)) + "px";
        tooltip.style.top = Math.max(8, Math.min(window.innerHeight - alto - 8, evento.clientY - 10)) + "px";
    };

    const ocultarTooltip = () => {
        const tooltip = uno("[data-monitoreo-tooltip]");
        if (tooltip) tooltip.hidden = true;
    };

    const configurarGraficos = () => {
        const graficoTrafico = uno("[data-grafico-trafico]");
        if (graficoTrafico) {
            graficoTrafico.addEventListener("pointermove", (evento) => {
                if (!instantanea) return;
                const etiquetas = Array.isArray(instantanea.etiquetas) ? instantanea.etiquetas : [];
                const indice = indicePuntero(evento, graficoTrafico, 960, CONFIG, etiquetas.length);
                if (indice < 0) return;
                crosshair("[data-crosshair-trafico]", indice, etiquetas.length, CONFIG);
                const exito = alinear(instantanea.serieExitosas, etiquetas.length)[indice];
                const redirect = alinear(instantanea.serieRedirecciones, etiquetas.length)[indice];
                const cliente = alinear(instantanea.serieErroresCliente, etiquetas.length)[indice];
                const servidor = alinear(instantanea.serieErroresServidor, etiquetas.length)[indice];
                mostrarTooltip(evento, [
                    etiquetas[indice] || "",
                    exito + " correctas · " + redirect + " redirecciones",
                    cliente + " errores 4xx · " + servidor + " errores 5xx",
                    "Total " + (exito + redirect + cliente + servidor) + " peticiones"
                ]);
            });
            graficoTrafico.addEventListener("pointerleave", () => {
                ocultarCrosshair("[data-crosshair-trafico]");
                ocultarTooltip();
            });
        }

        const graficoLatencia = uno("[data-grafico-latencia]");
        if (graficoLatencia) {
            graficoLatencia.addEventListener("pointermove", (evento) => {
                if (!instantanea) return;
                const etiquetas = Array.isArray(instantanea.etiquetas) ? instantanea.etiquetas : [];
                const indice = indicePuntero(evento, graficoLatencia, 960, CONFIG, etiquetas.length);
                if (indice < 0) return;
                crosshair("[data-crosshair-latencia]", indice, etiquetas.length, CONFIG);
                const p50 = alinear(instantanea.serieLatenciaP50, etiquetas.length)[indice];
                const p95 = alinear(instantanea.serieLatencia, etiquetas.length)[indice];
                const p99 = alinear(instantanea.serieLatenciaP99, etiquetas.length)[indice];
                mostrarTooltip(evento, [
                    etiquetas[indice] || "",
                    "p50 " + p50 + " ms · p95 " + p95 + " ms",
                    "p99 " + p99 + " ms"
                ]);
            });
            graficoLatencia.addEventListener("pointerleave", () => {
                ocultarCrosshair("[data-crosshair-latencia]");
                ocultarTooltip();
            });
        }

        const graficoActividad = uno("[data-grafico-actividad]");
        if (graficoActividad) {
            graficoActividad.addEventListener("pointermove", (evento) => {
                const dias = instantanea && Array.isArray(instantanea.actividadDiaria)
                        ? instantanea.actividadDiaria : [];
                const indice = indicePuntero(evento, graficoActividad, 900, CONFIG_ACTIVIDAD, dias.length);
                if (indice < 0) return;
                crosshair("[data-crosshair-actividad]", indice, dias.length, CONFIG_ACTIVIDAD);
                const dia = dias[indice] || {};
                mostrarTooltip(evento, [
                    dia.etiqueta || "",
                    "Matrículas " + numero(dia.inscripciones),
                    "Pagos " + numero(dia.pagos),
                    "Certificados " + numero(dia.certificados)
                ]);
            });
            graficoActividad.addEventListener("pointerleave", () => {
                ocultarCrosshair("[data-crosshair-actividad]");
                ocultarTooltip();
            });
        }

        for (const [clave, selectores] of Object.entries(SELECTORES_RECURSO)) {
            const grafico = uno(selectores.grafico);
            if (!grafico) continue;
            grafico.addEventListener("pointermove", (evento) => {
                if (!instantanea) return;
                const valores = serie(instantanea[selectores.campo]);
                const indice = indicePuntero(evento, grafico, 300, CONFIG_RECURSO, MUESTRAS_RECURSO);
                const real = indice - (MUESTRAS_RECURSO - valores.length);
                if (indice < 0 || real < 0 || real >= valores.length) {
                    ocultarCrosshair(selectores.crosshair);
                    ocultarTooltip();
                    return;
                }
                crosshair(selectores.crosshair, indice, MUESTRAS_RECURSO, CONFIG_RECURSO);
                mostrarTooltip(evento, [
                    selectores.nombre + " " + Math.round(valores[real]) + "%",
                    "Hace " + (valores.length - 1 - real) * 2 + " s"
                ]);
            });
            grafico.addEventListener("pointerleave", () => {
                ocultarCrosshair(selectores.crosshair);
                ocultarTooltip();
            });
        }
    };

    const render = (datos) => {
        instantanea = datos;
        estado(datos.estado);
        const fecha = new Date(datos.servidor);
        texto("[data-monitoreo-reloj]", Number.isNaN(fecha.getTime())
                ? (datos.servidor || "—")
                : new Intl.DateTimeFormat("es-AR", {
                    hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false
                }).format(fecha));

        texto("[data-monitoreo-disponibilidad]", numero(datos.disponibilidad) + "%");
        texto("[data-monitoreo-disponibilidad-chip]", numero(datos.peticiones) === 0 ? "sin tráfico" : "30 min");
        texto("[data-monitoreo-disponibilidad-foot]", numero(datos.erroresServidor) + " respuestas 5xx");
        texto("[data-monitoreo-ultimo-minuto]", numero(datos.peticionesUltimoMinuto));
        texto("[data-monitoreo-peticiones]", numero(datos.peticiones));
        texto("[data-monitoreo-errores]", numero(datos.erroresServidor));
        texto("[data-monitoreo-errores-count]", numero(datos.erroresServidor));
        texto("[data-monitoreo-tasa-errores]", numero(datos.tasaErrores) + "% del tráfico");
        texto("[data-monitoreo-exitosas]", numero(datos.exitosas));
        texto("[data-monitoreo-redirecciones]", numero(datos.redirecciones));
        texto("[data-monitoreo-errores-cliente]", numero(datos.erroresCliente));
        texto("[data-monitoreo-p95]", numero(datos.latenciaP95) + " ms");
        texto("[data-monitoreo-latencia-total]", numero(datos.latenciaP95) + " ms");
        texto("[data-monitoreo-latencia-nota]", numero(datos.latenciaP95) + " ms p95");
        texto("[data-monitoreo-activos]", numero(datos.actividad && datos.actividad.activos));

        const tendencia = numero(datos.tendenciaPeticiones);
        texto("[data-monitoreo-tendencia]", (tendencia > 0 ? "+" : "") + tendencia + "%");
        const chipTendencia = uno("[data-monitoreo-tendencia]");
        if (chipTendencia) {
            chipTendencia.className = "monitor-kpi-chip is-"
                    + (tendencia > 0 ? "up" : tendencia < 0 ? "down" : "flat");
        }

        const peticiones = serie(datos.seriePeticiones);
        const fallos = serie(datos.serieErroresServidor);
        spark("peticiones", peticiones);
        spark("errores", fallos);
        spark("latencia", serie(datos.serieLatencia));
        spark("disponibilidad", peticiones.map((cantidad, indice) => cantidad === 0
                ? 100 : Math.max(0, (cantidad - numero(fallos[indice])) * 100 / cantidad)));

        recursos(datos);
        trafico(datos);
        latencia(datos);
        franjas(datos);
        incidentes(datos.incidentes);
        eventosEn("[data-monitoreo-recientes]", datos.recientes,
                "Sin respuestas en la ventana.", true);
        eventosEn("[data-monitoreo-tabla-errores]", datos.eventos,
                "Sin errores recientes.", false);
        servicios(datos.servicios);
        actividad(datos.actividad);
        actividadDiaria(datos.actividadDiaria);
        rutas(datos.rutas);

        ultimaCarga = Date.now();
        document.dispatchEvent(new CustomEvent("monitoreo:actualizado", { detail: datos }));
    };

    const refrescar = async (manual = false) => {
        if (busy) return;
        if (!manual && ultimaCarga && Date.now() - ultimaCarga < 10000) return;
        busy = true;
        const inicio = Date.now();
        try {
            const respuesta = await fetch("/admin/api/monitoreo", {
                headers: { Accept: "application/json" },
                cache: "no-store"
            });
            if (!respuesta.ok) throw new Error("respuesta no válida");
            render(await respuesta.json());
            texto("[data-monitoreo-sonda]", "Actualizado en " + (Date.now() - inicio) + " ms");
        } catch (_) {
            estado("sin conexión");
            texto("[data-monitoreo-sonda]", "Sin conexión");
        } finally {
            busy = false;
        }
    };

    const reloj = () => {
        if (!ultimaCarga) return;
        const segundos = Math.max(0, Math.round((Date.now() - ultimaCarga) / 1000));
        texto("[data-monitoreo-actualizado]", "hace " + segundos + " s");
    };

    const botonRefrescar = uno("[data-monitoreo-refrescar]");
    if (botonRefrescar) botonRefrescar.addEventListener("click", () => refrescar(true));

    configurarGraficos();
    refrescar();
    setInterval(refrescar, 2000);
    setInterval(reloj, 1000);
    document.addEventListener("visibilitychange", () => {
        if (!document.hidden) refrescar();
    });
})();
