import { expect, test } from '@playwright/test';
import { iniciarSesion, USUARIOS } from './helpers';

test.describe('Autenticación y sesión', () => {
  test('un visitante que entra a /dashboard termina en el login', async ({ page }) => {
    await page.goto('/dashboard');

    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('heading', { name: /iniciar sesión/i })).toBeVisible();
  });

  test('el login con credenciales válidas abre el dashboard', async ({ page }) => {
    await iniciarSesion(page, USUARIOS.estudiante);

    await expect(page).toHaveTitle(/Dashboard/);
    await expect(page.getByRole('heading', { name: /hola/i })).toBeVisible();
  });

  test('el login con contraseña incorrecta muestra un error sin filtrar datos', async ({ page }) => {
    await page.goto('/login');
    await page.locator('#username').fill(USUARIOS.estudiante);
    await page.locator('#password').fill('clave-incorrecta');
    await page.locator('button[type="submit"]').click();

    await expect(page).toHaveURL(/\/login\?error/);
    await expect(page.locator('.alert-error')).toContainText(/correo o contraseña incorrectos/i);
  });

  test('con sesión activa, la landing redirige al dashboard', async ({ page }) => {
    await iniciarSesion(page, USUARIOS.estudiante);

    await page.goto('/');
    await expect(page).toHaveURL(/\/dashboard$/);

    await page.goto('/inicio');
    await expect(page).toHaveURL(/\/dashboard$/);
  });

  test('al cerrar sesión el dashboard vuelve a pedir autenticación', async ({ page, context }) => {
    await iniciarSesion(page, USUARIOS.estudiante);

    await context.clearCookies();
    await page.goto('/dashboard');

    await expect(page).toHaveURL(/\/login$/);
  });
});
