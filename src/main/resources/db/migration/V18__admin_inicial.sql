-- =====================================================================
-- V18: cuenta administradora inicial
--
-- Deja lista la cuenta ADMIN de la plataforma desde el primer arranque.
-- Solo inserta si el correo no existe y nunca pisa la clave de una cuenta
-- ya creada. La contraseña se guarda como hash BCrypt, no en texto plano.
-- =====================================================================

insert into users (email, password_hash, full_name, role, active)
select 'admin@damianprint.com',
       '$2a$10$IPx7GXX2bOAnkkTEp1uZK.GVixfGu4TjdnJa60Rzei1Oxhw0oemkm',
       'Administrador',
       'ADMIN',
       true
where not exists (
    select 1 from users where lower(email) = 'admin@damianprint.com'
);

-- Si el correo ya existía con otro rol, se promueve sin tocar su clave.
update users
   set role = 'ADMIN',
       active = true
 where lower(email) = 'admin@damianprint.com'
   and (role <> 'ADMIN' or active = false);
