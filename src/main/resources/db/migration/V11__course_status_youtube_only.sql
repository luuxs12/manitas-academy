-- Los cursos tienen estado real, los videos solo viven en YouTube y los
-- avisos de certificado dejan de chocar con la restricción de la tabla.

alter table courses
    add column status text not null default 'BORRADOR'
        check (status in ('BORRADOR', 'PUBLICADO'));

-- Un borrador puede tener cero lecciones; el contador se reconstruye desde
-- el contenido real, no desde una cifra escrita a mano.
alter table courses drop constraint if exists courses_lessons_check;
alter table courses add constraint courses_lessons_check check (lessons >= 0);

update courses c
   set lessons = (select count(*) from videos v
                    join course_modules m on m.id = v.module_id
                   where m.course_id = c.id);

alter table notifications drop constraint if exists notifications_kind_check;
alter table notifications add constraint notifications_kind_check
    check (kind in ('ENROLLMENT', 'COMPLETION', 'PAYMENT', 'SYSTEM', 'CERTIFICATE'));

-- Ya no se guardan archivos de video ni subtítulos propios.
alter table videos drop column if exists url;
alter table videos drop column if exists object_key;
alter table videos drop column if exists subtitle_key;
alter table videos drop column if exists duration_seconds;
alter table videos drop column if exists transcript;
