import { test, expect } from './soporte/sin-tutorial-automatico';

test('docente: filtros por datos reconocibles y navegación conservada al recargar', async ({
  page: pagina,
}) => {
  await pagina.goto('/');
  await pagina.getByRole('button', { name: 'Explorar la demostración' }).click();
  await expect(pagina.getByRole('heading', { name: /El aprendizaje empieza/ })).toBeVisible();
  await pagina.getByRole('button', { name: 'Biblioteca de casos', exact: true }).click();
  await pagina.getByRole('textbox', { name: 'Buscar casos por nombre o tema' }).fill('pertenecer');
  await expect(pagina.locator('.tarjeta-caso')).toHaveCount(1);
  await pagina.getByRole('textbox', { name: 'Buscar casos por nombre o tema' }).fill('');
  await pagina.getByRole('button', { name: 'Borrador', exact: true }).click();
  await expect(pagina.locator('.tarjeta-caso')).toHaveCount(1);
  await expect(
    pagina.getByRole('heading', { name: 'Más allá de la primera impresión' }),
  ).toBeVisible();
  await pagina.getByRole('button', { name: 'Sesiones', exact: true }).click();
  await pagina.getByLabel('Buscar sesiones').fill('demo-sesion');
  await expect(pagina.locator('.tarjeta-sesion')).toHaveCount(0);
  await pagina.getByLabel('Buscar sesiones').fill('Aula 302');
  await expect(pagina.locator('.tarjeta-sesion')).toHaveCount(1);
  await pagina.getByRole('button', { name: 'Biblioteca de casos', exact: true }).click();
  await pagina.reload();
  await expect(pagina.getByRole('heading', { name: 'Biblioteca de casos.' })).toBeVisible();
});

test('creación de borrador y transición a publicado en demostración', async ({ page: pagina }) => {
  await pagina.goto('/');
  await pagina.getByRole('button', { name: 'Explorar la demostración' }).click();
  await pagina.getByRole('button', { name: 'Crear un caso' }).click();
  const modal = pagina.getByRole('dialog');
  const nombreCaso = modal.getByLabel('Nombre del caso', { exact: true });
  const reiniciar = modal.getByRole('button', { name: 'Empezar de cero' });
  await nombreCaso.fill('Borrador que se va a borrar');
  await modal.getByRole('button', { name: 'Agregar escenario' }).click();
  pagina.once('dialog', (confirmacion) => confirmacion.dismiss());
  await reiniciar.click();
  await expect(nombreCaso).toHaveValue('Borrador que se va a borrar');
  pagina.once('dialog', (confirmacion) => confirmacion.accept());
  await reiniciar.click();
  await expect(nombreCaso).toHaveValue('');
  await expect(modal.locator('.escenario')).toHaveCount(1);
  await pagina.reload();
  await pagina.getByRole('button', { name: 'Crear un caso' }).click();
  await expect(nombreCaso).toHaveValue('');
  const titulo = modal.getByLabel('Título', { exact: true });
  const encabezadoEscenario = modal.locator('.escenario > summary');
  await expect(titulo).toBeHidden();
  await expect(encabezadoEscenario).toHaveText('Escenario 1');
  await encabezadoEscenario.press('Enter');
  await expect(titulo).toBeVisible();

  const total = modal.getByRole('status');
  const ponderacion = modal.getByLabel('Ponderación', { exact: true });
  const encabezadoCriterio = modal.locator('.criterio > summary');
  await expect(encabezadoCriterio).toHaveText('Análisis de la situación');
  await expect(ponderacion).toBeHidden();
  await encabezadoCriterio.press('Enter');
  await expect(total).toContainText('1.00');
  await expect(total).toHaveClass(/completa/);
  await ponderacion.fill('0.25');
  await expect(total).toContainText('0.25');
  await expect(total).not.toHaveClass(/completa/);
  await ponderacion.fill('1');

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
  const preguntaDelCriterio = modal.getByRole('checkbox', { name: campos.Pregunta });
  await expect(preguntaDelCriterio).toBeChecked();
  await preguntaDelCriterio.uncheck();
  await expect(encabezadoEscenario).toHaveText('La reunión');
  await encabezadoEscenario.click();
  await expect(titulo).toBeHidden();
  await pagina.keyboard.press('Escape');
  await expect(modal).toHaveCount(0);
  await pagina.getByRole('button', { name: 'Crear un caso' }).click();
  await expect(modal.getByLabel('Nombre del caso')).toHaveValue(campos['Nombre del caso']);
  await encabezadoCriterio.click();
  await expect(preguntaDelCriterio).not.toBeChecked();
  await preguntaDelCriterio.press('Space');
  await expect(preguntaDelCriterio).toBeChecked();
  await pagina.reload();
  await pagina.getByRole('button', { name: 'Crear un caso' }).click();
  await expect(encabezadoEscenario).toHaveText('La reunión');
  await expect(titulo).toBeHidden();
  await expect(ponderacion).toBeHidden();
  await encabezadoCriterio.click();
  await expect(preguntaDelCriterio).toBeChecked();
  await encabezadoCriterio.click();
  await pagina.getByRole('button', { name: 'Guardar borrador' }).click();
  await expect(modal).toHaveCount(0);
  await pagina.getByRole('button', { name: 'Crear un caso' }).click();
  await expect(modal.getByLabel('Nombre del caso')).toHaveValue('');
  await pagina.keyboard.press('Escape');
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
  expect(cuerpo).toEqual({ nombre: 'Psicología comunitaria', periodo: '2026-2', estudiantes: [] });
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

test('reporte: Excel conserva notas, avance y ausencias como datos editables', async ({
  page: pagina,
}, prueba) => {
  let sinParticipaciones = false;
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
    if (direccion.pathname.endsWith('/sesiones'))
      return ruta.fulfill({
        json: [
          {
            id: 'sesion',
            idCaso: 'caso',
            idEspacio: 'espacio',
            inicioPrevisto: '2026-11-15T15:00:00Z',
            finPrevisto: '2026-11-15T16:00:00Z',
            duracionMaximaMin: 30,
            estado: 'FINALIZADO',
            modoNavegacion: 'FLEXIBLE',
            lugar: '',
            mensaje: '',
            convocatorias: [],
          },
        ],
      });
    if (direccion.pathname === '/api/casos')
      return ruta.fulfill({ json: [{ id: 'caso', nombre: 'Análisis de grupo' }] });
    if (direccion.pathname === '/api/academico/espacios')
      return ruta.fulfill({ json: [{ id: 'espacio', nombre: 'Psicología social', periodo: '2026-2' }] });
    if (direccion.pathname.endsWith('/reportes/sesiones/sesion'))
      return ruta.fulfill({
        json: sinParticipaciones ? [] : [
          {
            idEstudiante: 'estudiante',
            nombreEstudiante: 'Ana Aprendiz',
            correoEstudiante: 'ana@ejemplo.edu',
            idIntento: 'intento-1',
            estado: 'FINALIZADO',
            calificacion: null,
            preguntasRespondidas: 1,
            preguntasTotales: 2,
            ausencia: null,
          },
          {
            idEstudiante: 'ausente',
            nombreEstudiante: 'Beto Ausente',
            correoEstudiante: 'beto@ejemplo.edu',
            idIntento: null,
            estado: 'AUSENTE',
            calificacion: 0,
            ausencia: {
              fechaConsolidacion: '2026-11-15T16:00:00Z',
              detalles: [{ idPregunta: 'pregunta', puntos: 0, valoracion: 'Ausencia' }],
            },
          },
        ],
      });
    if (direccion.pathname.endsWith('/reportes/casos/caso/estudiantes/estudiante'))
      return ruta.fulfill({
        json: [
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
        ],
      });
    return ruta.fulfill({ json: [] });
  });
  await pagina.goto('/');
  await pagina.getByLabel('Correo institucional').fill('docente@ejemplo.edu');
  await pagina.getByLabel('Contraseña', { exact: true }).fill('clave-ficticia');
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await pagina.getByRole('button', { name: 'Resultados', exact: true }).click();
  await pagina.getByLabel('Sesión para exportar').selectOption('sesion');
  await pagina.getByRole('button', { name: 'Preparar reporte' }).click();
  await expect(pagina.getByText('Ana Aprendiz', { exact: true })).toBeVisible();
  await expect(pagina.getByText('Ausencia consolidada · 1 pregunta con 0 puntos.')).toBeVisible();
  await expect(pagina.getByText('1 de 2 preguntas respondidas')).toBeVisible();
  await pagina.getByLabel('Buscar estudiante').fill('Beto');
  await expect(pagina.getByText('Beto Ausente', { exact: true })).toBeVisible();
  await expect(pagina.getByText('Ana Aprendiz', { exact: true })).toHaveCount(0);
  await pagina.getByLabel('Filtrar por estado').selectOption('AUSENTE');
  await expect(pagina.locator('.fila-reporte-sesion')).toHaveCount(1);
  const { Workbook } = (await import('exceljs')).default;
  const descargaPendiente = pagina.waitForEvent('download');
  await pagina.getByRole('button', { name: 'Descargar Excel' }).click();
  const descarga = await descargaPendiente;
  expect(descarga.suggestedFilename()).toBe('participacion-analisis-de-grupo-sesion.xlsx');
  const archivo = prueba.outputPath('participacion.xlsx');
  await descarga.saveAs(archivo);
  const libro = new Workbook();
  await libro.xlsx.readFile(archivo);
  const hoja = libro.getWorksheet('Participación')!;
  expect(hoja.getCell('A1').value).toBe('Reporte de participación');
  expect(hoja.getCell('B2').value).toBe('Análisis de grupo');
  expect(hoja.getCell('B3').value).toBe('Psicología social · 2026-2');
  expect(hoja.getCell('B4').text).toContain('10:00');
  expect(libro.views).toMatchObject([{ activeTab: 0, visibility: 'visible' }]);
  expect(hoja.views).toMatchObject([{ state: 'normal', activeCell: 'A1' }]);
  expect(hoja.views[0]).not.toHaveProperty('ySplit');
  expect(hoja.autoFilter).toBe('A7:I9');
  expect(hoja.getCell('E7').value).toBe('Preguntas respondidas');
  expect(hoja.getCell('A8').value).toBe('Ana Aprendiz');
  expect(hoja.getCell('D8').value).toBeNull();
  expect(hoja.getCell('E8').value).toBe(1);
  expect(hoja.getCell('F8').value).toBe(2);
  expect(hoja.getCell('I8').value).toBe('intento-1');
  expect(hoja.getCell('C9').value).toBe('Ausente');
  expect(hoja.getCell('D9').value).toBe(0);
  expect(hoja.getCell('D9').numFmt).toBe('0.00');
  expect(hoja.getCell('G9').value).toBe(1);
  expect(hoja.getCell('I9').value).toBeNull();

  await pagina.getByRole('button', { name: 'Limpiar filtros' }).click();
  await pagina.getByRole('button', { name: 'Ver resultados de Ana Aprendiz' }).click();
  await expect(pagina.getByText('Evaluación pendiente.', { exact: false })).toBeVisible();
  await expect(pagina.locator('.intento-reporte .nota-calificacion')).toHaveCount(0);
  await expect(
    pagina.getByRole('heading', { name: 'Recorrido académico · Ana Aprendiz' }),
  ).toBeVisible();

  sinParticipaciones = true;
  await pagina.getByRole('button', { name: 'Preparar reporte' }).click();
  await expect(pagina.getByText('La sesión todavía no tiene participaciones para mostrar.')).toBeVisible();
  const descargaVaciaPendiente = pagina.waitForEvent('download');
  await pagina.getByRole('button', { name: 'Descargar Excel' }).click();
  const vacio = new Workbook();
  await vacio.xlsx.readFile(await (await descargaVaciaPendiente).path());
  expect(vacio.getWorksheet('Participación')?.getCell('A8').value)
    .toBe('La sesión todavía no tiene participaciones para mostrar.');
});
