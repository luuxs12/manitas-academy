-- =====================================================================
-- V7: videos y subtítulos privados en R2.
--
-- Los videos nuevos se guardan por clave en un bucket sin acceso público y
-- se entregan con URLs prefirmadas. La URL histórica se conserva para los
-- videos que todavía no pudieron migrarse; el reproductor la usa como
-- respaldo sin exponer claves privadas.
-- =====================================================================

-- La URL deja de ser obligatoria: los videos privados se identifican por clave.
alter table videos
    alter column url drop not null;

alter table videos
    add column object_key       text,
    add column subtitle_key     text,
    add column duration_seconds integer     not null default 0,
    add column transcript       text;

alter table videos
    add constraint ck_videos_duration check (duration_seconds >= 0);

comment on column videos.object_key is
    'Clave del video en el bucket privado; vacía en los videos históricos.';
comment on column videos.subtitle_key is
    'Clave del único subtítulo VTT en español, opcional.';
comment on column videos.duration_seconds is
    'Duración declarada por el administrador, en segundos; 0 si se desconoce.';
comment on column videos.transcript is
    'Transcripción en texto plano del video, opcional.';

-- Recupera la ruta "videos/…" de las URLs públicas antiguas como clave.
update videos
   set object_key = ltrim(substring(url from '/videos/[A-Za-z0-9_./-]+$'), '/')
 where substring(url from '/videos/[A-Za-z0-9_./-]+$') is not null;

create unique index ux_videos_object_key
    on videos (object_key) where object_key is not null;
