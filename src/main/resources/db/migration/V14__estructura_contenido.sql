-- =====================================================================
-- V14: módulo -> sección -> ítems (VIDEO, MATERIAL, EJERCICIO).
--
-- Conserva íntegramente módulos, videos, ejercicios, matrículas y avance:
-- los ítems nuevos son una capa de orden y visibilidad sobre el contenido
-- que ya existe. Los disparadores mantienen esa capa sin tocar el CRUD actual.
-- =====================================================================

alter table course_modules
    add column visible boolean not null default true;

alter table courses
    add column objectives text,
    add column requirements text;

create table course_sections (
    id          uuid        primary key default gen_random_uuid(),
    module_id   uuid        not null references course_modules (id) on delete cascade,
    title       text        not null,
    position    integer     not null default 0,
    visible     boolean     not null default true,
    created_at  timestamptz not null default now()
);
create index ix_course_sections_module on course_sections (module_id, position);

create table content_items (
    id               uuid        primary key default gen_random_uuid(),
    module_id        uuid        not null references course_modules (id) on delete cascade,
    section_id       uuid        references course_sections (id) on delete set null,
    source_type      text        not null check (source_type in ('VIDEO', 'MATERIAL', 'EJERCICIO')),
    source_id        uuid,
    title            text        not null,
    description      text,
    url              text        check (url is null or url like 'https://%'),
    youtube_id       varchar(11) check (youtube_id is null
                                        or youtube_id ~ '^[A-Za-z0-9_-]{11}$'),
    duration_seconds integer     check (duration_seconds is null or duration_seconds > 0),
    position         integer     not null default 0,
    visible          boolean     not null default true,
    created_at       timestamptz not null default now(),
    constraint ux_content_items_source unique (source_type, source_id)
);
create index ix_content_items_module on content_items (module_id, position);
create index ix_content_items_section on content_items (section_id, position);

create table course_resources (
    id        uuid primary key default gen_random_uuid(),
    course_id uuid not null references courses (id) on delete cascade,
    title     text not null,
    url       text not null check (url like 'https://%'),
    position  integer not null default 0
);
create index ix_course_resources_course on course_resources (course_id, position);

create table item_progress (
    enrollment_id uuid        not null references enrollments (id) on delete cascade,
    item_id       uuid        not null references content_items (id) on delete cascade,
    completed_at  timestamptz not null default now(),
    primary key (enrollment_id, item_id)
);
create index ix_item_progress_item on item_progress (item_id);

-- El contenido ya cargado entra en la misma estructura, sin duplicar tablas.
insert into content_items (module_id, source_type, source_id, title, youtube_id, position)
select module_id, 'VIDEO', id, title, youtube_id, position from videos;

insert into content_items (module_id, source_type, source_id, title, description, position)
select module_id, 'EJERCICIO', id, title, prompt, position from exercises;

create function sync_video_item() returns trigger as $$
begin
    if tg_op = 'DELETE' then
        delete from content_items where source_type = 'VIDEO' and source_id = old.id;
        return old;
    elsif tg_op = 'INSERT' then
        insert into content_items (module_id, source_type, source_id, title, youtube_id, position)
        values (new.module_id, 'VIDEO', new.id, new.title, new.youtube_id, new.position);
        return new;
    end if;
    update content_items
       set module_id = new.module_id,
           section_id = case when module_id = new.module_id then section_id else null end,
           title = new.title,
           youtube_id = new.youtube_id,
           position = new.position
     where source_type = 'VIDEO' and source_id = new.id;
    return new;
end;
$$ language plpgsql;

create function sync_exercise_item() returns trigger as $$
begin
    if tg_op = 'DELETE' then
        delete from content_items where source_type = 'EJERCICIO' and source_id = old.id;
        return old;
    elsif tg_op = 'INSERT' then
        insert into content_items (module_id, source_type, source_id, title, description, position)
        values (new.module_id, 'EJERCICIO', new.id, new.title, new.prompt, new.position);
        return new;
    end if;
    update content_items
       set module_id = new.module_id,
           section_id = case when module_id = new.module_id then section_id else null end,
           title = new.title,
           description = new.prompt,
           position = new.position
     where source_type = 'EJERCICIO' and source_id = new.id;
    return new;
end;
$$ language plpgsql;

create trigger trg_video_item after insert or update or delete on videos
    for each row execute function sync_video_item();
create trigger trg_exercise_item after insert or update or delete on exercises
    for each row execute function sync_exercise_item();

comment on table content_items is
    'Orden, sección y visibilidad de videos, materiales y ejercicios del curso.';
comment on table item_progress is
    'Avance de materiales y ejercicios; los videos siguen en lesson_progress.';
