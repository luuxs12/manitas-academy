# Guía para cargar el contenido de un curso

Esta guía es para cuentas con rol `ADMIN`. Todo lo que se carga se guarda en
PostgreSQL y lo ve el estudiante en `/panel/cursos/{slug}` sin recargar ni
copiar datos a otro sistema.

## Acceso

Entrá a `/admin`, abrí **Cursos** y tocá **Contenido** en el curso que querés
armar. También podés ir directo a `/admin/cursos/{id}/contenido`, donde `{id}` es
el identificador del curso. Las rutas del panel administrativo exigen
`ROLE_ADMIN`.

Antes de cargar contenido, el curso debe existir y tener al menos un módulo. Los
módulos se crean desde **Cursos** con su título y posición; en el temario se
muestran como “Módulo 1”, “Módulo 2” y así sucesivamente, seguidos del título
real. En **Módulos** elegí primero el curso para ver y ordenar solo sus módulos;
en **Videos** y **Ejercicios**, elegí primero el curso y después el módulo: la
lista de módulos se reduce al curso seleccionado.

## Cómo se organiza el contenido

| Nivel | Qué es | Dónde se carga |
|---|---|---|
| Módulo | Agrupador principal del curso | Alta de módulos en **Módulos** |
| Sección | Agrupador opcional dentro de un módulo | Formulario “Nueva sección” de esta página |
| Ítem | Una clase (`VIDEO`), su versión en señas (`VIDEO_LSP`), un enlace (`MATERIAL`) o una consigna (`EJERCICIO`) | Formulario “Nuevo ítem” de esta página |
| Sílabo | Objetivos, requisitos y recursos complementarios | Bloque “Sílabo” de esta página |

Los ítems que no tienen sección quedan bajo el rótulo **“Sin sección”** en el
temario del estudiante. No hace falta crear una sección para empezar.

## Paso a paso

### 1. Crear una sección

En “Nueva sección” elegí el módulo, escribí un título, una posición y si querés
que sea visible. Las secciones invisibles no aparecen en el temario del alumno;
sí se ven en esta página para que puedas seguir trabajando.

### 2. Cargar ítems

En “Nuevo ítem” completá:

| Campo | Para qué sirve |
|---|---|
| Módulo y sección | Dónde va el ítem. La sección es opcional. |
| Tipo | `VIDEO`, `VIDEO_LSP`, `MATERIAL` o `EJERCICIO`. |
| Título | Texto que aparece en el temario. Hasta 140 caracteres. |
| Descripción | Texto de apoyo, hasta 4000 caracteres. Es opcional. |
| Enlace | Solo para `MATERIAL`: dirección `https` hasta 300 caracteres. |
| Enlace de YouTube | Solo para `VIDEO` y `VIDEO_LSP`: enlace o ID de 11 caracteres de YouTube. |
| Duración | Segundos del video, entre 0 y 86400. Se muestra en el temario. |
| Posición | Orden dentro de la sección o del módulo, entre 0 y 9999. |
| Visible | Si está desmarcado, el ítem queda como borrador y no se muestra al alumno. |

Los enlaces se validan en el servidor: los materiales aceptan solo `https` con
host explícito, y los videos aceptan solo dominios de YouTube. Para subir un
archivo propio se usa **Nuevo documento** en el encabezado de la página: acepta
PDF, DOC, DOCX, PPT, PPTX o TXT de hasta 10 MB y lo guarda con un nombre interno
seguro. Los PDFs subidos se abren en el visor del panel del alumno desde “Ver
material” y también se pueden descargar desde “Descargar documento”.

Para cada clase cargada como `VIDEO`, agregá un `VIDEO_LSP` con el mismo título y
posición. Las cuentas que marcaron **“Uso LSP”** al registrarse (o después en
Accesibilidad) ven solo la versión en señas; las demás ven el video habitual.
Clickear una sesión del temario abre su video a la izquierda.

Los ejercicios se corrigen con 10 preguntas de 2 puntos: con 20/20 quedan
marcados como vistos, y con cualquier otro puntaje se pueden volver a rendir.
En `/admin/ejercicios` se ve la cantidad de preguntas cargadas y el estado. Si
un ejercicio se resuelve sin cuestionario, desmarcá **“Marcar al rendir”** para
poder marcarlo como visto a mano desde el temario.

### 3. Ordenar y borrar

Las flechas de cada sección y de cada ítem los suben o bajan dentro de su grupo.
El botón **“Acciones”** abre un modal con las opciones de esa fila: editar cuando
corresponde, ocultar o mostrar y eliminar. La visibilidad se guarda recién
después de confirmar, y el alumno deja de ver el bloque. Para borrar, el modal
muestra el título real; el borrado recién se ejecuta con un POST que conserva el
token CSRF. Sin JavaScript se usa la página de confirmación. No se abren
ventanas nuevas.

Al borrar una sección, sus ítems pasan a “Sin sección” y conservan su progreso.
Al borrar un ítem, se borra también el avance asociado a ese ítem.

### 4. Completar el sílabo

El bloque “Sílabo” permite guardar los objetivos y los requisitos. Los recursos
son enlaces `https` con título y posición, y se muestran como apoyo del curso.
Los recursos se agregan con “Agregar recurso” y se quitan con su botón “Quitar”,
que también envía un POST con token CSRF.

### 5. Publicar

Un curso en `BORRADOR` no se muestra en el catálogo ni se puede inscribir.
Cuando el temario y el sílabo estén listos, cambiá el estado a `PUBLICADO` desde
la ficha del curso en **Cursos**.

## Qué ve el estudiante

- El temario con “Módulo N”, las secciones y los ítems “Sin sección”.
- En cada ítem: el tipo, el título, la descripción y el estado **Visto** o
  **Pendiente**. Al lado de la duración del video, los demás ítems muestran
  **“Sin fecha límite”**; no hay fechas de disponibilidad.
- Los videos se abren en el reproductor de la página, con YouTube sin cookies y
  sin videos relacionados. El tema, el contraste y la velocidad los controla
  YouTube; desde acá no se fuerzan.
- Un botón **“Marcar como visto”** para videos, materiales y ejercicios con
  marcado manual, que también permite volver a “Pendiente”. El avance se guarda
  en la base y actualiza la barra de progreso. Los ejercicios configurados para
  marcar al rendir se completan solo con 20/20.
- Si el curso es de paga y la cuenta es gratuita, el contenido aparece como
  **“Requiere Premium”** y no se pueden marcar ítems como vistos.

## Datos y auditoría

Cada alta, movida, borrado y guardado de sílabo registra una auditoría con el
identificador de quien lo hizo y un mensaje en el
log. Los videos y ejercicios
creados desde el alta clásica siguen sincronizados con los ítems mediante los
triggers de la migración `V14__estructura_contenido.sql`, así que el progreso
anterior no se pierde.

## Monitoreo

En el sidebar, entre **Reportes** y **Auditoría**, está **Monitoreo**. Esa vista
es una consola en vivo: reloj y sondeo cada 2 segundos, disponibilidad, ritmo
actual, CPU y memoria del proceso con curvas que se desplazan, tráfico por tipo
de respuesta, latencia p50/p95/p99, mapa de calor por minuto, rutas más
solicitadas, feed de respuestas y actividad real (altas, matrículas, lecciones,
cuestionarios aprobados, certificados y pagos). Las caídas
se detectan por respuestas `5xx` o por fallo de PostgreSQL, y los eventos
conservan el `requestId` de los logs.

Los datos los sirve `/admin/api/monitoreo` cada 2 segundos. El navegador no
consulta Actuator: las métricas recientes viven en un buffer acotado del
servidor y los conteos de negocio se leen de PostgreSQL con cache corta.

## Mi cuenta

El menú de la derecha del encabezado muestra tu nombre, **Mi perfil**,
**Configuración** y **Cerrar sesión**. En **Mi perfil** podés subir o quitar la
foto que aparece en la cabecera y en ese mismo menú. El archivo se elige desde
un área con icono de cámara: también se puede arrastrar una imagen encima.
Acepta JPG, PNG o WebP de hasta 2 MB; si no hay foto, se muestran tus iniciales.
El formulario de documentos usa el mismo tipo de área: elige o arrastra el
archivo y el cuadro pasa a mostrar su nombre.

Las migraciones ya ejecutadas **no se editan**: un cambio nuevo va en una
migración nueva, sin modificar las anteriores.
