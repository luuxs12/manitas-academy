-- =====================================================================
-- V3: catálogo de cursos, matrículas y tipo de cuenta
--
-- El catálogo deja de vivir en memoria: los cursos, las inscripciones y
-- el avance quedan en PostgreSQL. La cuenta guarda su plan para decidir
-- a qué cursos puede inscribirse.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Tipo de cuenta
-- ---------------------------------------------------------------------
alter table users
    add column plan text not null default 'FREE'
        check (plan in ('FREE', 'MONTHLY', 'ANNUAL'));

comment on column users.plan is
    'Plan contratado: FREE, MONTHLY o ANNUAL.';

-- ---------------------------------------------------------------------
-- Catálogo de cursos
-- ---------------------------------------------------------------------
create table courses (
    id         uuid    primary key default gen_random_uuid(),
    slug       text    not null unique,
    title      text    not null,
    level      text    not null,
    lessons    integer not null check (lessons > 0),
    free       boolean not null default true,
    category   text    not null,
    summary    text    not null,
    image      text    not null,
    position   integer not null default 0,
    created_at timestamptz not null default now()
);

create index ix_courses_category on courses (category);
create index ix_courses_position on courses (position);

-- Los seis cursos del catálogo. El orden define cómo se muestran.
insert into courses (slug, title, level, lessons, free, category, summary, image, position)
values
    ('python-desde-cero',
     'Python desde cero',
     'Nivel inicial',
     32,
     true,
     'Programación',
     'Variables, ciclos y funciones con ejercicios cortos para automatizar tareas reales.',
     '/img/cursos/python.jpg',
     1),
    ('desarrollo-web-html-css',
     'Desarrollo web con HTML y CSS',
     'Nivel inicial',
     28,
     true,
     'Desarrollo web',
     'Armá tus primeras páginas: estructura, estilos y diseño adaptable desde el primer día.',
     '/img/cursos/desarrollo-web.jpg',
     2),
    ('redes-y-soporte-tecnico',
     'Redes y soporte técnico',
     'Nivel intermedio',
     24,
     false,
     'Redes',
     'Cableado, direcciones IP y diagnóstico de fallas para dejar una red funcionando.',
     '/img/cursos/redes.jpg',
     3),
    ('ciberseguridad-basica',
     'Ciberseguridad básica',
     'Nivel intermedio',
     20,
     false,
     'Seguridad',
     'Contraseñas, phishing y buenas prácticas para proteger cuentas y equipos.',
     '/img/cursos/ciberseguridad.jpg',
     4),
    ('bases-de-datos-sql',
     'Bases de datos con SQL',
     'Nivel intermedio',
     26,
     false,
     'Datos',
     'Modelá tablas y escribí consultas para convertir datos sueltos en respuestas.',
     '/img/cursos/bases-de-datos.jpg',
     5),
    ('mantenimiento-de-hardware',
     'Mantenimiento de hardware',
     'Nivel inicial',
     18,
     false,
     'Hardware',
     'Limpieza, diagnóstico y cambio de piezas: el ABC del soporte en sitio.',
     '/img/cursos/hardware.jpg',
     6);

-- ---------------------------------------------------------------------
-- Matrículas (una fila por persona y curso)
-- ---------------------------------------------------------------------
create table enrollments (
    id           uuid        primary key default gen_random_uuid(),
    user_id      uuid        not null references users (id) on delete cascade,
    course_id    uuid        not null references courses (id) on delete cascade,
    enrolled_at  timestamptz not null default now(),
    completed_at timestamptz,
    constraint ux_enrollments_user_course unique (user_id, course_id)
);

create index ix_enrollments_user on enrollments (user_id);
create index ix_enrollments_course on enrollments (course_id);
create index ix_enrollments_completed on enrollments (user_id, completed_at);
