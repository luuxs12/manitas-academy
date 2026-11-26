/* Consola de monitoreo: solo pinta lo que devuelve /admin/api/monitoreo.
   Cada gráfico usa su propia unidad y se actualiza con datos reales. */

(() => {
    const root = document.querySelector("[data-monitoreo]");
    if (!root) return;

    const SVG = "http://www.w3.org/2000/svg";
    const INTERVALO = 2000;
    const IZQUIERDA = 48;
    const DERECHA = 930;
    const ARRIBA = 30;
    const ABAJO = 210;
    const uno = (selector) => root.querySelector(selector);
    let busy = false;
    let pausado = false;
    let ultimaCarga = 0;

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

    const xDe = (indice, total) => IZQUIERDA + indice * (DERECHA - IZQUIERDA) / Math.max(1, total - 1);

    const puntosDe = (valores, tope) => valores.map((valor, indice) =>
            xDe(indice, valores.length).toFixed(1) + "," + (ABAJO - valor * (ABAJO - ARRIBA) / tope).toFixed(1)
    ).join(" ");

    const areaDe = (valores, tope) => {
        if (!valores.length) return "";
        const puntos = valores.map((valor, indice) =>
                xDe(indice, valores.length).toFixed(1) + " "
                + (ABAJO - valor * (ABAJO - ARRIBA) / tope).toFixed(1));
        return "M " + puntos[0] + " L " + puntos.slice(1).join(" L ")
                + " L " + xDe(valores.length - 1, valores.length).toFixed(1) + " " + ABAJO
                + " L " + IZQUIERDA + " " + ABAJO + " Z";
    };

    const ejeX = (grupo, etiquetas) => {
        if (!grupo) return;
        grupo.replaceChildren();
        etiquetas.forEach((etiqueta, indice) => {
            if (indice % 5 !== 0 && indice !== etiquetas.length - 1) return;
            const textoEje = svg("text", {
                x: xDe(indice, etiquetas.length).toFixed(1),
                y: 238
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
        const grupo = uno("[data-barras-trafico]");
        const linea = uno("[data-linea-trafico]");
        const area = uno("[data-area-trafico]");
        const puntos = uno("[data-puntos-trafico]");
        if (!grupo) return;

        const etiquetas = Array.isArray(datos.etiquetas) ? datos.etiquetas : [];
        const exitosas = serie(datos.serieExitosas);
        const redirecciones = serie(datos.serieRedirecciones);
        const cliente = serie(datos.serieErroresCliente);
        const servidor = serie(datos.serieErroresServidor);
        const totales = etiquetas.map((_, indice) => [
            numero(exitosas[indice]), numero(redirecciones[indice]),
            numero(cliente[indice]), numero(servidor[indice])
        ].reduce((suma, valor) => suma + valor, 0));
        const tope = Math.max(1, ...totales);
        const escala = (ABAJO - ARRIBA) / tope;
        const ancho = (DERECHA - IZQUIERDA) / Math.max(1, etiquetas.length);
        const barra = Math.min(22, ancho * .62);

        grupo.replaceChildren();
        if (puntos) puntos.replaceChildren();
        etiquetas.forEach((etiqueta, indice) => {
            const centro = IZQUIERDA + ancho * (indice + .5);
            const segmentos = [
                [exitosas[indice], "ok", "2xx"],
                [redirecciones[indice], "redirect", "3xx"],
                [cliente[indice], "warn", "4xx"],
                [servidor[indice], "bad", "5xx"]
            ];
            let acumulado = 0;
            for (const [valor, nivel, nombre] of segmentos) {
                const cantidad = numero(valor);
                if (cantidad <= 0) continue;
                const altura = Math.max(1.5, cantidad * escala);
                const rect = svg("rect", {
                    x: (centro - barra / 2).toFixed(1),
                    y: (ABAJO - (acumulado + cantidad) * escala).toFixed(1),
                    width: barra.toFixed(1),
                    height: altura.toFixed(1),
                    rx: 2,
                    "class": "monitor-barra is-" + nivel
                });
                rect.append(titulo(etiqueta + " · " + nombre + ": " + cantidad));
                grupo.append(rect);
                acumulado += cantidad;
            }
            if (totales[indice] > 0 && puntos) {
                const circulo = svg("circle", {
                    cx: centro.toFixed(1),
                    cy: (ABAJO - totales[indice] * escala).toFixed(1),
                    r: 3.2,
                    "class": "monitor-punto"
                });
                circulo.append(titulo(etiqueta + " · " + totales[indice] + " peticiones"));
                puntos.append(circulo);
            }
        });

        if (linea) linea.setAttribute("points", puntosDe(totales, tope));
        if (area) area.setAttribute("d", areaDe(totales, tope));
        texto("[data-eje-trafico-max]", tope + " req/min");
        ejeX(uno("[data-eje-trafico]"), etiquetas);
        const grafico = uno("[data-grafico-trafico]");
        if (grafico) {
            grafico.setAttribute("aria-label", "Peticiones por minuto en los últimos 30 minutos. Máximo "
                    + tope + " por minuto.");
        }
    };

    const latencia = (datos) => {
        const etiquetas = Array.isArray(datos.etiquetas) ? datos.etiquetas : [];
        const p50 = serie(datos.serieLatenciaP50);
        const p95 = serie(datos.serieLatencia);
        const p99 = serie(datos.serieLatenciaP99);
        const tope = Math.max(1, ...p50, ...p95, ...p99);
        const puntos = uno("[data-puntos-latencia]");

        for (const [selector, valores] of [
            ["[data-linea-latencia-p50]", p50],
            ["[data-linea-latencia]", p95],
            ["[data-linea-latencia-p99]", p99]
        ]) {
            const linea = uno(selector);
            if (linea) linea.setAttribute("points", puntosDe(valores, tope));
        }
        const area = uno("[data-area-latencia]");
        if (area) area.setAttribute("d", areaDe(p95, tope));

        if (puntos) {
            puntos.replaceChildren();
            p95.forEach((valor, indice) => {
                if (valor <= 0) return;
                const circulo = svg("circle", {
                    cx: xDe(indice, p95.length).toFixed(1),
                    cy: (ABAJO - valor * (ABAJO - ARRIBA) / tope).toFixed(1),
                    r: 3.2,
                    "class": "monitor-punto monitor-punto--latencia"
                });
                circulo.append(titulo((etiquetas[indice] || "") + " · p95 " + valor + " ms"));
                puntos.append(circulo);
            });
        }

        texto("[data-eje-latencia-max]", tope + " ms");
        ejeX(uno("[data-eje-latencia]"), etiquetas);
        const grafico = uno("[data-grafico-latencia]");
        if (grafico) {
            grafico.setAttribute("aria-label", "Percentiles de latencia por minuto en los últimos 30 minutos. Máximo "
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
            const franja = document.createElement("span");
            franja.className = "admin-franja is-" + nivel;
            franja.style.setProperty("--altura", Math.max(8, Math.round(cantidad * 100 / tope)) + "%");
            franja.title = etiqueta + ": " + cantidad + " peticiones, " + fallos
                    + " errores 5xx, p95 " + latencia + " ms";
            contenedor.append(franja);
        });
        contenedor.setAttribute("aria-label", "Estado por minuto durante los últimos "
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
        const grupo = uno("[data-barras-actividad]");
        const eje = uno("[data-eje-actividad]");
        const maximo = uno("[data-eje-actividad-max]");
        if (!grupo || !Array.isArray(dias)) return;

        grupo.replaceChildren();
        if (eje) eje.replaceChildren();
        const tope = Math.max(1, ...dias.flatMap((dia) => [
            numero(dia.inscripciones), numero(dia.pagos), numero(dia.certificados)
        ]));
        if (maximo) maximo.textContent = String(tope);

        const ancho = 820 / Math.max(1, dias.length);
        const barra = Math.min(20, ancho / 4.5);
        const series = [
            ["inscripciones", "admin-bar--inscripciones"],
            ["pagos", "admin-bar--pagos"],
            ["certificados", "admin-bar--certificados"]
        ];

        dias.forEach((dia, indice) => {
            const centro = 40 + ancho * (indice + .5);
            series.forEach(([clave, clase], numeroSerie) => {
                const valor = numero(dia[clave]);
                const altura = valor * 170 / tope;
                const rect = svg("rect", {
                    x: (centro - barra * 1.5 + numeroSerie * barra).toFixed(1),
                    y: (210 - altura).toFixed(1),
                    width: barra.toFixed(1),
                    height: altura.toFixed(1),
                    rx: 2,
                    "class": "admin-bar " + clase
                });
                rect.append(titulo((dia.etiqueta || "") + " · " + clave + ": " + valor));
                grupo.append(rect);
            });
            if (eje) {
                const etiqueta = svg("text", { x: centro.toFixed(1), y: 235 });
                etiqueta.textContent = dia.etiqueta || "";
                eje.append(etiqueta);
            }
        });
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

    const render = (datos) => {
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
    };

    const reiniciarBarra = () => {
        const barra = uno("[data-monitoreo-barra]");
        if (!barra) return;
        barra.style.animation = "none";
        void barra.offsetWidth;
        barra.style.animation = "";
    };

    const refrescar = async (manual = false) => {
        if (busy || (pausado && !manual)) return;
        busy = true;
        const inicio = Date.now();
        try {
            const respuesta = await fetch("/admin/api/monitoreo", {
                headers: { Accept: "application/json" },
                cache: "no-store"
            });
            if (!respuesta.ok) throw new Error("respuesta no válida");
            render(await respuesta.json());
            texto("[data-monitoreo-sonda]", "Sondeo " + (Date.now() - inicio) + " ms");
            reiniciarBarra();
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

    const botonPausa = uno("[data-monitoreo-pausa]");
    if (botonPausa) {
        botonPausa.addEventListener("click", () => {
            pausado = !pausado;
            botonPausa.textContent = pausado ? "Reanudar" : "Pausar";
            texto("[data-monitoreo-sonda]", pausado ? "En pausa" : "Sondeo 2 s");
            if (!pausado) refrescar();
        });
    }

    refrescar();
    setInterval(refrescar, 2000);
    setInterval(reloj, 1000);
    document.addEventListener("visibilitychange", () => {
        if (!document.hidden && !pausado) refrescar();
    });
})();
