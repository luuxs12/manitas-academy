import { expect, test } from '@playwright/test';
import { iniciarSesion, USUARIOS } from './helpers';

test.describe('Autorización por rol', () => {
  test('el padre no puede entrar al catálogo de cursos', async ({ page }) => {
    await iniciarSesion(page, USUARIOS.padre);

    const respuesta = await page.goto('/cursos');

    expect(respuesta?.status()).toBe(403);
    await expect(page).toHaveTitle(/403/);
    await expect(page.getByRole('heading', { name: /acceso denegado/i })).toBeVisible();
  });

  test('el administrador entra a la gestión de usuarios', async ({ page }) => {
    await iniciarSesion(page, USUARIOS.administrador);

    const respuesta = await page.goto('/admin/usuarios');

    expect(respuesta?.status()).toBe(200);
    await expect(page).toHaveTitle(/Usuarios/);
    await expect(page.getByRole('heading', { name: /usuarios de la plataforma/i })).toBeVisible();
  });

  test('el docente no puede administrar usuarios', async ({ page }) => {
    await iniciarSesion(page, USUARIOS.docente);

    const respuesta = await page.goto('/admin/usuarios');

    expect(respuesta?.status()).toBe(403);
  });

  test('el estudiante no accede al panel administrativo', async ({ page }) => {
    await iniciarSesion(page, USUARIOS.estudiante);

    const respuesta = await page.goto('/admin');

    expect(respuesta?.status()).toBe(403);
  });

  test('el docente solo ve sus propios cursos', async ({ page }) => {
    await iniciarSesion(page, USUARIOS.docente);

    const respuesta = await page.goto('/docente/cursos');

    expect(respuesta?.status()).toBe(200);
    await expect(page).toHaveTitle(/Docente|Cursos/i);
  });
});
