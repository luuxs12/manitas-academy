-- V12: saco del catálogo los seis cursos sembrados en V3.
--
-- Los slugs se escriben uno por uno: nunca un borrado por patrón ni una
-- tabla completa. Matrículas, módulos, videos, ejercicios, avances y
-- certificados desaparecen por las claves foráneas en cascada.

delete from courses where slug in (
    'python-desde-cero',
    'desarrollo-web-html-css',
    'redes-y-soporte-tecnico',
    'ciberseguridad-basica',
    'bases-de-datos-sql',
    'mantenimiento-de-hardware'
);
