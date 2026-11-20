import { test, expect } from '@playwright/test';
import { simularApi, ingresar } from './soporte/identidad';

test('administración: roles, filtros, detalle, edición y errores sin perder el formulario', async ({
  page: pagina,
}) => {
  const estado = await simularApi(pagina);
  await ingresar(pagina);
  await expect(pagina.getByRole('heading', { name: 'Roles y permisos' })).toHaveCount(0);
  await expect(pagina.getByRole('button', { name: 'Permisos docentes' })).toHaveCount(0);
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
  expect(escritura.csrf).toBe('csrf-gestion');
  expect(escritura.datos).toEqual({
    nombre: 'Docente actualizado',
    correo: 'actualizado@example.test',
  });
  expect(
    estado.solicitudes.some((solicitud) => solicitud.ruta.includes('/academico/')),
  ).toBeFalsy();
  expect(
    estado.solicitudes.some((solicitud) => solicitud.ruta.includes('/creacion-casos')),
  ).toBeFalsy();
  await pagina.getByRole('button', { name: 'Volver al listado' }).click();
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
  await pagina.getByRole('button', { name: 'Volver al listado' }).focus();
  await pagina.keyboard.press('Tab');
  await expect(pagina.getByRole('button', { name: 'Cerrar detalle de usuario' })).toBeFocused();
  await pagina.keyboard.press('Shift+Tab');
  await expect(pagina.getByRole('button', { name: 'Volver al listado' })).toBeFocused();
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
      .every((solicitud) => solicitud.csrf === 'csrf-gestion'),
  ).toBeTruthy();
  await pagina.getByRole('button', { name: 'Cerrar sesión', exact: true }).click();
  await expect(pagina.getByRole('heading', { name: 'Administración de usuarios.' })).toHaveCount(0);
  expect(await pagina.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
});

test('docente: editar espacio y conservar valores ante conflicto 409', async ({ page: pagina }) => {
  const estado = await simularApi(pagina, 'DOCENTE');
  await ingresar(pagina, 'docente@example.test');
  await pagina.getByRole('button', { name: 'Espacios académicos', exact: true }).click();
  await pagina.getByRole('button', { name: 'Editar espacio Psicología social' }).click();
  await pagina.getByLabel('Nombre', { exact: true }).fill('Psicología comunitaria');
  await pagina.getByLabel('Periodo', { exact: true }).fill('2027-1');
  estado.conflictoEspacio = true;
  await pagina.getByRole('button', { name: 'Guardar espacio' }).click();
  await expect(pagina.getByRole('alert')).toHaveText('El espacio ya esta asociado a una sesion');
  await expect(pagina.getByLabel('Nombre', { exact: true })).toHaveValue('Psicología comunitaria');
  estado.conflictoEspacio = false;
  await pagina.getByRole('button', { name: 'Guardar espacio' }).click();
  await expect(pagina.getByRole('dialog')).toHaveCount(0);
  await expect(pagina.getByRole('heading', { name: 'Psicología comunitaria' })).toBeVisible();
  const escritura = estado.solicitudes.filter((solicitud) => solicitud.metodo === 'PUT').at(-1)!;
  expect(escritura.csrf).toBe('csrf-gestion');
  expect(escritura.datos).toEqual({ nombre: 'Psicología comunitaria', periodo: '2027-1' });
  await pagina.getByRole('button', { name: 'Editar espacio Psicología comunitaria' }).click();
  await pagina.keyboard.press('Escape');
  await expect(
    pagina.getByRole('button', { name: 'Editar espacio Psicología comunitaria' }),
  ).toBeFocused();
});

test('docente: buscar correo exacto, agregar sin duplicados y limpiar resultado al fallar', async ({
  page: pagina,
}) => {
  const estado = await simularApi(pagina, 'DOCENTE');
  await ingresar(pagina, 'docente@example.test');
  await pagina.getByRole('button', { name: 'Espacios académicos', exact: true }).click();
  await pagina.getByRole('button', { name: 'Programar un encuentro' }).click();
  const campo = pagina.getByLabel('Estudiantes (identificadores separados por comas)');
  for (let intento = 0; intento < 2; intento++) {
    await pagina
      .getByLabel('Correo del estudiante', { exact: true })
      .fill('alumna+grupo@example.test');
    await pagina.getByRole('button', { name: 'Buscar por correo' }).click();
    await pagina.getByRole('button', { name: 'Agregar a Alumna de prueba' }).click();
    await expect(campo).toHaveValue('00000000-0000-0000-0000-000000000123');
  }
  expect(
    estado.solicitudes
      .filter((solicitud) => solicitud.ruta.includes('/estudiantes?'))
      .every(
        (solicitud) =>
          new URL('http://prueba' + solicitud.ruta).searchParams.get('correo') ===
          'alumna+grupo@example.test',
      ),
  ).toBeTruthy();
  await pagina.getByLabel('Correo del estudiante', { exact: true }).fill('ausente@example.test');
  await pagina.getByRole('button', { name: 'Buscar por correo' }).click();
  await expect(pagina.getByRole('alert')).toHaveText('Estudiante no encontrado');
  await expect(pagina.getByRole('button', { name: 'Agregar a Alumna de prueba' })).toHaveCount(0);
  await expect(pagina.getByRole('button', { name: 'Administración', exact: true })).toHaveCount(0);
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

test('el ingreso de estudiantes no abre funciones del personal', async ({ page: pagina }) => {
  await simularApi(pagina, 'ESTUDIANTE');
  await ingresar(pagina, 'alumna@example.test');
  await expect(pagina.getByRole('alert')).toHaveText(
    'Esta interfaz está disponible para docentes y administradores.',
  );
  await expect(pagina.getByRole('heading', { name: 'Administración de usuarios.' })).toHaveCount(0);
});

test('administración: registrar docente valida contraseña y conserva datos ante conflicto', async ({
  page: pagina,
}) => {
  const estado = await simularApi(pagina);
  await ingresar(pagina);
  await pagina.getByRole('button', { name: 'Registrar docente' }).click();
  await expect(pagina.getByRole('dialog', { name: 'Registrar docente' })).toBeVisible();
  const guardar = pagina.getByRole('button', { name: 'Crear cuenta docente' });
  await expect(guardar).toBeDisabled();
  await pagina.getByLabel('Nombre del docente').fill('Docente nuevo');
  await pagina.getByLabel('Correo del docente').fill('nuevo@example.test');
  await pagina.getByLabel('Contraseña inicial', { exact: true }).fill('🔐'.repeat(20));
  await guardar.click();
  await expect(pagina.getByRole('alert')).toContainText('72 bytes');
  expect(estado.solicitudes.filter((s) => s.metodo === 'POST')).toHaveLength(0);
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
  expect(solicitud.csrf).toBe('csrf-gestion');
  expect(solicitud.datos).toEqual({
    nombre: 'Docente nuevo',
    correo: 'nuevo@example.test',
    clave: 'clave-ficticia',
  });
  await pagina.getByRole('button', { name: 'Registrar docente' }).click();
  await expect(pagina.getByLabel('Contraseña inicial', { exact: true })).toHaveValue('');
  await pagina.keyboard.press('Escape');
  await expect(pagina.getByRole('button', { name: 'Registrar docente' })).toBeFocused();
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
  expect(
    estado.solicitudes.filter((s) => s.ruta.endsWith('/rol') && s.metodo === 'PUT'),
  ).toHaveLength(0);
  await pagina.getByRole('button', { name: 'Confirmar cambio de rol' }).click();
  await expect(pagina.getByRole('dialog', { name: 'Detalle de Docente original' })).toBeVisible();
  await expect(
    pagina.getByRole('dialog').getByText('Administrador', { exact: true }),
  ).toBeVisible();
  await expect(pagina.getByRole('dialog').getByText('Activo', { exact: true })).toBeVisible();
  await pagina.getByRole('button', { name: 'Volver al listado' }).click();
  await expect(pagina.locator('.cuenta')).toHaveCount(0);
  await expect(pagina.getByRole('button', { name: 'Actualizar lista' })).toBeFocused();
  const solicitud = estado.solicitudes.find((s) => s.ruta.endsWith('/docente/rol'))!;
  expect(solicitud.datos).toEqual({ rol: 'ADMINISTRADOR' });
  expect(solicitud.csrf).toBe('csrf-gestion');
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

test('administración: registro de docentes en móvil', async ({ page: pagina }) => {
  const estado = await simularApi(pagina);
  await pagina.setViewportSize({ width: 390, height: 844 });
  await ingresar(pagina);
  await pagina.getByRole('button', { name: 'Registrar docente' }).click();
  await expect(pagina.getByRole('dialog')).toBeVisible();
  const limites = await pagina.getByRole('dialog').boundingBox();
  expect(limites!.x + limites!.width).toBeLessThanOrEqual(390);
  expect(await pagina.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
    390,
  );
  await pagina.screenshot({ path: 'capturas/registro-docente-movil.png' });
  await pagina.setViewportSize({ width: 1440, height: 1000 });
  await pagina.screenshot({ path: 'capturas/registro-docente-escritorio.png' });
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
  await pagina.getByRole('button', { name: 'Volver al listado' }).click();
  estado.falloListado = false;
  await pagina.getByRole('button', { name: 'Actualizar lista' }).click();
  await expect(
    pagina.getByRole('heading', { name: 'Docente guardado', exact: true }),
  ).toBeVisible();
  expect(estado.solicitudes.filter((s) => s.metodo === 'PUT')).toHaveLength(1);
});
