-- =====================================================================
-- V15: documentos descargables dentro del temario.
--
-- El archivo se guarda fuera de la base; acá queda su ruta interna y el
-- nombre original para mostrar. La ruta pública de imágenes no se toca.
-- =====================================================================

alter table content_items
    add column file_name text;

alter table content_items
    drop constraint if exists content_items_url_check;

alter table content_items
    add constraint content_items_url_check
    check (url is null
           or url like 'https://%'
           or url like '/uploads/materials/%');

comment on column content_items.file_name is
    'Nombre original del documento subido, para la descarga.';
