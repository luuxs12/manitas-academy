-- =====================================================================
-- V8: el avance se mide por video, no por número de lección.
--
-- Los cursos cargan su temario en videos, así que la lección que corresponda
-- a cada fila vieja se mapea al video del mismo orden dentro del curso.
-- Las filas que no tienen un video real se descartan: nunca se inventa
-- progreso sobre contenido que no existe.
-- =====================================================================

alter table lesson_progress
    add column video_id uuid;

update lesson_progress lp
   set video_id = mapped.video_id
  from (
      select e.id as enrollment_id,
             v.id as video_id,
             row_number() over (
                 partition by e.id
                 order by m.position, v.position, v.title
             ) as video_order
        from enrollments e
        join course_modules m on m.course_id = e.course_id
        join videos v on v.module_id = m.id
  ) mapped
 where mapped.enrollment_id = lp.enrollment_id
   and mapped.video_order = lp.lesson_number;

delete from lesson_progress where video_id is null;

alter table lesson_progress drop constraint lesson_progress_pkey;
alter table lesson_progress drop column lesson_number;
alter table lesson_progress alter column video_id set not null;

alter table lesson_progress
    add constraint fk_lesson_progress_video
    foreign key (video_id) references videos (id) on delete cascade;

alter table lesson_progress
    add primary key (enrollment_id, video_id);

create index ix_lesson_progress_video on lesson_progress (video_id);

comment on column lesson_progress.video_id is
    'Video real marcado como terminado dentro de la matrícula.';
