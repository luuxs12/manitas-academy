-- Panel administrativo: rol real, contenido editable, certificados emitidos
-- y auditoría de las acciones. Ninguna tabla se siembra con datos de ejemplo.

update users
   set role = 'ADMIN'
 where lower(email) = 'admin@damianprint.com';

create table audit_log (
    id          uuid        primary key default gen_random_uuid(),
    actor_id    uuid        not null references users (id),
    action      text        not null
                            check (action in ('CREATE', 'UPDATE', 'DELETE', 'ISSUE', 'REVOKE')),
    entity      text        not null,
    entity_id   uuid,
    detail      text        not null,
    created_at  timestamptz not null default now()
);
create index ix_audit_log_created on audit_log (created_at desc);
create index ix_audit_log_actor on audit_log (actor_id, created_at desc);

create table course_modules (
    id          uuid        primary key default gen_random_uuid(),
    course_id   uuid        not null references courses (id) on delete cascade,
    title       text        not null,
    position    integer     not null default 0,
    created_at  timestamptz not null default now()
);
create index ix_course_modules_course on course_modules (course_id, position);

create table videos (
    id          uuid        primary key default gen_random_uuid(),
    module_id   uuid        not null references course_modules (id) on delete cascade,
    title       text        not null,
    url         text        not null,
    position    integer     not null default 0,
    created_at  timestamptz not null default now()
);
create index ix_videos_module on videos (module_id, position);

create table exercises (
    id          uuid        primary key default gen_random_uuid(),
    module_id   uuid        not null references course_modules (id) on delete cascade,
    title       text        not null,
    prompt      text        not null,
    position    integer     not null default 0,
    created_at  timestamptz not null default now()
);
create index ix_exercises_module on exercises (module_id, position);

create table certificates (
    id          uuid        primary key default gen_random_uuid(),
    user_id     uuid        not null references users (id) on delete cascade,
    course_id   uuid        not null references courses (id) on delete cascade,
    code        text        not null unique,
    issued_at   timestamptz not null default now(),
    revoked_at  timestamptz
);
create unique index ux_certificates_active
    on certificates (user_id, course_id) where revoked_at is null;
create index ix_certificates_issued on certificates (issued_at desc);
