-- =====================================================================
-- V2: recuperación de contraseña
--
-- Guarda solo el hash SHA-256 del token que viaja por correo. Si alguien
-- lee la base no puede usar esos valores para entrar: el token original
-- existe únicamente en el enlace del mensaje.
--
-- Cada token sirve una vez, vence a los 60 minutos y se borra cuando se
-- pide uno nuevo para la misma cuenta.
-- =====================================================================

create table password_reset_tokens (
    id         uuid        primary key default gen_random_uuid(),
    user_id    uuid        not null references users (id) on delete cascade,
    token_hash text        not null,
    expires_at timestamptz not null,
    used_at    timestamptz,
    created_at timestamptz not null default now()
);

create unique index ux_password_reset_token_hash
    on password_reset_tokens (token_hash);

create index ix_password_reset_user
    on password_reset_tokens (user_id);
