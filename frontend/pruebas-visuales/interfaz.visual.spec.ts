import { test, expect } from '@playwright/test';

test.beforeEach(async ({ page: pagina }) => {
  await pagina.goto('/ingreso');
  await expect(pagina.getByRole('heading', { name: 'Tu próxima perspectiva.' })).toBeVisible();
  await pagina.evaluate(async () => {
    await document.fonts.ready;
  });
  await expect(pagina.locator('vite-error-overlay')).toHaveCount(0);
});

test('ingreso institucional', async ({ page: pagina }) => {
  await expect(pagina.getByLabel('Correo institucional')).toBeVisible();
  await expect(pagina.getByRole('button', { name: 'Entrar a mi espacio' })).toBeVisible();
  expect(await pagina.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
    pagina.viewportSize()!.width,
  );
  await expect(pagina).toHaveScreenshot('ingreso.png', { fullPage: true });
});

test('biblioteca y modal con datos ficticios', async ({ page: pagina }) => {
  await pagina.getByRole('button', { name: 'Explorar la demostración' }).click();
  await pagina.getByRole('button', { name: 'Biblioteca de casos', exact: true }).click();
  await expect(pagina.locator('.tarjeta-caso')).toHaveCount(3);
  await pagina.evaluate(async () => {
    await document.fonts.ready;
    // La referencia representa la vista inicial, no el desplazamiento heredado del login móvil.
    window.scrollTo(0, 0);
    (document.activeElement as HTMLElement | null)?.blur();
  });
  expect(await pagina.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
    pagina.viewportSize()!.width,
  );
  await expect(pagina).toHaveScreenshot('biblioteca.png', { fullPage: true });
  await pagina.getByRole('button', { name: 'Crear un caso' }).click();
  const ventana = pagina.getByRole('dialog');
  await expect(ventana).toBeVisible();
  await expect(pagina.getByLabel('Nombre del caso', { exact: true })).toBeVisible();
  const limites = await ventana.boundingBox();
  expect(limites!.x).toBeGreaterThanOrEqual(0);
  expect(limites!.x + limites!.width).toBeLessThanOrEqual(pagina.viewportSize()!.width);
  await expect(pagina).toHaveScreenshot('modal-caso.png');
  await pagina.keyboard.press('Escape');
  await expect(ventana).toHaveCount(0);
});
