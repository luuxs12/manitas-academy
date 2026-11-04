<!--
Gracias por contribuir a Mundo Manitas.
Marca la Definition of Done antes de pedir revisión.
Referencia: docs/PLAN_DE_PRUEBAS.md §9
-->

## Qué cambia

<!-- Resumen en 2-3 líneas. Enlaza la historia o el issue: Closes #123 -->

## Tipo de cambio

- [ ] Nueva funcionalidad
- [ ] Corrección de defecto
- [ ] Refactor sin cambio de comportamiento
- [ ] Documentación
- [ ] Infraestructura / CI

## Cómo probarlo

<!-- Pasos concretos para que otra persona reproduzca la verificación. -->

1.
2.
3.

## Evidencia

<!-- Capturas, video, salida de la suite o del smoke manual. -->

## Definition of Done

- [ ] Criterios de aceptación verificados.
- [ ] Pruebas nuevas o actualizadas incluidas en este PR.
- [ ] `./mvnw verify` en verde (81+ pruebas, 0 fallos).
- [ ] Sin regresiones en la matriz de roles (`docs/CHECKLIST_PRUEBAS_MANUALES.md` §3).
- [ ] Revisión de accesibilidad si toca interfaz (teclado, foco, contraste, `alt`).
- [ ] Responsive verificado en 390 px, 768 px y escritorio.
- [ ] Migración Flyway incluida si cambia el esquema.
- [ ] Sin secretos ni credenciales en el diff.
- [ ] Documentación o README actualizado si cambia el comportamiento.

## Riesgos y reversión

<!-- ¿Qué podría romperse? ¿Cómo se revierte? -->
