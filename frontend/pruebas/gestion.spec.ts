import { test, expect, Page } from '@playwright/test';

async function simularApi(pagina: Page, rol = 'ADMINISTRADOR') {
  const administrador = {
    id: 'admin',
    nombre: 'Administradora',
    correo: 'admin@example.test',
    rol: 'ADMINISTRADOR',
    activo: true,
    puedeCrearCasos: false,
  };
  const docente = {
    id: 'docente',
    nombre: 'Docente original',
    correo: 'docente@example.test',
    rol: 'DOCENTE',
    activo: true,
    puedeCrearCasos: true,
  };
  const estudiante = {
    id: '00000000-0000-0000-0000-000000000123',
    nombre: 'Alumna de prueba',
    correo: 'alumna+grupo@example.test',
    rol: 'ESTUDIANTE',
    activo: true,
    puedeCrearCasos: false,
  };
  const estado = {
    rol,
    usuarios: [administrador, docente, estudiante],
    permiso: true,
    espacio: { id: 'espacio-real', nombre: 'Psicología social', periodo: '2026-2' },
    conflictoEspacio: false,
    conflictoDocente: false,
    solicitudes: [] as { ruta: string; metodo: string; datos: any; csrf: string | undefined }[],
  };
  await pagina.route('**/api/**', async (ruta) => {
    const solicitud = ruta.request();
    const url = new URL(solicitud.url());
    const metodo = solicitud.method();
    const datos = solicitud.postData() ? solicitud.postDataJSON() : null;
    estado.solicitudes.push({
      ruta: url.pathname + url.search,
      metodo,
      datos,
      csrf: solicitud.headers()['x-csrf-token'],
    });
    if (url.pathname.endsWith('/yo'))
      return ruta.fulfill({
        json:
          estado.rol === 'ADMINISTRADOR'
            ? administrador
            : estado.rol === 'DOCENTE'
              ? docente
              : estudiante,
      });
    if (url.pathname.endsWith('/csrf'))
      return ruta.fulfill({ json: { nombreCabecera: 'X-CSRF-TOKEN', token: 'csrf-gestion' } });
    if (url.pathname === '/api/identidad/usuarios')
      return ruta.fulfill({
        json: estado.usuarios.filter(
          (usuario) =>
            (!url.searchParams.get('rol') || usuario.rol === url.searchParams.get('rol')) &&
            (!url.searchParams.get('activo') ||
              String(usuario.activo) === url.searchParams.get('activo')),
        ),
      });
    if (url.pathname === '/api/identidad/roles')
      return ruta.fulfill({ json: ['ADMINISTRADOR', 'DOCENTE', 'ESTUDIANTE'] });
    if (url.pathname.endsWith('/creacion-casos')) {
      if (metodo === 'PUT') estado.permiso = datos.habilitado;
      return ruta.fulfill({
        status: metodo === 'PUT' ? 204 : 200,
        ...(metodo === 'PUT'
          ? {}
          : { json: { habilitado: url.pathname.includes('/DOCENTE/') && estado.permiso } }),
      });
    }
    if (url.pathname === '/api/identidad/estudiantes') {
      if (url.searchParams.get('correo')?.toLowerCase() !== estudiante.correo)
        return ruta.fulfill({ status: 404, json: { detail: 'Estudiante no encontrado' } });
      return ruta.fulfill({ json: estudiante });
    }
    if (url.pathname.startsWith('/api/identidad/usuarios/')) {
      const usuario = estado.usuarios.find((usuario) => usuario.id === url.pathname.split('/')[4]);
      if (!usuario) return ruta.fulfill({ status: 404, json: {} });
      if (metodo === 'PUT') usuario.activo = datos.activo;
      return ruta.fulfill({ json: usuario });
    }
    if (url.pathname.startsWith('/api/identidad/docentes/')) {
      if (estado.conflictoDocente)
        return ruta.fulfill({ status: 409, json: { detail: 'El correo ya está registrado' } });
      Object.assign(docente, datos);
      return ruta.fulfill({ json: docente });
    }
    if (url.pathname === '/api/academico/espacios') return ruta.fulfill({ json: [estado.espacio] });
    if (url.pathname.startsWith('/api/academico/espacios/') && metodo === 'PUT') {
      if (estado.conflictoEspacio)
        return ruta.fulfill({
          status: 409,
          json: { detail: 'El espacio ya esta asociado a una sesion' },
        });
      Object.assign(estado.espacio, datos);
      return ruta.fulfill({ json: estado.espacio });
    }
    return ruta.fulfill({ status: 404, json: { detail: 'Ruta no simulada' } });
  });
  return estado;
}

async function ingresar(pagina: Page, correo = 'admin@example.test') {
  await pagina.goto('/');
  await pagina.getByRole('button', { name: 'Conectar cuenta', exact: true }).first().click();
  await pagina.getByLabel('Correo institucional').fill(correo);
  await pagina.getByLabel('Contraseña', { exact: true }).fill('clave-ficticia');
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
}

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
});

test('administración: confirmación de estado, reactivación y permiso compartido', async ({
  page: pagina,
}) => {
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
  await pagina.getByRole('button', { name: 'Retirar permiso de creación' }).click();
  await pagina.getByRole('button', { name: 'Confirmar permiso', exact: true }).click();
  await expect(pagina.getByRole('button', { name: 'Conceder permiso de creación' })).toBeVisible();
  expect(estado.permiso).toBe(false);
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
});

test('el ingreso de estudiantes no abre funciones del personal', async ({ page: pagina }) => {
  await simularApi(pagina, 'ESTUDIANTE');
  await ingresar(pagina, 'alumna@example.test');
  await expect(pagina.getByRole('alert')).toHaveText(
    'Esta interfaz está disponible para docentes y administradores.',
  );
  await expect(pagina.getByRole('heading', { name: 'Administración de usuarios.' })).toHaveCount(0);
});
