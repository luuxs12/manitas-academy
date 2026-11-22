-- La auditoría conserva la historia aunque se elimine una cuenta:
-- actor_id pasa a ser opcional y la base lo deja en NULL al borrar al usuario.

alter table audit_log alter column actor_id drop not null;

do $$
declare
    constraint_name text;
begin
    select c.conname into constraint_name
      from pg_constraint c
      join pg_attribute a on a.attrelid = c.conrelid and a.attnum = any(c.conkey)
     where c.conrelid = 'audit_log'::regclass
       and c.confrelid = 'users'::regclass
       and c.contype = 'f'
       and a.attname = 'actor_id'
     limit 1;

    if constraint_name is not null then
        execute format('alter table audit_log drop constraint %I', constraint_name);
    end if;
end $$;

alter table audit_log
    add constraint fk_audit_log_actor
    foreign key (actor_id) references users (id) on delete set null;
