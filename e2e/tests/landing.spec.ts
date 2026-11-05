import { expect, test } from '@playwright/test';
import { sinDesbordamientoHorizontal } from './helpers';

test.describe('Landing pública', () => {
  test('presenta el hero, el catálogo y las secciones principales', async ({ page }) => {
    await page.goto('/');

    await expect(page).toHaveTitle(/Mundo Manitas/);
    await expect(page.getByRole('heading', { level: 1 })).toContainText('una aventura');

    const cursos = page.locator('.lp-course');
    await expect(cursos.first()).toBeVisible();
    expect(await cursos.count()).toBeLessThanOrEqual(3);

    await expect(page.locator('#cursos')).toBeAttached();
    await expect(page.locator('#progreso')).toBeAttached();
    await expect(page.locator('#testimonios')).toBeAttached();
  });

  test('los CTA de inscripción llevan al registro de estudiante', async ({ page }) => {
    await page.goto('/');

    await page.getByRole('link', { name: /crear mi cuenta gratis/i }).first().click();

    await expect(page).toHaveURL(/\/registro\/estudiante$/);
    await expect(page.getByRole('heading', { level: 1 }).first()).toBeVisible();
  });

  test('la navegación por anclas lleva a cada sección', async ({ page }) => {
    await page.goto('/');

    await page.getByRole('link', { name: 'Progreso', exact: true }).click();

    await expect(page).toHaveURL(/#progreso$/);
    await expect(page.locator('#progreso')).toBeVisible();
  });

  test('no desborda horizontalmente en móvil, tablet y escritorio', async ({ page }) => {
    for (const ancho of [390, 768, 1280]) {
      await page.setViewportSize({ width: ancho, height: 900 });
      await page.goto('/');
      await sinDesbordamientoHorizontal(page);
    }
  });

  test('en móvil se oculta la navegación de escritorio', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto('/');

    await expect(page.locator('.lp-nav-links')).toBeHidden();
    await expect(page.getByRole('link', { name: /empezar gratis/i })).toBeVisible();
  });
});
