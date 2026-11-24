-- =====================================================================
-- V10: videos alojados en YouTube.
--
-- Los videos nuevos se identifican por el ID público de YouTube y se embeben
-- sin cookies. Las claves privadas y la URL histórica permanecen intactas para
-- no perder los videos ya cargados en R2.
-- =====================================================================

alter table videos
    add column youtube_id varchar(32);

alter table videos
    add constraint ck_videos_youtube_id
    check (youtube_id is null or youtube_id ~ '^[A-Za-z0-9_-]{11}$');

comment on column videos.youtube_id is
    'ID del video de YouTube; vacío en los videos históricos alojados en R2.';
