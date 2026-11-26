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
real.

## Cómo se organiza el contenido

| Nivel | Qué es | Dónde se carga |
|---|---|---|
| Módulo | Agrupador principal del curso | Alta de módulos en **Cursos** |
| Sección | Agrupador opcional dentro de un módulo | Formulario “Nueva sección” de esta página |
| Ítem | Una clase (`VIDEO`), un enlace (`MATERIAL`) o una consigna (`EJERCICIO`) | Formulario “Nuevo ítem” de esta página |
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
| Tipo | `VIDEO`, `MATERIAL` o `EJERCICIO`. |
| Título | Texto que aparece en el temario. Hasta 140 caracteres. |
| Descripción | Texto de apoyo, hasta 4000 caracteres. Es opcional. |
| Enlace | Solo para `MATERIAL`: dirección `https` hasta 300 caracteres. |
| Enlace de YouTube | Solo para `VIDEO`: enlace o ID de 11 caracteres de YouTube. |
| Duración | Segundos del video, entre 0 y 86400. Se muestra en el temario. |
| Posición | Orden dentro de la sección o del módulo, entre 0 y 9999. |
| Visible | Si está desmarcado, el ítem queda como borrador y no se muestra al alumno. |

Los enlaces se validan en el servidor: los materiales aceptan solo `https` con
host explícito, y los videos aceptan solo dominios de YouTube. Para subir un
archivo propio se usa **Nuevo documento** en el encabezado de la página: acepta
PDF, DOC, DOCX, PPT, PPTX o TXT de hasta 10 MB y lo guarda con un nombre interno
seguro. Los PDFs subidos se abren en el visor del panel del alumno desde “Ver
material” y también se pueden descargar desde “Descargar documento”.

### 3. Ordenar y borrar

Las flechas de cada sección y de cada ítem los suben o bajan dentro de su grupo.
Para borrar, primero se abre una **página de confirmación** con el título real;
el borrado recién se ejecuta con el botón de esa página, mediante un POST con
token CSRF. No hay diálogos ni ventanas emergentes.

Al borrar una sección, sus ítems pasan a “Sin sección” y conservan su progreso.
Al borrar un ítem, se borra también el avance asociado a ese ítem.

### 4. Completar el sílabo

El bloque “Sílabo” permite guardar los objetivos y los requisitos. Los recursos
son enlaces `https` con título y posición, y se muestran como apoyo del curso.
Los recursos se agregan con “Agregar recurso” y se quitan con su botón “Quitar”,
que también envía un POST con token CSRF.

### 5. Descargar PDF de la sesión

En el encabezado de cada módulo del temario hay un enlace **“Descargar PDF de la
sesión”**. Es un GET a `/admin/exportar/sesion/modulos/{moduleId}/pdf` y genera un
PDF con el curso, el título real del módulo, sus secciones y sus ítems con tipo y
estado. Como la vista previa administrativa incluye los ocultos, el PDF también
los incluye y los marca como “Oculta”. No se inventa contenido: todo sale de lo
cargado en esta página.

### 6. Publicar

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
- Un botón **“Marcar como visto”** para cada ítem, que también permite volver a
  “Pendiente”. El avance se guarda en la base y actualiza la barra de progreso.
- Si el curso es de paga y la cuenta es gratuita, el contenido aparece como
  **“Requiere Premium”** y no se pueden marcar ítems como vistos.

## Datos y auditoría

Cada alta, movida, borrado, guardado de sílabo y descarga de un PDF de sesión
registra una auditoría con el identificador de quien lo hizo y un mensaje en el
log. Los videos y ejercicios
creados desde el alta clásica siguen sincronizados con los ítems mediante los
triggers de la migración `V14__estructura_contenido.sql`, así que el progreso
anterior no se pierde.

Las migraciones ya ejecutadas **no se editan**: un cambio nuevo va en una
migración posterior a `V16`.
