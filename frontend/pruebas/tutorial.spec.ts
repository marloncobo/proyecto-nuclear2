import { expect, test } from '@playwright/test';
import { ingresar, simularApi } from './soporte/identidad';

test('el tutorial se puede saltar, desactivar para futuros ingresos y abrir manualmente', async ({
  page: pagina,
}) => {
  await simularApi(pagina, 'DOCENTE');
  await ingresar(pagina, 'docente@example.test');
  const tutorial = pagina.getByRole('dialog', { name: 'Tutorial guiado' });
  const preferencia = tutorial.getByRole('checkbox', { name: 'No volver a mostrar automáticamente' });
  await expect(tutorial.getByRole('heading', { name: 'Menú docente' })).toBeVisible();
  await tutorial.getByRole('button', { name: 'Siguiente' }).click();
  await expect(tutorial.getByRole('heading', { name: 'Crear un caso' })).toBeVisible();
  await tutorial.getByRole('button', { name: 'Saltar' }).click();
  await expect(tutorial).toHaveCount(0);

  await pagina.getByRole('button', { name: 'Cerrar sesión' }).click();
  await ingresar(pagina, 'docente@example.test');
  await expect(tutorial).toBeVisible();
  await preferencia.check();
  await tutorial.getByRole('button', { name: 'Saltar' }).click();

  await pagina.getByRole('button', { name: 'Cerrar sesión' }).click();
  await ingresar(pagina, 'docente@example.test');
  await expect(pagina.getByRole('button', { name: 'Sesiones', exact: true })).toBeVisible();
  await expect(tutorial).toHaveCount(0);
  await pagina.getByRole('button', { name: 'Sesiones', exact: true }).click();
  await pagina.getByRole('button', { name: 'Tutorial' }).click();
  await expect(tutorial.getByRole('heading', { name: 'Programar sesión' })).toBeVisible();
  await expect(preferencia).toBeChecked();
  await pagina.keyboard.press('Escape');
  await expect(tutorial).toHaveCount(0);
  await expect(pagina.getByRole('button', { name: 'Tutorial' })).toBeFocused();
});

test('los estudiantes ven su recorrido y los formularios del docente tienen ayuda propia', async ({
  page: pagina,
}) => {
  const estado = await simularApi(pagina, 'ESTUDIANTE');
  await ingresar(pagina);
  const tutorial = pagina.getByRole('dialog', { name: 'Tutorial guiado' });
  await expect(tutorial.getByRole('heading', { name: 'Menú estudiantil' })).toBeVisible();
  await tutorial.getByRole('button', { name: 'Saltar' }).click();
  await pagina.getByRole('button', { name: 'Cerrar sesión' }).click();

  estado.rol = 'DOCENTE';
  await ingresar(pagina);
  await tutorial.getByRole('button', { name: 'Saltar' }).click();
  await pagina.getByRole('button', { name: 'Crear un caso' }).click();
  const editor = pagina.getByRole('dialog', { name: 'Crea una experiencia.' });
  await editor.getByRole('button', { name: 'Tutorial' }).click();
  await expect(tutorial.getByRole('heading', { name: 'Datos del caso' })).toBeVisible();
  await tutorial.getByRole('button', { name: 'Siguiente' }).click();
  await expect(tutorial.getByRole('heading', { name: 'Escenarios y preguntas' })).toBeVisible();
  await pagina.keyboard.press('Escape');
  await expect(tutorial).toHaveCount(0);
  await expect(editor).toBeVisible();
});
