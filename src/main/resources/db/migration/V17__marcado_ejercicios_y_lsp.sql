-- =====================================================================
-- V17: marcado configurable de ejercicios e identificador LSP.
--
-- Un ejercicio puede marcarse solo al rendir el cuestionario (valor
-- inicial, comportamiento que ya tenia la plataforma) o a mano desde el
-- temario. La cuenta guarda si la persona necesita lengua de senas y el
-- temario puede tener un item VIDEO_LSP independiente del video comun.
-- =====================================================================

alter table exercises
    add column mark_on_quiz boolean not null default true;

alter table users
    add column uses_lsp boolean not null default false;

alter table content_items drop constraint if exists content_items_source_type_check;
alter table content_items add constraint ck_content_items_source_type
    check (source_type in ('VIDEO', 'VIDEO_LSP', 'MATERIAL', 'EJERCICIO'));

comment on column exercises.mark_on_quiz is
    'El ejercicio se marca como visto al enviar el cuestionario; si es false se marca a mano.';
comment on column users.uses_lsp is
    'Identificador de cuenta: muestra los videos en lengua de senas disponibles.';
