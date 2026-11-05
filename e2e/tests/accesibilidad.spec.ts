import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { iniciarSesion, USUARIOS } from './helpers';

const IMPACTOS_GRAVES = new Set(['serious', 'critical']);

async function violacionesGraves(page: Page) {
  const resultado = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze();

  return resultado.violations
    .filter((violacion) => IMPACTOS_GRAVES.has(violacion.impact ?? ''))
    .map((violacion) => ({
      id: violacion.id,
      impacto: violacion.impact,
      ayuda: violacion.help,
      nodos: violacion.nodes.map((nodo) => nodo.target.join(' '))
    }));
}

function detalle(violaciones: unknown): string {
  return JSON.stringify(violaciones, null, 2);
}

test.describe('Accesibilidad (WCAG 2.1 AA)', () => {
  test('la landing no tiene violaciones graves', async ({ page }) => {
    await page.goto('/');

    const violaciones = await violacionesGraves(page);
    expect(violaciones, detalle(violaciones)).toEqual([]);
  });

  test('el login no tiene violaciones graves', async ({ page }) => {
    await page.goto('/login');

    const violaciones = await violacionesGraves(page);
    expect(violaciones, detalle(violaciones)).toEqual([]);
  });

  test('el dashboard del estudiante no tiene violaciones graves', async ({ page }) => {
    await iniciarSesion(page, USUARIOS.estudiante);

    const violaciones = await violacionesGraves(page);
    expect(violaciones, detalle(violaciones)).toEqual([]);
  });

  test('la landing se puede recorrer con teclado', async ({ page }) => {
    await page.goto('/');

    await page.keyboard.press('Tab');
    const primerFoco = await page.evaluate(() => document.activeElement?.textContent?.trim() ?? '');

    expect(primerFoco).toMatch(/ir al contenido/i);
  });
});
