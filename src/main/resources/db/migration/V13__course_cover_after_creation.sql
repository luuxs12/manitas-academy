-- =====================================================================
-- V13: la portada se sube después de crear el curso.
--
-- El panel crea primero la ficha y recién después permite subir la imagen.
-- Hasta ese momento el curso queda en borrador sin portada; la publicación
-- sigue exigiendo una imagen real.
-- =====================================================================

alter table courses
    alter column image drop not null;

comment on column courses.image is
    'Portada local; vacía mientras el curso está en borrador y todavía no se subió.';
