-- =====================================================================
-- V1: cuentas, roles y preferencias de accesibilidad
--
-- Crea el punto de partida de la plataforma: quién entra al sistema
-- y cómo quiere ver el contenido (Lengua de Señas, subtítulos).
--
-- Convenciones:
--   * uuid para identificadores (no se pueden adivinar desde la URL).
--   * timestamptz para fechas (guardan zona horaria).
--   * text en lugar de varchar (PostgreSQL no gana nada con el límite).
--   * check para valores cerrados (rol, tamaño de subtítulo).
-- =====================================================================

-- ---------------------------------------------------------------------
-- Usuarios
-- ---------------------------------------------------------------------
create table users (
    id            uuid        primary key default gen_random_uuid(),
    email         text        not null,
    password_hash text        not null,
    full_name     text        not null,
    role          text        not null default 'STUDENT'
                              check (role in ('STUDENT', 'TEACHER', 'ADMIN')),
    active        boolean     not null default true,
    locale        text        not null default 'es-PE',
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()
);

-- El email no distingue mayúsculas: Ana@Mail.com y ana@mail.com son la misma cuenta.
create unique index ux_users_email on users (lower(email));

create index ix_users_role on users (role);
create index ix_users_active on users (active);

-- ---------------------------------------------------------------------
-- Preferencias de accesibilidad (1 a 1 con el usuario)
--
-- No se guarda ninguna discapacidad ni dato médico: solo la preferencia
-- de cómo mostrar el contenido. Es opcional y modificable.
-- ---------------------------------------------------------------------
create table accessibility_preferences (
    user_id                uuid        primary key references users (id) on delete cascade,
    sign_language_enabled  boolean     not null default false,
    sign_language_variant  text,
    captions_enabled       boolean     not null default true,
    caption_size           text        not null default 'MEDIUM'
                                       check (caption_size in ('SMALL', 'MEDIUM', 'LARGE')),
    playback_speed         numeric(3, 2) not null default 1.00
                                       check (playback_speed between 0.50 and 2.00),
    updated_at             timestamptz not null default now()
);

-- ---------------------------------------------------------------------
-- Trigger reutilizable para updated_at
-- ---------------------------------------------------------------------
create or replace function set_updated_at()
returns trigger as $$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;

create trigger trg_users_updated
    before update on users
    for each row execute function set_updated_at();

create trigger trg_accessibility_preferences_updated
    before update on accessibility_preferences
    for each row execute function set_updated_at();
