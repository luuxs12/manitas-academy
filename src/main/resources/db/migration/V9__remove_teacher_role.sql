-- =====================================================================
-- V9: el perfil Docente se retira del producto.
--
-- Las cuentas que todavía tenían TEACHER pasan a STUDENT para no dejarlas
-- sin acceso al panel. La restricción de la tabla se reemplaza para que
-- tampoco pueda volver a cargarse ese rol por otra vía.
-- =====================================================================

update users
   set role = 'STUDENT'
 where role = 'TEACHER';

alter table users drop constraint users_role_check;

alter table users
    add constraint users_role_check
    check (role in ('STUDENT', 'ADMIN'));

comment on column users.role is 'Rol de la cuenta: STUDENT o ADMIN.';
