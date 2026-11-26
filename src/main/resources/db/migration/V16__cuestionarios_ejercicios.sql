-- =====================================================================
-- V16: los ejercicios pasan a ser cuestionarios.
--
-- Cada ejercicio tiene 10 preguntas de 2 puntos (20 en total) con 4
-- alternativas y una sola correcta. El resultado del alumno vive aparte
-- del avance de videos y materiales: un intento por matrícula y ejercicio,
-- inmutable, con el detalle de lo que respondió.
-- =====================================================================

create table quiz_questions (
    id          uuid        primary key default gen_random_uuid(),
    exercise_id uuid        not null references exercises (id) on delete cascade,
    position    integer     not null check (position between 1 and 10),
    prompt      text        not null,
    points      integer     not null default 2 check (points = 2),
    created_at  timestamptz not null default now(),
    constraint ux_quiz_questions_position unique (exercise_id, position)
);
create index ix_quiz_questions_exercise on quiz_questions (exercise_id, position);

create table quiz_alternatives (
    id          uuid        primary key default gen_random_uuid(),
    question_id uuid        not null references quiz_questions (id) on delete cascade,
    position    integer     not null check (position between 1 and 4),
    label       text        not null,
    is_correct  boolean     not null default false,
    created_at  timestamptz not null default now(),
    constraint ux_quiz_alternatives_position unique (question_id, position),
    constraint ux_quiz_alternatives_id_question unique (id, question_id)
);
create unique index ux_quiz_alternatives_correct
    on quiz_alternatives (question_id) where is_correct;
create index ix_quiz_alternatives_question on quiz_alternatives (question_id, position);

create table quiz_attempts (
    id            uuid        primary key default gen_random_uuid(),
    enrollment_id uuid        not null references enrollments (id) on delete cascade,
    exercise_id   uuid        not null references exercises (id) on delete cascade,
    score         integer     not null check (score between 0 and 20),
    submitted_at  timestamptz not null default now(),
    constraint ux_quiz_attempts_enrollment_exercise unique (enrollment_id, exercise_id)
);
create index ix_quiz_attempts_exercise on quiz_attempts (exercise_id, submitted_at);

create table quiz_attempt_answers (
    id             uuid    primary key default gen_random_uuid(),
    attempt_id     uuid    not null references quiz_attempts (id) on delete cascade,
    question_id    uuid    not null,
    alternative_id uuid    not null,
    points_earned  integer not null check (points_earned in (0, 2)),
    constraint ux_quiz_attempt_answers_question unique (attempt_id, question_id),
    constraint fk_quiz_answers_alternative
        foreign key (alternative_id, question_id)
        references quiz_alternatives (id, question_id) on delete cascade
);
create index ix_quiz_attempt_answers_attempt on quiz_attempt_answers (attempt_id);

comment on table quiz_questions is
    'Las diez preguntas de cada ejercicio; cada acierto vale dos puntos.';
comment on table quiz_alternatives is
    'Cuatro alternativas por pregunta, con una sola respuesta correcta.';
comment on table quiz_attempts is
    'Intento único y definitivo del alumno; el puntaje se calcula al enviar.';
comment on table quiz_attempt_answers is
    'Respuesta elegida por pregunta y puntos obtenidos en el intento.';
