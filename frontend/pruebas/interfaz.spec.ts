import { test, expect } from '@playwright/test';

test('biblioteca: búsqueda, filtros y navegación conservada al recargar', async ({
  page: pagina,
}) => {
  await pagina.goto('/');
  await pagina.getByRole('button', { name: 'Explorar la demostración' }).click();
  await expect(pagina.getByRole('heading', { name: /El aprendizaje empieza/ })).toBeVisible();
  await pagina.getByRole('button', { name: 'Biblioteca de casos', exact: true }).click();
  await pagina.getByRole('textbox', { name: 'Buscar casos por nombre' }).fill('pertenecer');
  await expect(pagina.locator('.tarjeta-caso')).toHaveCount(1);
  await pagina.getByRole('textbox', { name: 'Buscar casos por nombre' }).fill('');
  await pagina.getByRole('button', { name: 'Borrador', exact: true }).click();
  await expect(pagina.locator('.tarjeta-caso')).toHaveCount(1);
  await expect(
    pagina.getByRole('heading', { name: 'Más allá de la primera impresión' }),
  ).toBeVisible();
  await pagina.reload();
  await expect(pagina.getByRole('heading', { name: 'Biblioteca de casos.' })).toBeVisible();
});

test('creación de borrador y transición a publicado en demostración', async ({ page: pagina }) => {
  await pagina.goto('/');
  await pagina.getByRole('button', { name: 'Explorar la demostración' }).click();
  await pagina.getByRole('button', { name: 'Crear un caso' }).click();
  const campos = {
    'Nombre del caso': 'Decisiones compartidas',
    'Tema o contexto': 'Influencia social',
    'Desarrollo de la situación': 'Un grupo debe resolver un conflicto.',
    Título: 'La reunión',
    'Contexto del escenario': 'Cinco estudiantes deliberan.',
    Pregunta: '¿Qué facilita el diálogo?',
    'Opción correcta': 'Escuchar a todos',
    'Opción incorrecta': 'Imponer una decisión',
    'Justificación correcta': 'Permite considerar distintas perspectivas.',
    'Justificación incorrecta': 'Limita la participación.',
  };
  for (const [nombre, valor] of Object.entries(campos))
    await pagina.getByLabel(nombre, { exact: true }).fill(valor);
  await pagina.getByRole('button', { name: 'Guardar borrador' }).click();
  const tarjeta = pagina.locator('.tarjeta-caso').filter({ hasText: 'Decisiones compartidas' });
  await tarjeta.getByRole('button', { name: 'Explorar caso' }).click();
  await pagina.getByRole('button', { name: 'Publicar caso' }).click();
  await expect(pagina.getByRole('dialog').getByText('Publicado', { exact: true })).toBeVisible();
});

test('detalle de caso presenta el contenido y explica cada requisito de publicación pendiente', async ({
  page: pagina,
}) => {
  const caso = {
    id: 'caso-invalido',
    nombre: 'Caso por completar',
    contexto: 'Influencia social',
    desarrolloSituacional: 'Un grupo debe tomar una decisión bajo presión.',
    tiempoEstimadoMin: 30,
    estado: 'BORRADOR',
    escenarios: [
      {
        id: 'escenario-1',
        titulo: 'La reunión',
        contextoSituacional: 'El equipo escucha distintas propuestas.',
        orden: 1,
        preguntas: [],
      },
    ],
    rubrica: null,
  };
  await pagina.route('**/api/**', async (ruta) => {
    const direccion = new URL(ruta.request().url());
    if (direccion.pathname.endsWith('/identidad/ingreso'))
      return ruta.fulfill({ json: { token: 'token-ficticio', tipo: 'Bearer' } });
    if (direccion.pathname.endsWith('/identidad/yo'))
      return ruta.fulfill({
        json: {
          id: 'docente',
          nombre: 'Docente de prueba',
          rol: 'DOCENTE',
          correo: 'docente@ejemplo.edu',
          puedeCrearCasos: true,
        },
      });
    if (direccion.pathname.endsWith('/casos/caso-invalido/publicar'))
      return ruta.fulfill({
        status: 422,
        contentType: 'application/problem+json',
        json: {
          detail: 'El caso no cumple las condiciones de publicacion',
          errores: [
            {
              codigo: 'PREGUNTAS_REQUERIDAS',
              ruta: 'escenarios[0].preguntas',
              mensaje: 'El escenario debe contener preguntas',
            },
            {
              codigo: 'RUBRICA_REQUERIDA',
              ruta: 'rubrica',
              mensaje: 'Debe asociar una rubrica',
            },
          ],
        },
      });
    if (direccion.pathname.endsWith('/casos')) return ruta.fulfill({ json: [caso] });
    return ruta.fulfill({ json: [] });
  });

  await pagina.goto('/');
  await pagina.getByLabel('Correo institucional').fill('docente@ejemplo.edu');
  await pagina.getByLabel('Contraseña', { exact: true }).fill('clave-ficticia');
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await pagina.getByRole('button', { name: 'Biblioteca de casos', exact: true }).click();
  await expect(pagina.getByRole('heading', { name: 'Biblioteca de casos.' })).toBeVisible();
  const tarjeta = pagina.locator('.tarjeta-caso').filter({ hasText: 'Caso por completar' });
  await expect(tarjeta).toBeVisible();
  await tarjeta.getByRole('button', { name: 'Explorar caso' }).click();

  const modal = pagina.getByRole('dialog');
  await expect(modal.getByRole('heading', { name: 'Caso por completar' })).toBeVisible();
  await expect(modal.getByText('Escenarios y preguntas')).toBeVisible();
  await expect(modal.getByText('Rúbrica', { exact: true })).toBeVisible();
  await pagina.getByRole('button', { name: 'Publicar caso' }).click();

  const alerta = modal.getByRole('alert');
  await expect(
    alerta.getByRole('heading', { name: 'Este caso aún no está listo para publicarse' }),
  ).toBeVisible();
  await expect(alerta.getByText('Escenario 1 · Preguntas')).toBeVisible();
  await expect(alerta.getByText('El escenario debe contener preguntas')).toBeVisible();
  await expect(alerta.getByText('Rúbrica', { exact: true })).toBeVisible();
  await expect(alerta.getByText('Debe asociar una rubrica')).toBeVisible();
  await expect(alerta.getByRole('button', { name: 'Corregir el borrador' })).toBeVisible();
});

test('API: autenticación Bearer y separación de datos ficticios al crear un espacio', async ({
  page: pagina,
}) => {
  let autorizacion = '';
  let cuerpo: unknown;
  await pagina.route('**/api/**', async (ruta) => {
    const url = new URL(ruta.request().url());
    if (url.pathname.endsWith('/ingreso'))
      return ruta.fulfill({ json: { token: 'token-ficticio', tipo: 'Bearer' } });
    expect(ruta.request().headers()['authorization']).toBe('Bearer token-ficticio');
    if (url.pathname.endsWith('/yo'))
      return ruta.fulfill({
        json: {
          id: 'docente',
          nombre: 'Docente de prueba',
          rol: 'DOCENTE',
          correo: 'docente@ejemplo.edu',
          puedeCrearCasos: true,
        },
      });
    if (ruta.request().method() === 'POST') {
      autorizacion = ruta.request().headers()['authorization'];
      expect(ruta.request().headers()['x-csrf-token']).toBeUndefined();
      cuerpo = ruta.request().postDataJSON();
      return ruta.fulfill({ json: { id: 'espacio-real', ...(cuerpo as object) } });
    }
    return ruta.fulfill({ json: [] });
  });
  await pagina.goto('/');
  await pagina.getByLabel('Correo institucional').fill('docente@ejemplo.edu');
  await pagina.getByLabel('Contraseña', { exact: true }).fill('clave-ficticia');
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await expect(pagina.getByText('Conexión establecida.', { exact: false })).toBeVisible();
  await expect(pagina.getByRole('button', { name: 'El poder de pertenecer' })).toHaveCount(0);
  await pagina.getByRole('button', { name: 'Espacios académicos', exact: true }).click();
  await pagina.getByRole('button', { name: 'Nuevo espacio' }).click();
  await pagina.getByLabel('Nombre', { exact: true }).fill('Psicología comunitaria');
  await pagina.getByRole('button', { name: 'Crear espacio' }).click();
  await expect(pagina.getByRole('heading', { name: 'Psicología comunitaria' })).toBeVisible();
  expect(autorizacion).toBe('Bearer token-ficticio');
  expect(cuerpo).toEqual({ nombre: 'Psicología comunitaria', periodo: '2026-2' });
  expect(await pagina.evaluate(() => localStorage.length)).toBe(0);
});

test('error de acceso visible sin abandonar el login', async ({ page: pagina }) => {
  await pagina.route('**/api/identidad/ingreso', (ruta) => ruta.fulfill({ status: 401, json: {} }));
  await pagina.goto('/');
  await pagina.getByLabel('Correo institucional').fill('docente@ejemplo.edu');
  await pagina.getByLabel('Contraseña', { exact: true }).fill('incorrecta');
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await expect(pagina.getByRole('alert')).toHaveText('Correo o clave incorrectos.');
  await pagina.keyboard.press('Escape');
  await expect(pagina).toHaveURL(/\/ingreso$/);
  await expect(pagina.getByLabel('Contraseña', { exact: true })).toHaveValue('');
});

test('vista móvil sin desbordamiento horizontal', async ({ page: pagina }) => {
  await pagina.setViewportSize({ width: 390, height: 844 });
  await pagina.goto('/');
  await pagina.getByRole('button', { name: 'Explorar la demostración' }).click();
  expect(await pagina.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
    390,
  );
  await pagina.screenshot({ path: 'capturas/inicio-movil.png', fullPage: true });
  await pagina.setViewportSize({ width: 1440, height: 1050 });
  await pagina.screenshot({ path: 'capturas/inicio-escritorio.png', fullPage: true });
});

test('reporte: evaluación pendiente y ausencia de intentos sin inventar notas', async ({
  page: pagina,
}) => {
  let conIntento = true;
  await pagina.route('**/api/**', async (ruta) => {
    const direccion = new URL(ruta.request().url());
    if (direccion.pathname.endsWith('/ingreso'))
      return ruta.fulfill({ json: { token: 'token-ficticio', tipo: 'Bearer' } });
    if (direccion.pathname.endsWith('/yo'))
      return ruta.fulfill({
        json: {
          id: 'docente',
          nombre: 'Docente de prueba',
          rol: 'DOCENTE',
          correo: 'docente@ejemplo.edu',
          puedeCrearCasos: true,
        },
      });
    if (direccion.pathname.includes('/reportes/'))
      return ruta.fulfill({
        json: conIntento
          ? [
              {
                participacion: {
                  idIntento: 'intento-1',
                  estado: 'FINALIZADO',
                  inicio: '2026-11-15T15:00:00Z',
                  idSesion: 'sesion',
                },
                resultado: null,
                valoracionesGenerales: [],
              },
            ]
          : [],
      });
    return ruta.fulfill({ json: [] });
  });
  await pagina.goto('/');
  await pagina.getByLabel('Correo institucional').fill('docente@ejemplo.edu');
  await pagina.getByLabel('Contraseña', { exact: true }).fill('clave-ficticia');
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await pagina.getByRole('button', { name: 'Resultados', exact: true }).click();
  await pagina.getByLabel('Identificador del caso').fill('caso');
  await pagina.getByLabel('Identificador del estudiante').fill('estudiante');
  await pagina.getByRole('button', { name: 'Consultar resultados' }).click();
  await expect(pagina.getByText('Evaluación pendiente.', { exact: false })).toBeVisible();
  await expect(pagina.locator('.nota-calificacion')).toHaveCount(0);
  conIntento = false;
  await pagina.getByRole('button', { name: 'Consultar resultados' }).click();
  await expect(
    pagina.getByText('No hay intentos finalizados para este estudiante y caso.'),
  ).toBeVisible();
});
