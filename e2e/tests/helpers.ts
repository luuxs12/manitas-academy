import { expect, type Page } from '@playwright/test';

export const CONTRASENA = '123456';

/** Usuarios sembrados por `DatosIniciales` en cualquier entorno de prueba. */
export const USUARIOS = {
  estudiante: 'alumno@mundomanitas.com',
  padre: 'padre@mundomanitas.com',
  docente: 'docente@mundomanitas.com',
  administrador: 'admin@mundomanitas.com',
  sistemas: 'sistemas@mundomanitas.com'
} as const;

export async function iniciarSesion(page: Page, email: string): Promise<void> {
  await page.goto('/login');
  await page.locator('#username').fill(email);
  await page.locator('#password').fill(CONTRASENA);
  await page.locator('button[type="submit"]').click();
  await expect(page).toHaveURL(/\/dashboard$/);
}

/** Falla si la página genera scroll horizontal. */
export async function sinDesbordamientoHorizontal(page: Page): Promise<void> {
  const desborda = await page.evaluate(
    () => document.documentElement.scrollWidth > window.innerWidth + 1
  );
  expect(desborda, 'la página desborda horizontalmente').toBe(false);
}
