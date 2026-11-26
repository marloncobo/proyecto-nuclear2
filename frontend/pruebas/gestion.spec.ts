import { test, expect } from './soporte/sin-tutorial-automatico';
import { simularApi, ingresar } from './soporte/identidad';

test('docente: consulta las cinco notificaciones priorizadas y marca su lectura', async ({
  page: pagina,
}) => {
  const estado = await simularApi(pagina, 'DOCENTE');
  const tipos = [
    ['recordatorio', 'RECORDATORIO_SESION', 'La sesión de Conflictos Sociales comenzará mañana.'],
    ['finalizacion', 'FINALIZACION_SESION', 'La sesión de Influencia Grupal ha finalizado.'],
    ['pendientes', 'ESTUDIANTES_PENDIENTES', '5 de 25 estudiantes no completaron la sesión.'],
    [
      'evaluaciones',
      'EVALUACIONES_DISPONIBLES',
      'Los resultados de la sesión ya están disponibles.',
    ],
    ['reporte', 'REPORTE_DISPONIBLE', 'El reporte de desempeño está disponible.'],
  ];
  estado.notificaciones.push(
    ...tipos.map(([id, tipo, mensaje], indice) => ({
      id,
      idSesion: 'sesion-' + indice,
      idIntento: null,
      tipo,
      mensaje,
      fechaCreacion: `2026-11-21T${15 + indice}:00:00Z`,
      leida: false,
    })),
  );
  const erroresNavegador: string[] = [];
  pagina.on('console', (mensaje) => {
    if (mensaje.type() === 'error') erroresNavegador.push(mensaje.text());
  });
  pagina.on('pageerror', (error) => erroresNavegador.push(error.message));

  await ingresar(pagina, 'docente@example.test');
  await pagina.getByRole('button', { name: /Notificaciones/ }).click();

  await expect(pagina.getByRole('heading', { name: 'Notificaciones docentes.' })).toBeVisible();
  await expect(pagina.getByRole('heading', { name: '5 notificaciones nuevas' })).toBeVisible();
  for (const titulo of [
    'Recordatorio de sesión próxima',
    'Finalización de sesión',
    'Estudiantes que no completaron la actividad',
    'Evaluaciones disponibles',
    'Reporte disponible',
  ])
    await expect(pagina.getByRole('heading', { name: titulo })).toBeVisible();

  await pagina.getByRole('button', { name: 'Marcar como leída' }).first().click();
  await expect(pagina.getByRole('heading', { name: '4 notificaciones nuevas' })).toBeVisible();
  expect(estado.solicitudes).toContainEqual(
    expect.objectContaining({
      ruta: '/api/notificaciones/recordatorio/leida',
      metodo: 'PUT',
    }),
  );
  await pagina.getByLabel('Filtrar por lectura').selectOption('LEIDA');
  await expect(pagina.locator('.lista-notificaciones article')).toHaveCount(1);
  await expect(
    pagina.getByRole('heading', { name: 'Recordatorio de sesión próxima' }),
  ).toBeVisible();
  await pagina.getByRole('button', { name: 'Limpiar filtros' }).click();
  await pagina.getByLabel('Filtrar por tipo').selectOption('REPORTE_DISPONIBLE');
  await expect(pagina.locator('.lista-notificaciones article')).toHaveCount(1);
  await expect(pagina.getByRole('heading', { name: 'Reporte disponible' })).toBeVisible();
  await pagina.getByRole('button', { name: 'Marcar todo como leído' }).click();
  await expect(pagina.getByRole('heading', { name: 'Sin notificaciones nuevas' })).toBeVisible();
  await pagina.getByRole('button', { name: 'Limpiar filtros' }).click();

  await pagina.setViewportSize({ width: 390, height: 844 });
  expect(await pagina.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
    390,
  );
  await expect(pagina.getByRole('heading', { name: 'Notificaciones docentes.' })).toBeVisible();
  expect(erroresNavegador).toEqual([]);
});

test('estudiante: filtra convocatorias y conserva los avisos leídos', async ({ page: pagina }) => {
  const estado = await simularApi(pagina, 'ESTUDIANTE');
  const apertura = 'Apertura programada: 30/11/2026 a las 10:00 (hora de Colombia).';
  estado.notificaciones.push(
    {
      id: 'convocatoria',
      idSesion: 'sesion-1',
      idIntento: null,
      tipo: 'CONVOCATORIA',
      mensaje: apertura,
      fechaCreacion: '2026-11-25T15:00:00Z',
      leida: false,
    },
    {
      id: 'resultado',
      idSesion: 'sesion-2',
      idIntento: 'intento-1',
      tipo: 'RESULTADO',
      mensaje: 'Tu calificación ya está disponible.',
      fechaCreacion: '2026-11-20T15:00:00Z',
      leida: true,
    },
  );
  await ingresar(pagina, 'alumna@example.test');
  await pagina.getByRole('navigation', { name: 'Secciones del estudiante' })
    .getByRole('button', { name: /Notificaciones/ }).click();
  const panel = pagina.locator('psico-panel-notificaciones');
  await expect(panel.getByRole('heading', { name: '1 notificación nueva' })).toBeVisible();
  await panel.getByLabel('Filtrar por tipo').selectOption('CONVOCATORIA');
  await expect(panel.locator('article')).toHaveCount(1);
  await expect(panel.getByText(apertura)).toBeVisible();
  await panel.getByRole('button', { name: 'Marcar todo como leído' }).click();
  await expect(panel.getByText('Leída', { exact: true })).toBeVisible();
  await panel.getByRole('button', { name: 'Limpiar filtros' }).click();
  await panel.getByLabel('Filtrar por lectura').selectOption('LEIDA');
  await expect(panel.locator('article')).toHaveCount(2);
  for (const [width, height] of [[1440, 1000], [768, 1024], [390, 844]]) {
    await pagina.setViewportSize({ width, height });
    expect(await panel.evaluate((elemento) => elemento.scrollWidth)).toBeLessThanOrEqual(width);
    expect(await pagina.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(width);
  }
});

test('administración: roles, filtros, detalle, edición y errores sin perder el formulario', async ({
  page: pagina,
}) => {
  const estado = await simularApi(pagina);
  await ingresar(pagina);
  await expect(pagina.getByRole('heading', { name: 'Administración de usuarios.' })).toBeVisible();
  await expect(
    pagina.getByRole('button', { name: 'Biblioteca de casos', exact: true }),
  ).toHaveCount(0);
  await expect(
    pagina.getByRole('button', { name: 'Desactivar Administradora', exact: true }),
  ).toBeDisabled();
  await pagina.getByLabel('Filtrar por rol').selectOption('DOCENTE');
  await pagina.getByLabel('Filtrar por estado').selectOption('true');
  await pagina.getByRole('button', { name: 'Aplicar filtros' }).click();
  await expect(pagina.locator('.cuenta')).toHaveCount(1);
  expect(
    estado.solicitudes.some((solicitud) => solicitud.ruta.includes('rol=DOCENTE&activo=true')),
  ).toBeTruthy();
  await pagina.getByRole('button', { name: 'Ver detalle de Docente original' }).click();
  await expect(pagina.getByRole('dialog', { name: 'Detalle de Docente original' })).toBeVisible();
  await expect(pagina.getByRole('heading', { name: 'Detalle de Docente original' })).toBeFocused();
  await pagina.getByRole('button', { name: 'Editar docente', exact: true }).click();
  await pagina.getByLabel('Nombre del docente').fill('Docente actualizado');
  await pagina.getByLabel('Correo del docente').fill('actualizado@example.test');
  estado.conflictoDocente = true;
  await pagina.getByRole('button', { name: 'Guardar docente' }).click();
  await expect(pagina.getByRole('alert')).toHaveText('El correo ya está registrado');
  await expect(pagina.getByLabel('Nombre del docente')).toHaveValue('Docente actualizado');
  estado.conflictoDocente = false;
  await pagina.getByRole('button', { name: 'Guardar docente' }).click();
  await expect(
    pagina.getByRole('heading', { name: 'Detalle de Docente actualizado' }),
  ).toBeVisible();
  const escritura = estado.solicitudes.filter((solicitud) => solicitud.metodo === 'PUT').at(-1)!;
  expect(escritura.autorizacion).toBe('Bearer token-ficticio');
  expect(escritura.datos).toEqual({
    nombre: 'Docente actualizado',
    correo: 'actualizado@example.test',
  });
  expect(
    estado.solicitudes.some((solicitud) => solicitud.ruta.includes('/academico/')),
  ).toBeFalsy();
  expect(
    estado.solicitudes.some(
      (solicitud) => solicitud.metodo === 'PUT' && solicitud.ruta.includes('/creacion-casos'),
    ),
  ).toBeFalsy();
  await pagina.getByRole('button', { name: 'Cerrar detalle de usuario' }).click();
  await expect(pagina.getByRole('dialog')).toHaveCount(0);
  await expect(
    pagina.getByRole('button', { name: 'Ver detalle de Docente actualizado' }),
  ).toBeFocused();
});

test('detalle de usuario: ventana modal con teclado, cierre y borrador descartable', async ({
  page: pagina,
}) => {
  await simularApi(pagina);
  await ingresar(pagina);
  const abrir = pagina.getByRole('button', { name: 'Ver detalle de Docente original' });
  await abrir.click();
  const ventana = pagina.getByRole('dialog', { name: 'Detalle de Docente original' });
  await expect(ventana).toBeVisible();
  expect(
    await ventana.evaluate((elemento) => (elemento as HTMLDialogElement).matches(':modal')),
  ).toBe(true);
  await expect(ventana.getByRole('button', { name: 'Volver al listado' })).toHaveCount(0);
  const cambiarRol = ventana.getByRole('button', { name: 'Cambiar rol' });
  await cambiarRol.focus();
  await pagina.keyboard.press('Tab');
  await expect(pagina.getByRole('button', { name: 'Cerrar detalle de usuario' })).toBeFocused();
  await pagina.keyboard.press('Shift+Tab');
  await expect(cambiarRol).toBeFocused();
  await pagina.getByRole('button', { name: 'Editar docente', exact: true }).click();
  await pagina.getByLabel('Nombre del docente').fill('Cambio sin guardar');
  await pagina.keyboard.press('Escape');
  await expect(ventana).toHaveCount(0);
  await expect(abrir).toBeFocused();
  await abrir.click();
  await pagina.getByRole('button', { name: 'Editar docente', exact: true }).click();
  await expect(pagina.getByLabel('Nombre del docente')).toHaveValue('Docente original');
  await pagina.getByRole('button', { name: 'Cerrar detalle de usuario' }).click();
  await expect(pagina.getByRole('dialog')).toHaveCount(0);
  await abrir.click();
  await expect(ventana).toBeVisible();
  await pagina.mouse.click(4, 4);
  await expect(pagina.getByRole('dialog')).toHaveCount(0);
  await expect(abrir).toBeFocused();
});

test('administración: confirmación de estado y reactivación', async ({ page: pagina }) => {
  const estado = await simularApi(pagina);
  await ingresar(pagina);
  await pagina.getByRole('button', { name: 'Desactivar Docente original', exact: true }).click();
  await pagina.getByRole('button', { name: 'Cancelar cambio de estado' }).click();
  expect(estado.solicitudes.filter((solicitud) => solicitud.metodo === 'PUT')).toHaveLength(0);
  await pagina.getByRole('button', { name: 'Desactivar Docente original', exact: true }).click();
  await pagina.getByRole('button', { name: 'Confirmar desactivación' }).click();
  await expect(
    pagina.getByRole('button', { name: 'Reactivar Docente original', exact: true }),
  ).toBeVisible();
  await pagina.getByRole('button', { name: 'Reactivar Docente original', exact: true }).click();
  await pagina.getByRole('button', { name: 'Confirmar reactivación' }).click();
  await expect(
    pagina.getByRole('button', { name: 'Desactivar Docente original', exact: true }),
  ).toBeVisible();
  expect(
    estado.solicitudes
      .filter((solicitud) => solicitud.metodo === 'PUT')
      .every((solicitud) => solicitud.autorizacion === 'Bearer token-ficticio'),
  ).toBeTruthy();
  await pagina.getByRole('button', { name: 'Cerrar sesión', exact: true }).click();
  await expect(pagina.getByRole('heading', { name: 'Administración de usuarios.' })).toHaveCount(0);
  expect(await pagina.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
});

test('administración: concede y retira el permiso de creación de un solo docente', async ({
  page: pagina,
}) => {
  const estado = await simularApi(pagina);
  const docente = estado.usuarios.find((usuario) => usuario.rol === 'DOCENTE')!;
  const otro = { ...docente, id: 'otro-docente', nombre: 'Otro docente', correo: 'otro@example.test' };
  estado.usuarios.push(otro);
  await ingresar(pagina);
  await pagina.getByRole('button', { name: 'Ver detalle de Docente original' }).click();
  const detalle = pagina.getByRole('dialog');
  const confirmar = detalle.getByRole('button', { name: 'Confirmar permiso' });
  await detalle.getByRole('button', { name: 'Retirar permiso de creación' }).click();
  await expect(confirmar).toBeFocused();
  await confirmar.press('Enter');
  const conceder = detalle.getByRole('button', { name: 'Conceder permiso de creación' });
  await expect(conceder).toBeVisible();
  expect(docente.puedeCrearCasos).toBe(false);
  expect(otro.puedeCrearCasos).toBe(true);
  await conceder.click();
  await confirmar.click();
  await expect(detalle.getByRole('button', { name: 'Retirar permiso de creación' })).toBeVisible();
  expect(docente.puedeCrearCasos).toBe(true);
  expect(estado.solicitudes.filter((solicitud) => solicitud.ruta.endsWith('/creacion-casos')).map(
    ({ ruta, datos }) => [ruta, datos.habilitado],
  )).toEqual([
    ['/api/identidad/usuarios/docente/creacion-casos', false],
    ['/api/identidad/usuarios/docente/creacion-casos', true],
  ]);

  await detalle.getByRole('button', { name: 'Cerrar detalle de usuario' }).click();
  await pagina.getByRole('button', { name: 'Ver detalle de Alumna de prueba' }).click();
  await expect(detalle).toBeVisible();
  await expect(detalle.getByRole('button', { name: /permiso de creación/ })).toHaveCount(0);
});

test('docente sin permiso individual no tiene el botón para crear casos', async ({ page: pagina }) => {
  const estado = await simularApi(pagina, 'DOCENTE');
  estado.usuarios.find((usuario) => usuario.rol === 'DOCENTE')!.puedeCrearCasos = false;
  await ingresar(pagina, 'docente@example.test');
  await expect(pagina.getByRole('button', { name: /Crear un caso/ })).toHaveCount(0);
  await pagina.getByRole('button', { name: 'Biblioteca de casos' }).click();
  await expect(pagina.getByRole('heading', { name: 'Biblioteca de casos.' })).toBeVisible();
  await expect(pagina.getByRole('button', { name: /Crear un caso/ })).toHaveCount(0);
});

test('docente: registra desde el menú lateral y selecciona al estudiante en una sesión', async ({
  page: pagina,
}) => {
  const estado = await simularApi(pagina, 'DOCENTE');
  await ingresar(pagina, 'docente@example.test');
  const menu = pagina.getByRole('navigation');
  const registrar = menu.getByRole('button', { name: 'Registrar estudiante' });
  await registrar.click();
  const modal = pagina.getByRole('dialog');
  await modal.getByLabel('Nombre del estudiante').fill('Estudiante nuevo');
  await modal.getByLabel('Correo del estudiante').fill('nuevo@example.test');
  await modal.getByLabel('Contraseña inicial').fill('clave-temporal');
  await modal.getByRole('button', { name: 'Crear estudiante' }).click();
  await expect(modal).toHaveCount(0);
  await expect(registrar).toBeFocused();
  await expect(pagina.getByRole('status')).toContainText('Estudiante registrado.');
  expect(estado.solicitudes.some((solicitud) =>
    solicitud.ruta === '/api/identidad/estudiantes' && solicitud.metodo === 'POST',
  )).toBe(true);

  await menu.getByRole('button', { name: 'Espacios académicos' }).click();
  await pagina.getByRole('button', { name: 'Programar un encuentro' }).click();
  await expect(modal.getByRole('button', { name: 'Registrar estudiante' })).toHaveCount(0);
  const estudiante = modal.getByRole('checkbox', { name: /Estudiante nuevo/ });
  await expect(estudiante).not.toBeChecked();
  await estudiante.check();
  await expect(estudiante).toBeChecked();
});

test('docente: editar espacio y conservar valores ante conflicto 409', async ({ page: pagina }) => {
  const estado = await simularApi(pagina, 'DOCENTE');
  await ingresar(pagina, 'docente@example.test');
  await pagina.getByRole('button', { name: 'Espacios académicos', exact: true }).click();
  await pagina.getByRole('button', { name: 'Editar espacio Psicología social' }).click();
  await pagina.getByLabel('Nombre', { exact: true }).fill('Psicología comunitaria');
  await pagina.getByLabel('Periodo', { exact: true }).fill('2027-1');
  const estudiante = pagina.getByRole('checkbox', { name: /Alumna de prueba/ });
  await estudiante.check();
  estado.conflictoEspacio = true;
  await pagina.getByRole('button', { name: 'Guardar espacio' }).click();
  await expect(pagina.getByRole('alert')).toHaveText('El espacio ya esta asociado a una sesion');
  await expect(pagina.getByLabel('Nombre', { exact: true })).toHaveValue('Psicología comunitaria');
  estado.conflictoEspacio = false;
  await pagina.getByRole('button', { name: 'Guardar espacio' }).click();
  await expect(pagina.getByRole('dialog')).toHaveCount(0);
  await expect(pagina.getByRole('heading', { name: 'Psicología comunitaria' })).toBeVisible();
  const escritura = estado.solicitudes.filter((solicitud) => solicitud.metodo === 'PUT').at(-1)!;
  expect(escritura.autorizacion).toBe('Bearer token-ficticio');
  expect(escritura.datos).toEqual({
    nombre: 'Psicología comunitaria',
    periodo: '2027-1',
    estudiantes: ['00000000-0000-0000-0000-000000000123'],
  });
  await pagina.getByRole('button', { name: 'Editar espacio Psicología comunitaria' }).click();
  await pagina.keyboard.press('Escape');
  await expect(
    pagina.getByRole('button', { name: 'Editar espacio Psicología comunitaria' }),
  ).toBeFocused();
  await pagina.getByRole('button', { name: 'Programar un encuentro' }).click();
  await expect(estudiante).toBeChecked();
  await estudiante.uncheck();
  await expect(estudiante).not.toBeChecked();
  await pagina.keyboard.press('Escape');
  await pagina.getByRole('button', { name: 'Sesiones', exact: true }).click();
  await pagina.getByRole('button', { name: 'Programar sesión' }).click();
  const espacioSesion = pagina.getByRole('combobox', { name: 'Espacio académico', exact: true });
  await espacioSesion.selectOption('');
  await expect(estudiante).not.toBeChecked();
  await espacioSesion.selectOption(estado.espacio.id);
  await expect(estudiante).toBeChecked();
});

test('docente: pagina las sesiones de cinco en cinco y vuelve al inicio al filtrar', async ({
  page: pagina,
}) => {
  await simularApi(pagina, 'DOCENTE');
  await pagina.route('**/api/sesiones', (ruta) => ruta.fulfill({
    json: Array.from({ length: 11 }, (_, indice) => ({
      id: `sesion-${indice + 1}`,
      idCaso: 'caso',
      idEspacio: 'espacio-real',
      inicioPrevisto: '2026-11-24T14:00:00Z',
      duracionMaximaMin: 40,
      lugar: `Aula ${indice + 1}`,
      estado: indice < 6 ? 'FINALIZADO' : 'SIN_INICIAR',
      convocatorias: [],
    })),
  }));
  await ingresar(pagina, 'docente@example.test');
  await pagina.getByRole('button', { name: 'Sesiones', exact: true }).click();
  const tarjetas = pagina.locator('.tarjeta-sesion');
  const paginacion = pagina.getByRole('group', { name: 'Paginación de sesiones' });
  const siguiente = paginacion.getByRole('button', { name: 'Siguiente' });
  const anterior = paginacion.getByRole('button', { name: 'Anterior' });
  await expect(tarjetas).toHaveCount(5);
  await expect(anterior).toBeDisabled();
  await siguiente.click();
  await expect(tarjetas.first()).toContainText('Aula 6');
  await siguiente.click();
  await expect(tarjetas).toHaveCount(1);
  await expect(tarjetas.first()).toContainText('Aula 11');
  await expect(siguiente).toBeDisabled();
  await anterior.click();
  await expect(paginacion).toContainText('Página 2 de 3');

  const estado = pagina.getByLabel('Filtrar por estado');
  await estado.selectOption('FINALIZADO');
  await expect(paginacion).toContainText('Página 1 de 2');
  await expect(tarjetas.first()).toContainText('Aula 1');
  await estado.selectOption('SIN_INICIAR');
  await expect(tarjetas).toHaveCount(5);
  await expect(paginacion).toHaveCount(0);
});

test('docente: advierte antes de abrir una sesión y refleja la apertura automática', async ({ page }) => {
  await page.clock.install({ time: new Date('2026-11-25T14:00:00Z') });
  await simularApi(page, 'DOCENTE');
  const base = {
    idCaso: 'caso', idEspacio: 'espacio-real', estado: 'SIN_INICIAR',
    finPrevisto: '2026-11-25T16:00:00Z', duracionMaximaMin: 30, convocatorias: [],
  };
  const manual = { ...base, id: 'manual', lugar: 'Aula manual', inicioPrevisto: '2026-11-25T15:00:00Z' };
  const programada = { ...base, id: 'programada', lugar: 'Aula automática', inicioPrevisto: '2026-11-25T14:01:00Z' };
  let iniciada = false;
  let aperturaAutomatica = false;
  await page.route('**/api/sesiones', (ruta) => ruta.fulfill({ json: [
    iniciada ? { ...manual, estado: 'EN_CURSO' } : manual,
    aperturaAutomatica ? { ...programada, estado: 'EN_CURSO' } : programada,
  ] }));
  await page.route('**/api/sesiones/manual/iniciar', (ruta) => {
    iniciada = true;
    return ruta.fulfill({ json: { ...manual, estado: 'EN_CURSO' } });
  });
  await ingresar(page, 'docente@example.test');
  await page.getByRole('button', { name: 'Sesiones', exact: true }).click();
  const tarjetaManual = page.locator('.tarjeta-sesion').filter({ hasText: 'Aula manual' });
  await tarjetaManual.getByRole('button', { name: 'Iniciar antes de tiempo' }).click();
  const aviso = page.getByRole('dialog', { name: '¿Abrir la sesión antes de tiempo?' });
  await expect(aviso.getByText(/Los estudiantes convocados podrán entrar desde ahora/)).toBeVisible();
  expect(iniciada).toBe(false);
  await aviso.getByRole('button', { name: 'Confirmar inicio' }).click();
  await expect(tarjetaManual.getByRole('button', { name: 'Finalizar sesión' })).toBeVisible();

  aperturaAutomatica = true;
  await page.clock.fastForward(65_000);
  const tarjetaAutomatica = page.locator('.tarjeta-sesion').filter({ hasText: 'Aula automática' });
  await expect(tarjetaAutomatica.getByRole('button', { name: 'Finalizar sesión' })).toBeVisible();
});

test('docente: abre la tarjeta de sesión y consulta el resumen de participación', async ({
  page: pagina,
}) => {
  await simularApi(pagina, 'DOCENTE');
  const sesion = {
    id: 'sesion-resumen', idCaso: 'caso', idEspacio: 'espacio-real', estado: 'FINALIZADO',
    inicioPrevisto: '2026-11-23T14:00:00Z', finPrevisto: '2026-11-23T15:00:00Z',
    duracionMaximaMin: 50, lugar: 'Aula 1', convocatorias: [],
  };
  await pagina.route('**/api/sesiones', (ruta) => ruta.fulfill({ json: [sesion] }));
  await pagina.route('**/api/sesiones/sesion-resumen', (ruta) => ruta.fulfill({
    json: { ...sesion, aperturaReal: '2026-11-24T14:05:00Z', cierreReal: '2026-11-24T15:15:00Z' },
  }));
  await pagina.route('**/api/reportes/sesiones/sesion-resumen', (ruta) => ruta.fulfill({
    json: [
      { idIntento: 'intento', nombreEstudiante: 'Ana Participante', estado: 'FINALIZADO',
        correoEstudiante: 'ana@example.test',
        inicioIntento: '2026-11-24T14:07:00Z', finIntento: '2026-11-24T14:43:00Z',
        preguntasCorrectas: 3, preguntasTotales: 4, calificacion: 3.75 },
      { idIntento: null, nombreEstudiante: 'Bruno Ausente', estado: 'AUSENTE' },
    ],
  }));
  await ingresar(pagina, 'docente@example.test');
  await pagina.getByRole('button', { name: 'Sesiones', exact: true }).click();
  const calendario = pagina.locator('.tarjeta-sesion .calendario');
  await expect(calendario).toBeVisible();
  const limites = (await calendario.boundingBox())!;
  await pagina.mouse.click(limites.x + limites.width / 2, limites.y + limites.height / 2);
  const detalle = pagina.getByRole('dialog');
  await expect(detalle.getByText('Apertura real', { exact: true }).locator('..')).toContainText('24/11/2026');
  await expect(detalle.getByText('Cierre real', { exact: true }).locator('..')).toContainText('24/11/2026');
  const participacion = detalle.getByRole('row', { name: /Ana Participante/ });
  await expect(participacion.getByRole('cell').nth(0)).toContainText('24/11/2026');
  await expect(participacion.getByRole('cell').nth(1)).toContainText('24/11/2026');
  await expect(participacion.getByRole('cell', { name: '3 / 4', exact: true })).toBeVisible();
  await expect(participacion.getByRole('cell', { name: '3.75', exact: true })).toBeVisible();
  await expect(detalle.getByText('Bruno Ausente')).toHaveCount(0);
  const filtro = detalle.getByRole('searchbox', { name: 'Buscar estudiante' });
  await filtro.fill('ANA@EXAMPLE.TEST');
  await expect(participacion).toBeVisible();
  await filtro.fill('sin coincidencias');
  await expect(participacion).toHaveCount(0);
  await expect(detalle.getByRole('status')).toHaveText('No hay estudiantes que coincidan con la búsqueda.');
  await filtro.fill('Participante');
  await expect(participacion).toBeVisible();
  await pagina.setViewportSize({ width: 390, height: 844 });
  await expect(participacion.getByRole('cell', { name: '3.75', exact: true })).toBeInViewport();
  await pagina.setViewportSize({ width: 1280, height: 720 });
  await pagina.keyboard.press('Escape');
  await expect(detalle).toHaveCount(0);
  await expect(pagina.getByRole('button', { name: /Ver detalles de/ })).toBeFocused();
  await pagina.getByRole('button', { name: /Ver detalles de/ }).click();
  await expect(filtro).toHaveValue('');
});

test('docente: consulta espacios por nombre y periodo en escritorio y móvil', async ({
  page: pagina,
}) => {
  await simularApi(pagina, 'DOCENTE');
  await ingresar(pagina, 'docente@example.test');
  await pagina.getByRole('button', { name: 'Espacios académicos', exact: true }).click();

  await expect(pagina.getByRole('columnheader', { name: 'Espacio académico' })).toBeVisible();
  await expect(pagina.getByRole('columnheader', { name: 'Identificador' })).toHaveCount(0);
  await expect(pagina.locator('.resumen-espacios')).toContainText('1 espacio encontrado');
  await pagina.getByLabel('Buscar espacios').fill('espacio-real');
  await expect(pagina.getByText('No encontramos coincidencias.')).toBeVisible();
  await pagina.getByRole('button', { name: 'Limpiar filtros' }).click();
  await pagina.getByLabel('Buscar espacios').fill('sin coincidencias');
  await expect(pagina.getByText('No encontramos coincidencias.')).toBeVisible();
  await pagina.getByRole('button', { name: 'Limpiar filtros' }).click();
  await expect(pagina.getByRole('heading', { name: 'Psicología social' })).toBeVisible();
  await pagina.getByLabel('Filtrar por periodo').selectOption('2026-2');
  await expect(pagina.locator('.resumen-espacios')).toContainText('1 espacio encontrado');

  await pagina.setViewportSize({ width: 390, height: 844 });
  await expect(pagina.locator('.fila-espacio')).toHaveCount(1);
  expect(
    await pagina
      .locator('.fila-espacio')
      .first()
      .evaluate((elemento) => getComputedStyle(elemento).display),
  ).toBe('grid');
  expect(await pagina.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
    390,
  );
});

test('docente: selecciona estudiantes por nombre y conserva la selección al filtrar', async ({
  page: pagina,
}) => {
  await simularApi(pagina, 'DOCENTE');
  await ingresar(pagina, 'docente@example.test');
  await pagina.getByRole('button', { name: 'Espacios académicos', exact: true }).click();
  await pagina.getByRole('button', { name: 'Programar un encuentro' }).click();
  await expect(pagina.getByRole('group', { name: 'Buscar estudiantes' })).toHaveCount(0);
  await expect(pagina.getByText('Este espacio no tiene estudiantes predeterminados.', { exact: false })).toBeVisible();
  const estudiante = pagina.getByRole('checkbox', { name: /Alumna de prueba/ });
  const filtro = pagina.getByRole('searchbox', { name: 'Filtrar por nombre o correo' });
  await estudiante.check();
  await filtro.fill('sin coincidencias');
  await expect(pagina.getByText('No hay coincidencias.')).toBeVisible();
  await expect(pagina.getByText('1 seleccionado', { exact: true })).toBeVisible();
  await filtro.fill('alumna+grupo@example.test');
  await expect(estudiante).toBeChecked();
  await estudiante.uncheck();
  await expect(pagina.getByText('0 seleccionados')).toBeVisible();
});

test('administración móvil: filtros, teclado y contenido sin desbordamiento', async ({
  page: pagina,
}) => {
  await pagina.setViewportSize({ width: 390, height: 844 });
  await simularApi(pagina);
  await ingresar(pagina);
  await expect(pagina.getByRole('heading', { name: 'Administración de usuarios.' })).toBeVisible();
  await pagina.getByLabel('Filtrar por rol').selectOption('ESTUDIANTE');
  await pagina.getByRole('button', { name: 'Aplicar filtros' }).click();
  await expect(pagina.locator('.cuenta')).toHaveCount(1);
  expect(await pagina.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
    390,
  );
  await pagina.screenshot({ path: 'capturas/administracion-movil.png', fullPage: true });
  await pagina.getByRole('button', { name: 'Ver detalle de Alumna de prueba' }).click();
  const ventana = pagina.getByRole('dialog');
  await expect(ventana).toBeVisible();
  const limites = await ventana.boundingBox();
  expect(limites!.x).toBeGreaterThanOrEqual(0);
  expect(limites!.x + limites!.width).toBeLessThanOrEqual(390);
  await pagina.screenshot({ path: 'capturas/detalle-usuario-movil.png' });
  await pagina.setViewportSize({ width: 1440, height: 1000 });
  await pagina.screenshot({ path: 'capturas/detalle-usuario-escritorio.png' });
});

test('el ingreso de estudiantes abre su espacio y no funciones del personal', async ({ page: pagina }) => {
  await simularApi(pagina, 'ESTUDIANTE');
  await pagina.route('**/api/identidad/yo/clave', (ruta) => {
    const { actual, nueva } = ruta.request().postDataJSON();
    if (actual !== 'clave-ficticia')
      return ruta.fulfill({ status: 400, json: { detail: 'La contraseña actual es incorrecta' } });
    expect(nueva).toBe('clave-nueva');
    return ruta.fulfill({ status: 204 });
  });
  await ingresar(pagina, 'alumna@example.test');
  await expect(pagina.getByRole('heading', { name: '¡Bienvenido!' })).toBeVisible();
  await expect(pagina.getByRole('heading', { name: 'Administración de usuarios.' })).toHaveCount(0);
  const perfil = pagina.locator('.navegacion .perfil');
  await expect(perfil.getByText('Alumna de prueba')).toBeVisible();
  await expect(perfil.getByText('Estudiante', { exact: true })).toBeVisible();
  await expect(perfil.getByRole('button', { name: 'Cerrar sesión' })).toBeVisible();
  const menu = (await pagina.getByRole('navigation', { name: 'Secciones del estudiante' }).boundingBox())!;
  const perfilLateral = (await perfil.boundingBox())!;
  expect(perfilLateral.y - (menu.y + menu.height)).toBeLessThanOrEqual(24);
  const miCuenta = pagina.getByRole('button', { name: 'Mi cuenta', exact: true });
  await miCuenta.click();
  const cuenta = pagina.getByRole('dialog', { name: 'Hola, Alumna de prueba.' });
  await expect(cuenta.getByText('alumna+grupo@example.test', { exact: true })).toBeVisible();
  const actual = cuenta.getByLabel('Contraseña actual', { exact: true });
  const nueva = cuenta.getByLabel('Nueva contraseña', { exact: true });
  const confirmacion = cuenta.getByLabel('Confirmar nueva contraseña', { exact: true });
  const guardar = cuenta.getByRole('button', { name: 'Guardar contraseña' });
  await actual.fill('clave-errada');
  await nueva.fill('clave-nueva');
  await confirmacion.fill('clave-distinta');
  await guardar.click();
  await expect(cuenta.getByRole('alert')).toHaveText('Las contraseñas nuevas no coinciden.');
  await confirmacion.fill('clave-nueva');
  await guardar.click();
  await expect(cuenta.getByRole('alert')).toHaveText('La contraseña actual es incorrecta');
  await actual.fill('clave-ficticia');
  await guardar.click();
  await expect(cuenta.getByRole('status')).toHaveText('Contraseña actualizada.');
  await expect(actual).toHaveValue('');
  await expect(nueva).toHaveValue('');
  await pagina.keyboard.press('Escape');
  await expect(cuenta).toBeHidden();
  await expect(miCuenta).toBeFocused();
  await pagina.setViewportSize({ width: 390, height: 844 });
  await expect(perfil).toBeHidden();
  await miCuenta.click();
  await cuenta.getByRole('button', { name: 'Cerrar sesión' }).click();
  await expect(pagina).toHaveURL(/\/ingreso$/);
});

test('administración: registrar docente valida contraseña y conserva datos ante conflicto', async ({
  page: pagina,
}) => {
  const estado = await simularApi(pagina);
  await ingresar(pagina);
  const crearPerfil = pagina.getByRole('button', { name: 'Crear perfil', exact: true });
  await crearPerfil.click();
  await expect(pagina.getByRole('dialog', { name: 'Crear perfil' })).toBeVisible();
  await expect(pagina.getByLabel('Rol del perfil')).toHaveValue('DOCENTE');
  const guardar = pagina.getByRole('button', { name: 'Crear cuenta docente' });
  await expect(guardar).toBeDisabled();
  await pagina.getByLabel('Nombre del docente').fill('Docente nuevo');
  await pagina.getByLabel('Correo del docente').fill('nuevo@example.test');
  await pagina.getByLabel('Contraseña inicial', { exact: true }).fill('🔐'.repeat(20));
  await guardar.click();
  await expect(pagina.getByRole('alert')).toContainText('72 bytes');
  expect(
    estado.solicitudes.filter((s) => s.metodo === 'POST' && s.ruta === '/api/identidad/docentes'),
  ).toHaveLength(0);
  await pagina.getByLabel('Contraseña inicial', { exact: true }).fill('clave-ficticia');
  estado.conflictoRegistro = true;
  await guardar.click();
  await expect(pagina.getByRole('alert')).toHaveText('El correo ya está registrado');
  await expect(pagina.getByLabel('Nombre del docente')).toHaveValue('Docente nuevo');
  estado.conflictoRegistro = false;
  await guardar.click();
  await expect(pagina.getByRole('dialog')).toHaveCount(0);
  await expect(pagina.getByRole('heading', { name: 'Docente nuevo', exact: true })).toBeVisible();
  const solicitud = estado.solicitudes.filter((s) => s.metodo === 'POST').at(-1)!;
  expect(solicitud.ruta).toBe('/api/identidad/docentes');
  expect(solicitud.autorizacion).toBe('Bearer token-ficticio');
  expect(solicitud.datos).toEqual({
    nombre: 'Docente nuevo',
    correo: 'nuevo@example.test',
    clave: 'clave-ficticia',
  });
  await crearPerfil.click();
  await expect(pagina.getByLabel('Contraseña inicial', { exact: true })).toHaveValue('');
  await pagina.keyboard.press('Escape');
  await expect(crearPerfil).toBeFocused();
});

test('administración: confirma cambio de rol y respeta el filtro actual', async ({
  page: pagina,
}) => {
  const estado = await simularApi(pagina);
  await ingresar(pagina);
  await pagina.getByLabel('Filtrar por rol').selectOption('DOCENTE');
  await pagina.getByRole('button', { name: 'Aplicar filtros' }).click();
  await pagina.getByRole('button', { name: 'Ver detalle de Docente original' }).click();
  await pagina.getByRole('button', { name: 'Cambiar rol', exact: true }).click();
  await expect(pagina.getByRole('button', { name: 'Revisar cambio' })).toBeDisabled();
  await pagina.getByLabel('Nuevo rol').selectOption('ADMINISTRADOR');
  await pagina.getByRole('button', { name: 'Revisar cambio' }).click();
  estado.desactivarDuranteCambioRol = true;
  expect(
    estado.solicitudes.filter((s) => s.ruta.endsWith('/rol') && s.metodo === 'PUT'),
  ).toHaveLength(0);
  await pagina.getByRole('button', { name: 'Confirmar cambio de rol' }).click();
  await expect(pagina.getByRole('dialog', { name: 'Detalle de Docente original' })).toBeVisible();
  await expect(
    pagina.getByRole('dialog').getByText('Administrador', { exact: true }),
  ).toBeVisible();
  await expect(pagina.getByRole('dialog').getByText('Inactivo', { exact: true })).toBeVisible();
  await pagina.getByRole('button', { name: 'Cerrar detalle de usuario' }).click();
  await expect(pagina.locator('.cuenta')).toHaveCount(0);
  await expect(pagina.getByRole('button', { name: 'Actualizar lista' })).toBeFocused();
  const solicitud = estado.solicitudes.find((s) => s.ruta.endsWith('/docente/rol'))!;
  expect(solicitud.datos).toEqual({ rol: 'ADMINISTRADOR' });
  expect(solicitud.autorizacion).toBe('Bearer token-ficticio');
  expect(
    estado.solicitudes.some(
      (s) => s.metodo === 'GET' && s.ruta === '/api/identidad/usuarios/docente',
    ),
  ).toBe(true);
  const secuencia = estado.solicitudes
    .filter(
      (s) =>
        s.ruta.endsWith('/docente/rol') ||
        s.ruta === '/api/identidad/usuarios/docente' ||
        s.ruta.startsWith('/api/identidad/usuarios?'),
    )
    .slice(-3)
    .map((s) => `${s.metodo} ${s.ruta.split('?')[0]}`);
  expect(secuencia).toEqual([
    'PUT /api/identidad/usuarios/docente/rol',
    'GET /api/identidad/usuarios/docente',
    'GET /api/identidad/usuarios',
  ]);
});

test('administración: rol rechazado conserva selección y el rol propio se protege', async ({
  page: pagina,
}) => {
  const estado = await simularApi(pagina);
  await ingresar(pagina);
  await pagina.getByRole('button', { name: 'Ver detalle de Administradora' }).click();
  await expect(pagina.getByRole('button', { name: 'Cambiar rol', exact: true })).toBeDisabled();
  await pagina.keyboard.press('Escape');
  await pagina.getByRole('button', { name: 'Ver detalle de Alumna de prueba' }).click();
  await pagina.getByRole('button', { name: 'Cambiar rol', exact: true }).click();
  await pagina.getByLabel('Nuevo rol').selectOption('DOCENTE');
  await pagina.getByRole('button', { name: 'Revisar cambio' }).click();
  estado.rechazarRol = true;
  await pagina.getByRole('button', { name: 'Confirmar cambio de rol' }).click();
  await expect(pagina.getByRole('alert')).toHaveText('El personal requiere una contraseña');
  await expect(pagina.getByLabel('Nuevo rol')).toHaveValue('DOCENTE');
  expect(estado.usuarios.find((u) => u.nombre === 'Alumna de prueba')!.rol).toBe('ESTUDIANTE');
  await pagina.getByRole('button', { name: 'Revisar selección' }).click();
  await pagina.getByRole('button', { name: 'Volver al detalle' }).click();
  await expect(pagina.getByRole('dialog').getByText('Estudiante', { exact: true })).toBeVisible();
});

test('administración: conserva el rol guardado si falla la recarga del detalle', async ({
  page: pagina,
}) => {
  const estado = await simularApi(pagina);
  await ingresar(pagina);
  await pagina.getByRole('button', { name: 'Ver detalle de Docente original' }).click();
  await pagina.getByRole('button', { name: 'Cambiar rol', exact: true }).click();
  await pagina.getByLabel('Nuevo rol').selectOption('ADMINISTRADOR');
  await pagina.getByRole('button', { name: 'Revisar cambio' }).click();
  estado.falloDetalle = true;
  await pagina.getByRole('button', { name: 'Confirmar cambio de rol' }).click();
  await expect(
    pagina.getByRole('dialog').getByText('Administrador', { exact: true }),
  ).toBeVisible();
  await expect(pagina.getByText(/Rol actualizado\. No se pudo recargar el detalle/)).toBeVisible();
  await expect(pagina.getByRole('button', { name: 'Cambiar rol', exact: true })).toBeVisible();
  expect(estado.solicitudes.filter((s) => s.ruta.endsWith('/rol'))).toHaveLength(1);
  estado.falloDetalle = false;
  await pagina.getByRole('button', { name: 'Reintentar consulta' }).click();
  await expect(
    pagina.getByText('El cambio se guardó, pero el detalle no pudo recargarse.'),
  ).toHaveCount(0);
  await expect(
    pagina.getByRole('dialog').getByText('Administrador', { exact: true }),
  ).toBeVisible();
  expect(estado.solicitudes.filter((s) => s.ruta.endsWith('/rol'))).toHaveLength(1);
});

test('administración: el creador de perfiles registra estudiantes en móvil', async ({ page: pagina }) => {
  const estado = await simularApi(pagina);
  await pagina.setViewportSize({ width: 390, height: 844 });
  await ingresar(pagina);
  await pagina.getByRole('button', { name: 'Crear perfil', exact: true }).click();
  await expect(pagina.getByRole('dialog')).toBeVisible();
  const limites = await pagina.getByRole('dialog').boundingBox();
  expect(limites!.x + limites!.width).toBeLessThanOrEqual(390);
  expect(await pagina.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
    390,
  );
  await pagina.getByLabel('Nombre del docente').fill('Estudiante nuevo');
  await pagina.getByLabel('Rol del perfil').selectOption('ESTUDIANTE');
  await expect(pagina.getByLabel('Nombre del estudiante')).toHaveValue('Estudiante nuevo');
  await pagina.getByLabel('Correo del estudiante').fill('estudiante-nuevo@example.test');
  await pagina.getByLabel('Contraseña inicial', { exact: true }).fill('clave-ficticia');
  await pagina.getByRole('button', { name: 'Crear cuenta estudiantil' }).click();
  await expect(pagina.getByRole('dialog')).toHaveCount(0);
  const solicitud = estado.solicitudes.filter((s) => s.metodo === 'POST').at(-1)!;
  expect(solicitud.ruta).toBe('/api/identidad/estudiantes');
  expect(solicitud.datos).toEqual({
    nombre: 'Estudiante nuevo',
    correo: 'estudiante-nuevo@example.test',
    clave: 'clave-ficticia',
  });
});

test('administración: búsqueda en API, contador y limpieza de filtros combinados', async ({
  page: pagina,
}) => {
  const estado = await simularApi(pagina);
  await ingresar(pagina);
  const buscar = pagina.getByLabel('Buscar usuarios');
  await expect(pagina.locator('.resumen-usuarios')).toContainText('3 usuarios encontrados');
  await buscar.fill('DOCENTE');
  await buscar.press('Enter');
  await expect(pagina.locator('.cuenta')).toHaveCount(1);
  await expect(pagina.locator('.resumen-usuarios')).toContainText('1 usuario encontrado');
  expect(
    estado.solicitudes.some(
      (s) => new URL('http://prueba' + s.ruta).searchParams.get('busqueda') === 'DOCENTE',
    ),
  ).toBe(true);
  await buscar.fill('ALUMNA+GRUPO@EXAMPLE.TEST');
  await pagina.getByRole('button', { name: 'Aplicar filtros' }).click();
  await expect(
    pagina.getByRole('heading', { name: 'Alumna de prueba', exact: true }),
  ).toBeVisible();
  const peticion = estado.solicitudes
    .filter((s) => s.ruta.startsWith('/api/identidad/usuarios?'))
    .at(-1)!;
  expect(new URL('http://prueba' + peticion.ruta).searchParams.get('busqueda')).toBe(
    'ALUMNA+GRUPO@EXAMPLE.TEST',
  );
  await pagina.getByLabel('Filtrar por rol').selectOption('DOCENTE');
  await pagina.getByRole('button', { name: 'Aplicar filtros' }).click();
  await expect(pagina.locator('.cuenta')).toHaveCount(0);
  await expect(pagina.getByText('No encontramos coincidencias.')).toBeVisible();
  await pagina.getByRole('button', { name: 'Limpiar filtros' }).click();
  await expect(buscar).toHaveValue('');
  await expect(pagina.getByLabel('Filtrar por rol')).toHaveValue('');
  await expect(pagina.getByLabel('Filtrar por estado')).toHaveValue('');
  await expect(pagina.locator('.cuenta')).toHaveCount(3);
});

test('administración: pagina de diez en diez y vuelve a la primera página al filtrar', async ({
  page: pagina,
}) => {
  await pagina.setViewportSize({ width: 1440, height: 1000 });
  const estado = await simularApi(pagina);
  estado.usuarios.push(
    ...Array.from({ length: 20 }, (_, indice) => ({
      id: `lote-${indice}`,
      nombre: `Usuario del lote ${indice + 1}`,
      correo: `lote-${indice + 1}@example.test`,
      rol: 'ESTUDIANTE',
      activo: true,
      puedeCrearCasos: false,
    })),
  );
  await ingresar(pagina);

  const filas = pagina.locator('.tabla-usuarios .cuenta');
  const paginador = pagina.getByRole('navigation', { name: 'Paginación de usuarios' });
  const anterior = paginador.getByRole('button', { name: 'Anterior' });
  const siguiente = paginador.getByRole('button', { name: 'Siguiente' });
  await expect(filas).toHaveCount(10);
  await expect(pagina.locator('.resumen-usuarios')).toContainText('23 usuarios encontrados');
  await expect(paginador).toContainText('Página 1 de 3');
  await expect(anterior).toBeDisabled();
  const primeraCuenta = await filas.first().innerText();

  await siguiente.click();
  await expect(paginador).toContainText('Página 2 de 3');
  await expect(filas).toHaveCount(10);
  await expect(filas.first()).not.toHaveText(primeraCuenta);

  for (const pantalla of [{ width: 768, height: 1024 }, { width: 390, height: 844 }]) {
    await pagina.setViewportSize(pantalla);
    await expect(siguiente).toBeVisible();
    expect(await paginador.evaluate((elemento) => elemento.scrollWidth <= elemento.clientWidth))
      .toBe(true);
  }
  await siguiente.press('Enter');
  await expect(paginador).toContainText('Página 3 de 3');
  await expect(filas).toHaveCount(3);
  await expect(siguiente).toBeDisabled();
  await anterior.click();
  await expect(paginador).toContainText('Página 2 de 3');

  const buscar = pagina.getByLabel('Buscar usuarios');
  await buscar.fill('Usuario del lote');
  await buscar.press('Enter');
  await expect(paginador).toContainText('Página 1 de 2');
  await expect(filas).toHaveCount(10);
  await expect(pagina.locator('.resumen-usuarios')).toContainText('20 usuarios encontrados');
});

test('administración: tabla compacta, etiquetas y explicación de cuenta propia', async ({
  page: pagina,
}) => {
  await pagina.setViewportSize({ width: 1440, height: 1000 });
  await simularApi(pagina);
  await ingresar(pagina);
  await expect(pagina.getByRole('columnheader', { name: 'Usuario', exact: true })).toBeVisible();
  await expect(pagina.getByRole('columnheader', { name: 'Rol', exact: true })).toBeVisible();
  const propia = pagina.locator('.cuenta').filter({ hasText: 'Administradora' });
  await expect(propia.locator('psico-etiqueta-usuario').first()).toHaveText('Administrador');
  const desactivar = pagina.getByRole('button', { name: 'Desactivar Administradora', exact: true });
  await expect(desactivar).toBeDisabled();
  await expect(desactivar).toHaveAccessibleDescription('No puedes desactivar tu propia cuenta.');
  const control = propia.getByRole('group', { name: 'Desactivar cuenta propia' });
  const aviso = propia.getByText('No puedes desactivar tu propia cuenta.', { exact: true });
  await expect(aviso).toBeHidden();
  await control.hover();
  await expect(aviso).toBeVisible();
  await propia.getByRole('button', { name: 'Ver detalle de Administradora' }).hover();
  await expect(aviso).toBeHidden();
  await propia.getByRole('button', { name: 'Ver detalle de Administradora' }).focus();
  await pagina.keyboard.press('Tab');
  await expect(control).toBeFocused();
  await expect(aviso).toBeVisible();
  expect(
    await pagina
      .locator('.cuenta')
      .first()
      .evaluate((e) => getComputedStyle(e).display),
  ).toBe('table-row');
  const primera = await pagina.locator('.cuenta').first().boundingBox();
  expect(primera!.height).toBeLessThan(150);
});

test('administración móvil: primera cuenta visible y menú explícito accesible', async ({
  page: pagina,
}) => {
  await pagina.setViewportSize({ width: 390, height: 844 });
  await simularApi(pagina);
  await ingresar(pagina);
  await expect(pagina.locator('.cuenta')).toHaveCount(3);
  const primera = await pagina.locator('.cuenta').first().boundingBox();
  expect(primera!.y + primera!.height).toBeLessThanOrEqual(844);
  expect(
    await pagina
      .locator('.cuenta')
      .first()
      .evaluate((e) => getComputedStyle(e).display),
  ).toBe('grid');
  for (const selector of [
    '.ruta',
    '.acciones-cabecera .boton-texto',
    '.celda-rol .etiqueta',
    '.celda-usuario p',
  ]) {
    expect(
      await pagina
        .locator(selector)
        .first()
        .evaluate((e) => parseFloat(getComputedStyle(e).fontSize)),
    ).toBeGreaterThanOrEqual(12);
  }
  const menu = pagina.getByRole('button', { name: 'Abrir menú principal' });
  await expect(menu).toHaveAttribute('aria-expanded', 'false');
  await menu.click();
  await expect(pagina.getByRole('button', { name: 'Administración', exact: true })).toBeVisible();
  await pagina.keyboard.press('Escape');
  await expect(menu).toBeFocused();
  await menu.click();
  await pagina.getByRole('button', { name: 'Administración', exact: true }).click();
  await expect(menu).toHaveAttribute('aria-expanded', 'false');
  expect(await pagina.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
    390,
  );
});

test('administración: fallo de lectura tras guardar no vuelve a enviar la escritura', async ({
  page: pagina,
}) => {
  const estado = await simularApi(pagina);
  await ingresar(pagina);
  await pagina.getByRole('button', { name: 'Ver detalle de Docente original' }).click();
  await pagina.getByRole('button', { name: 'Editar docente', exact: true }).click();
  await pagina.getByLabel('Nombre del docente').fill('Docente guardado');
  estado.falloListado = true;
  await pagina.getByRole('button', { name: 'Guardar docente' }).click();
  await expect(pagina.getByRole('heading', { name: 'Detalle de Docente guardado' })).toBeVisible();
  await expect(
    pagina.getByText('Los cambios se guardaron. Actualiza el listado al cerrar esta ventana.'),
  ).toBeVisible();
  await pagina.getByRole('button', { name: 'Cerrar detalle de usuario' }).click();
  estado.falloListado = false;
  await pagina.getByRole('button', { name: 'Actualizar lista' }).click();
  await expect(
    pagina.getByRole('heading', { name: 'Docente guardado', exact: true }),
  ).toBeVisible();
  expect(estado.solicitudes.filter((s) => s.metodo === 'PUT')).toHaveLength(1);
});

test('un enlace de convocatoria conserva el destino y el inicio es explícito', async ({
  page: pagina,
}) => {
  await pagina.clock.install({ time: new Date('2026-11-30T12:59:40Z') });
  const estado = await simularApi(pagina, 'ESTUDIANTE');
  const actividad = {
    idSesion: 'sesion-prueba', idCaso: 'caso', nombreCaso: 'Simulación de prueba',
    contexto: 'Psicología social', inicioPrevisto: '2026-11-30T11:00:00Z',
    finPrevisto: '2026-11-30T13:00:00Z', duracionMaximaMin: 30,
    modoNavegacion: 'RESTRINGIDA', estadoSesion: 'EN_CURSO', lugar: 'Virtual',
    mensaje: 'Responde con calma', idIntento: null, estadoIntento: null, accion: 'INICIAR',
  };
  const preguntas = [
    { id: 'p1', escenario: 'Escenario', contexto: 'Contexto', enunciado: '¿Qué harías?', opciones: [{ id: 'o1', texto: 'Escuchar al grupo' }] },
    { id: 'p2', escenario: 'Escenario', contexto: 'Contexto', enunciado: '¿Qué sigue?', opciones: [] },
  ];
  const intento = {
    id: 'intento', idSesion: 'sesion-prueba', inicio: '2026-11-30T12:59:40Z',
    limite: '2026-11-30T13:00:00Z', fin: null, estado: 'EN_PROGRESO',
    modoNavegacion: 'RESTRINGIDA', preguntas: ['p1', 'p2'], preguntaActual: 'p1', respuestas: {},
  };
  let sesionAbierta = false;
  await pagina.route('**/api/estudiante/actividades', (ruta) =>
    ruta.fulfill({ json: [
      { ...actividad, idSesion: 'sesion-finalizada', estadoSesion: 'FINALIZADO', accion: 'ESPERAR' },
      { ...actividad, idSesion: 'sesion-vencida', nombreCaso: 'Ingreso vencido', finPrevisto: '2026-11-30T12:30:00Z' },
      sesionAbierta ? actividad : { ...actividad, estadoSesion: 'SIN_INICIAR', accion: 'ESPERAR' },
    ] }),
  );
  await pagina.route('**/api/estudiante/actividades/sesion-prueba', (ruta) =>
    ruta.fulfill({ json: { actividad, preguntas, intento: null } }),
  );
  await pagina.route('**/api/estudiante/actividades/sesion-prueba/iniciar', (ruta) =>
    ruta.fulfill({ status: 201, json: intento }),
  );
  await pagina.route('**/api/participaciones/intento/respuestas', (ruta) =>
    ruta.fulfill({ json: { ...intento, respuestas: { p1: 'o1' } } }),
  );
  await pagina.route('**/api/estudiante/resultados', (ruta) =>
    ruta.fulfill({
      json: [{
        idIntento: 'intento-anterior', nombreCaso: 'Simulación anterior',
        fin: '2026-11-29T14:00:00Z', calificacion: 4,
        respuestas: [], valoraciones: [],
      }],
    }),
  );
  const comprobarAncho = async () => {
    for (const [width, height] of [[1440, 1000], [768, 1024], [390, 844]]) {
      await pagina.setViewportSize({ width, height });
      expect(await pagina.evaluate(() => document.documentElement.scrollWidth))
        .toBeLessThanOrEqual(width);
    }
  };
  await pagina.goto('/ingreso?sesion=sesion-prueba');
  await pagina.getByLabel('Correo institucional').fill('alumna+grupo@example.test');
  await pagina.getByLabel('Contraseña', { exact: true }).fill('clave-ficticia');
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await expect(pagina).toHaveURL(/\/estudiante\/actividades\/sesion-prueba/);
  expect(estado.solicitudes.filter((solicitud) => solicitud.metodo === 'POST')).toHaveLength(1);
  await comprobarAncho();
  await expect(pagina.getByText('Pendiente de inicio o fuera de la ventana de ingreso.')).toBeVisible();
  const vencida = pagina.locator('.tarjeta-actividad').filter({ hasText: 'Ingreso vencido' });
  await expect(vencida).toContainText('Plazo terminado');
  await expect(vencida.getByRole('button', { name: 'Iniciar actividad' })).toHaveCount(0);
  await expect(vencida.getByRole('timer')).toHaveCount(0);
  sesionAbierta = true;
  await pagina.getByRole('button', { name: 'Actualizar', exact: true }).click();
  const tarjetas = pagina.locator('.tarjeta-actividad');
  await expect(tarjetas.first()).toContainText('Sesión 2 de 2');
  await expect(tarjetas.first()).toContainText('Disponible');
  await expect(tarjetas.last()).toContainText('Sesión 1 de 2');
  await expect(tarjetas.last()).toContainText('Finalizada');
  await expect(tarjetas.last().getByRole('button', { name: 'Iniciar actividad' })).toHaveCount(0);
  const cuentaIngreso = tarjetas.first().getByRole('timer', { name: 'Tiempo para entrar' });
  await expect(cuentaIngreso).toHaveText(/^00:[0-5]\d$/);
  const tiempoInicial = await cuentaIngreso.textContent();
  await pagina.clock.fastForward(2000);
  await expect(cuentaIngreso).not.toHaveText(tiempoInicial!);
  await tarjetas.first().getByRole('button', { name: 'Iniciar actividad' }).click();
  await expect(pagina.getByRole('heading', { name: 'Simulación de prueba', level: 1 })).toBeVisible();
  await expect(pagina.getByRole('navigation', { name: 'Navegación principal' })).toHaveCount(0);
  await expect(pagina.getByRole('timer')).toBeVisible();
  await expect(pagina.getByRole('button', { name: 'Siguiente' })).toBeDisabled();
  await expect(pagina.getByText('0 de 2 preguntas respondidas')).toBeVisible();
  await pagina.getByRole('radio', { name: 'Escuchar al grupo' }).check();
  await expect(pagina.getByText('1 de 2 preguntas respondidas')).toBeVisible();
  await expect(pagina.getByRole('button', { name: 'Siguiente' })).toBeEnabled();
  await comprobarAncho();
  await pagina.getByRole('button', { name: 'Mis resultados', exact: true }).click();
  await expect(pagina.getByRole('heading', { name: 'Simulación anterior' })).toBeVisible();
  await comprobarAncho();
  await pagina.getByRole('button', { name: 'Ver retroalimentación' }).click();
  await expect(pagina.getByRole('button', { name: 'Todos los intentos' })).toBeVisible();
  await comprobarAncho();
});
