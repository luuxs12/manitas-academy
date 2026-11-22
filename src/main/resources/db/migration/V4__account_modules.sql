-- =====================================================================
-- V4: módulos de cuenta, avance real, avisos y cobros
--
-- Ninguna tabla se siembra con datos de ejemplo. Las preferencias se
-- crean con valores por defecto cuando la persona las abre; el avance,
-- los avisos y los pagos se escriben solo cuando ocurre una acción real.
-- =====================================================================

-- ---------------------------------------------------------------------
-- La cuenta conserva la foto, las preferencias y el estado de suscripción.
-- Los datos de tarjeta nunca se guardan acá: solo ids de Stripe.
-- ---------------------------------------------------------------------
alter table users
    add column avatar_path text,
    add column theme text not null default 'SYSTEM'
        check (theme in ('SYSTEM', 'LIGHT', 'DARK')),
    add column notify_email boolean not null default true,
    add column stripe_customer_id text unique,
    add column subscription_end timestamptz,
    add column cancel_at_period_end boolean not null default false;

comment on column users.avatar_path is
    'Ruta pública de la foto de perfil; vacía cuando se usan las iniciales.';
comment on column users.subscription_end is
    'Fin del período pagado, informado por Stripe.';
comment on column users.cancel_at_period_end is
    'La cancelación rige al final del período ya pagado.';

-- ---------------------------------------------------------------------
-- Avance por lección. El número se valida contra courses.lessons, así que
-- no hace falta duplicar el temario ni inventar contenidos.
-- ---------------------------------------------------------------------
create table lesson_progress (
    enrollment_id uuid        not null references enrollments (id) on delete cascade,
    lesson_number integer     not null check (lesson_number > 0),
    completed_at  timestamptz not null default now(),
    primary key (enrollment_id, lesson_number)
);

create index ix_lesson_progress_enrollment on lesson_progress (enrollment_id);

-- ---------------------------------------------------------------------
-- Historial de pagos: una fila por checkout, con el estado que informa
-- Stripe. El monto y la moneda son los del cobro real.
-- ---------------------------------------------------------------------
create table payments (
    id                       uuid        primary key default gen_random_uuid(),
    user_id                  uuid        not null references users (id) on delete cascade,
    plan                     text        not null check (plan in ('MONTHLY', 'ANNUAL')),
    amount_cents             integer     not null check (amount_cents > 0),
    currency                 text        not null default 'PEN',
    status                   text        not null default 'PENDING'
                                         check (status in ('PENDING', 'PAID', 'FAILED', 'REFUNDED')),
    stripe_session_id        text        unique,
    stripe_payment_intent_id text,
    stripe_subscription_id   text,
    period_end               timestamptz,
    created_at               timestamptz not null default now(),
    paid_at                  timestamptz
);

create index ix_payments_user on payments (user_id, created_at desc);

-- ---------------------------------------------------------------------
-- Avisos generados por acciones reales: inscripción, finalización,
-- pago confirmado y cancelación.
-- ---------------------------------------------------------------------
create table notifications (
    id         uuid        primary key default gen_random_uuid(),
    user_id    uuid        not null references users (id) on delete cascade,
    kind       text        not null
                           check (kind in ('ENROLLMENT', 'COMPLETION', 'PAYMENT', 'SYSTEM')),
    title      text        not null,
    message    text        not null,
    target_url text,
    read_at    timestamptz,
    created_at timestamptz not null default now()
);

create index ix_notifications_user on notifications (user_id, created_at desc);
create index ix_notifications_unread on notifications (user_id, read_at)
    where read_at is null;
