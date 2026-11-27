import { Page } from '@playwright/test';
import { test, expect } from './soporte/sin-tutorial-automatico';

test('recupera la contraseña desde el ingreso y elimina el enlace de la URL', async ({ page }) => {
  const token = '00000000-0000-0000-0000-000000000000.' + 'a'.repeat(43);
  const correo = 'estudiante@example.test';
  await page.route('**/api/identidad/recuperacion', (ruta) => {
    expect(ruta.request().postDataJSON()).toEqual({ correo });
    return ruta.fulfill({ status: 202 });
  });
  let vigente = false;
  await page.route('**/api/identidad/recuperacion/restablecer', (ruta) => {
    expect(ruta.request().postDataJSON()).toEqual({ token, clave: 'ContraseñaFicticia2026!' });
    return vigente ? ruta.fulfill({ status: 204 }) : ruta.fulfill({
      status: 400, json: { detail: 'El enlace ya no es válido. Solicita uno nuevo.' },
    });
  });
  await page.goto('/ingreso');
  await page.getByRole('button', { name: '¿Olvidaste tu contraseña?' }).click();
  await page.getByLabel('Correo de la cuenta').fill(correo);
  await page.getByRole('button', { name: 'Enviar enlace de recuperación' }).click();
  await expect(page.getByRole('status')).toContainText('Si el correo corresponde a una cuenta activa');
  await page.goto('/ingreso#recuperar=' + token);
  await expect(page.getByRole('heading', { name: 'Elige una nueva contraseña.' })).toBeVisible();
  await page.getByLabel('Nueva contraseña', { exact: true }).fill('ContraseñaFicticia2026!');
  await page.getByLabel('Confirmar contraseña').fill('ContraseñaFicticia2026!');
  await page.getByRole('button', { name: 'Guardar nueva contraseña' }).click();
  await expect(page.getByRole('alert')).toContainText('El enlace ya no es válido');
  vigente = true;
  await page.getByRole('button', { name: 'Guardar nueva contraseña' }).click();
  await expect(page.getByRole('status')).toContainText('Contraseña actualizada');
  await expect(page).toHaveURL(/\/ingreso$/);
  await expect(page.getByRole('button', { name: 'Entrar a mi espacio' })).toBeVisible();
});

async function simularCuenta(pagina: Page, rol = 'DOCENTE') {
  await pagina.route('**/api/**', (ruta) => {
    if (new URL(ruta.request().url()).pathname.endsWith('/ingreso')) {
      const { correo } = ruta.request().postDataJSON();
      return ruta.fulfill({ json: { token: 'ficticio-' + correo, tipo: 'Bearer' } });
    }
    if (new URL(ruta.request().url()).pathname.endsWith('/yo')) {
      const autorizacion = ruta.request().headers()['authorization'] ?? '';
      expect(autorizacion).toMatch(/^Bearer ficticio-/);
      const correo = autorizacion.slice('Bearer ficticio-'.length);
      return ruta.fulfill({
        json: {
          id: correo === 'personal@example.test' ? 'cuenta-primera' : 'cuenta-segunda',
          nombre: correo === 'personal@example.test' ? 'Cuenta de prueba' : 'Segunda cuenta',
          correo,
          rol,
          puedeCrearCasos: rol === 'DOCENTE',
        },
      });
    }
    return ruta.fulfill({ json: [] });
  });
}
async function completarFormulario(pagina: Page, correo = 'personal@example.test') {
  await pagina.getByLabel('Correo institucional').fill(correo);
  await pagina.getByLabel('Contraseña', { exact: true }).fill('clave-ficticia');
}

test('el ingreso es la entrada inicial y las rutas internas requieren identificación', async ({
  page: pagina,
}) => {
  let solicitudes = 0;
  await pagina.route('**/api/**', (ruta) => {
    solicitudes++;
    return ruta.fulfill({ status: 401, json: {} });
  });
  await pagina.goto('/?vista=administracion');
  await expect(pagina).toHaveURL(/\/ingreso$/);
  await expect(pagina.getByRole('heading', { name: 'Tu próxima perspectiva.' })).toBeVisible();
  await expect(pagina.getByRole('navigation')).toHaveCount(0);
  await pagina.goto('/ruta-desconocida');
  await expect(pagina).toHaveURL(/\/ingreso$/);
  expect(solicitudes).toBe(0);
});

test('campos inválidos no envían solicitudes y la contraseña se puede mostrar', async ({
  page: pagina,
}) => {
  let solicitudes = 0;
  await pagina.route('**/api/**', (ruta) => {
    solicitudes++;
    return ruta.fulfill({ status: 401, json: {} });
  });
  await pagina.goto('/ingreso');
  const ojo = pagina.getByRole('button', { name: 'Mostrar contraseña' });
  await expect(ojo).toBeVisible();
  await expect(ojo.locator('svg')).toBeVisible();
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await expect(pagina.getByText('Escribe un correo institucional válido.')).toBeVisible();
  await expect(pagina.getByText('Escribe tu contraseña.')).toBeVisible();
  await expect(ojo).toBeVisible();
  await pagina.getByLabel('Correo institucional').fill('correo-invalido');
  await pagina.getByLabel('Contraseña', { exact: true }).fill('una-clave');
  await pagina.getByRole('button', { name: 'Mostrar contraseña' }).click();
  await expect(pagina.getByLabel('Contraseña', { exact: true })).toHaveAttribute('type', 'text');
  await pagina.getByRole('button', { name: 'Ocultar contraseña' }).click();
  await expect(pagina.getByLabel('Contraseña', { exact: true })).toHaveAttribute(
    'type',
    'password',
  );
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  expect(solicitudes).toBe(0);
});

test('el docente entra a su espacio y el cierre de sesión impide volver con el historial', async ({
  page: pagina,
}) => {
  await simularCuenta(pagina);
  await pagina.goto('/ingreso');
  await completarFormulario(pagina);
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await expect(pagina).toHaveURL(/vista=inicio/);
  await expect(pagina.getByRole('heading', { name: /El aprendizaje empieza/ })).toBeVisible();
  await pagina.getByRole('button', { name: 'Biblioteca de casos', exact: true }).click();
  await pagina.getByRole('button', { name: 'Cerrar sesión', exact: true }).click();
  await expect(pagina).toHaveURL(/\/ingreso$/);
  await pagina.goBack();
  await expect(pagina).toHaveURL(/\/ingreso$/);
  await expect(pagina.getByRole('navigation')).toHaveCount(0);
  expect(await pagina.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
});

test('un 401 tardío de una generación anterior no cierra el nuevo ingreso', async ({
  page: pagina,
}) => {
  let retrasarListado = false;
  let liberarRespuesta!: () => void;
  let confirmarSolicitud!: () => void;
  const solicitudIniciada = new Promise<void>((resolve) => (confirmarSolicitud = resolve));
  const respuestaPendiente = new Promise<void>((resolve) => (liberarRespuesta = resolve));
  await pagina.route('**/api/**', async (ruta) => {
    const url = new URL(ruta.request().url());
    if (url.pathname.endsWith('/ingreso'))
      return ruta.fulfill({ json: { token: 'token-ficticio', tipo: 'Bearer' } });
    if (url.pathname.endsWith('/yo'))
      return ruta.fulfill({
        json: {
          id: 'admin',
          nombre: 'Administradora',
          correo: 'personal@example.test',
          rol: 'ADMINISTRADOR',
          puedeCrearCasos: false,
        },
      });
    if (url.pathname === '/api/identidad/usuarios' && retrasarListado) {
      retrasarListado = false;
      confirmarSolicitud();
      await respuestaPendiente;
      return ruta.fulfill({ status: 401, json: {} });
    }
    if (url.pathname === '/api/identidad/usuarios') return ruta.fulfill({ json: [] });
    if (url.pathname === '/api/identidad/roles')
      return ruta.fulfill({ json: ['ADMINISTRADOR', 'DOCENTE', 'ESTUDIANTE'] });
    return ruta.fulfill({ json: [] });
  });
  await pagina.goto('/ingreso');
  await completarFormulario(pagina);
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  retrasarListado = true;
  const actualizacionAnterior = pagina.getByRole('button', { name: 'Actualizar lista' }).click();
  await solicitudIniciada;
  await pagina.getByLabel('Cerrar sesión').click();
  await completarFormulario(pagina);
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await expect(pagina.getByText('Administradora', { exact: true }).first()).toBeVisible();
  liberarRespuesta();
  await actualizacionAnterior;
  await pagina.waitForTimeout(200);
  await expect(pagina).toHaveURL(/vista=administracion/);
  await expect(pagina.getByRole('navigation')).toBeVisible();
});

test('cerrar sesión y explorar la demostración restaura sus datos', async ({ page: pagina }) => {
  await simularCuenta(pagina);
  await pagina.goto('/ingreso');
  await completarFormulario(pagina);
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await pagina.getByLabel('Cerrar sesión').click();
  await pagina.getByRole('button', { name: 'Explorar la demostración' }).click();
  await pagina.getByRole('button', { name: 'Biblioteca de casos', exact: true }).click();
  await expect(pagina.getByRole('heading', { name: 'El poder de pertenecer' })).toBeVisible();
  await pagina.getByRole('button', { name: 'Sesiones', exact: true }).click();
  await expect(pagina.getByText('Aula 302')).toBeVisible();
});

test('la cuenta administrativa entra únicamente a administración', async ({ page: pagina }) => {
  await simularCuenta(pagina, 'ADMINISTRADOR');
  await pagina.goto('/ingreso');
  await completarFormulario(pagina);
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await expect(pagina).toHaveURL(/vista=administracion/);
  await expect(pagina.getByRole('heading', { name: 'Administración de usuarios.' })).toBeVisible();
  await expect(
    pagina.getByRole('button', { name: 'Biblioteca de casos', exact: true }),
  ).toHaveCount(0);
});

test('el estudiante consulta y marca sus notificaciones internas', async ({ page: pagina }) => {
  let avisoLeido = false;
  const solicitudes: { ruta: string; metodo: string; autorizacion: string }[] = [];
  const erroresNavegador: string[] = [];
  pagina.on('console', (mensaje) => {
    if (mensaje.type() === 'error') erroresNavegador.push(mensaje.text());
  });
  pagina.on('pageerror', (error) => erroresNavegador.push(error.message));
  await pagina.route('**/api/**', async (ruta) => {
    const solicitud = ruta.request();
    const url = new URL(solicitud.url());
    solicitudes.push({
      ruta: url.pathname + url.search,
      metodo: solicitud.method(),
      autorizacion: solicitud.headers()['authorization'] ?? '',
    });
    if (url.pathname === '/api/identidad/ingreso')
      return ruta.fulfill({ json: { token: 'token-estudiante', tipo: 'Bearer' } });
    if (url.pathname === '/api/identidad/yo')
      return ruta.fulfill({
        json: {
          id: 'estudiante',
          nombre: 'Estudiante de prueba',
          correo: 'estudiante@example.test',
          rol: 'ESTUDIANTE',
          puedeCrearCasos: false,
        },
      });
    if (url.pathname === '/api/estudiante/actividades') return ruta.fulfill({ json: [] });
    if (url.pathname === '/api/estudiante/resultados') return ruta.fulfill({ json: [] });
    if (url.pathname === '/api/notificaciones/no-leidas') return ruta.fulfill({ json: avisoLeido ? 0 : 1 });
    if (url.pathname === '/api/notificaciones' && solicitud.method() === 'GET')
      return ruta.fulfill({
        json: [
          {
            id: 'aviso-nuevo',
            idSesion: 'sesion-1',
            idIntento: 'intento-1',
            tipo: 'RESULTADO',
            mensaje: 'Tu resultado ya está disponible.',
            fechaCreacion: '2026-11-21T15:00:00Z',
            leida: avisoLeido,
          },
          {
            id: 'aviso-leido',
            idSesion: 'sesion-1',
            idIntento: 'intento-1',
            tipo: 'VALORACION',
            mensaje: 'Tienes una nueva valoración docente.',
            fechaCreacion: '2026-11-20T15:00:00Z',
            leida: true,
          },
        ],
      });
    if (url.pathname === '/api/notificaciones/aviso-nuevo/leida' && solicitud.method() === 'PUT') {
      avisoLeido = true;
      return ruta.fulfill({ status: 204 });
    }
    return ruta.fulfill({ status: 404, json: { detail: 'Ruta no simulada' } });
  });

  await pagina.goto('/ingreso');
  await pagina.getByLabel('Correo institucional').fill('estudiante@example.test');
  await pagina.getByLabel('Contraseña', { exact: true }).fill('clave-ficticia');
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  const notificaciones = pagina.getByRole('button', { name: 'Notificaciones, 1 sin leer', exact: true });
  await notificaciones.click();

  await expect(pagina.getByRole('heading', { name: 'Notificaciones' })).toBeVisible();
  await expect(pagina.getByText('Tu resultado ya está disponible.')).toBeVisible();
  await pagina.getByRole('button', { name: 'Marcar como leída' }).click();
  await expect(pagina.getByRole('button', { name: 'Marcar como leída' })).toHaveCount(0);
  await expect(pagina.getByRole('button', { name: 'Notificaciones', exact: true })).toBeVisible();

  expect(solicitudes).toContainEqual({
    ruta: '/api/notificaciones?limite=100&desplazamiento=0',
    metodo: 'GET',
    autorizacion: 'Bearer token-estudiante',
  });
  expect(solicitudes).toContainEqual({
    ruta: '/api/notificaciones/aviso-nuevo/leida',
    metodo: 'PUT',
    autorizacion: 'Bearer token-estudiante',
  });
  expect(erroresNavegador).toEqual([]);
});

test('al cambiar de cuenta se reinician consultas y programación', async ({ page: pagina }) => {
  await simularCuenta(pagina);
  await pagina.route('**/api/sesiones', (ruta) =>
    ruta.fulfill({
      json: [
        {
          id: 'sesion-cuenta',
          idCaso: 'caso-cuenta',
          idEspacio: 'espacio-cuenta',
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
    }),
  );
  await pagina.goto('/ingreso');
  await completarFormulario(pagina);
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();

  await pagina.getByRole('button', { name: 'Biblioteca de casos', exact: true }).click();
  await pagina.getByLabel('Buscar casos por nombre o tema').fill('consulta de la primera cuenta');
  await pagina.getByRole('button', { name: 'Sesiones', exact: true }).click();
  await pagina.getByLabel('Buscar sesiones').fill('sesión de la primera cuenta');
  await pagina.getByRole('button', { name: 'Programar sesión' }).click();
  await pagina.getByLabel('Lugar').fill('Aula de la primera cuenta');
  await pagina.getByLabel('Mensaje de convocatoria').fill('Mensaje privado de la primera cuenta');
  await pagina.keyboard.press('Escape');
  await pagina.getByRole('button', { name: 'Resultados', exact: true }).click();
  await pagina.getByLabel('Sesión para exportar').selectOption('sesion-cuenta');

  await pagina.getByRole('button', { name: 'Mi cuenta' }).click();
  await pagina
    .getByRole('dialog')
    .getByRole('button', { name: 'Cerrar sesión', exact: true })
    .click();
  await completarFormulario(pagina, 'segunda@example.test');
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await expect(pagina.getByText('Segunda cuenta', { exact: true })).toBeVisible();

  await pagina.getByRole('button', { name: 'Biblioteca de casos', exact: true }).click();
  await expect(pagina.getByLabel('Buscar casos por nombre o tema')).toHaveValue('');
  await pagina.getByRole('button', { name: 'Sesiones', exact: true }).click();
  await expect(pagina.getByLabel('Buscar sesiones')).toHaveValue('');
  await pagina.getByRole('button', { name: 'Programar sesión' }).click();
  await expect(pagina.getByLabel('Lugar')).toHaveValue('');
  await expect(pagina.getByLabel('Mensaje de convocatoria')).toHaveValue('');
  await pagina.keyboard.press('Escape');
  await pagina.getByRole('button', { name: 'Resultados', exact: true }).click();
  await expect(pagina.getByLabel('Sesión para exportar')).toHaveValue('');
});

test('un 401 de la cuenta activa cierra la sesión y un 403 no lo hace', async ({
  page: pagina,
}) => {
  let estado: 'valida' | 'prohibida' | 'revocada' = 'valida';
  await pagina.route('**/api/**', (ruta) => {
    const url = new URL(ruta.request().url());
    if (url.pathname.endsWith('/ingreso'))
      return ruta.fulfill({ json: { token: 'token-ficticio', tipo: 'Bearer' } });
    if (url.pathname.endsWith('/yo'))
      return ruta.fulfill({
        json: {
          id: 'cuenta-prueba',
          nombre: 'Cuenta de prueba',
          correo: 'personal@example.test',
          rol: 'DOCENTE',
          puedeCrearCasos: true,
        },
      });
    if (['/api/academico/espacios', '/api/casos', '/api/sesiones'].includes(url.pathname))
      return ruta.fulfill({ json: [] });
    if (url.pathname === '/api/notificaciones' && estado === 'valida')
      return ruta.fulfill({ json: [] });
    if (estado === 'prohibida') return ruta.fulfill({ status: 403, json: {} });
    if (estado === 'revocada') return ruta.fulfill({ status: 401, json: {} });
    return ruta.fulfill({ json: {} });
  });
  await pagina.goto('/ingreso');
  await completarFormulario(pagina);
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await pagina.getByRole('button', { name: /Notificaciones/ }).click();

  estado = 'prohibida';
  await pagina.getByRole('button', { name: 'Actualizar' }).click();
  await expect(pagina.getByRole('alert')).toHaveText(
    'Tu cuenta no tiene permiso para esta operación.',
  );
  await expect(pagina).toHaveURL(/vista=notificaciones/);
  await expect(pagina.getByRole('navigation')).toBeVisible();

  estado = 'revocada';
  await pagina.getByRole('button', { name: 'Actualizar' }).click();
  await expect(pagina).toHaveURL(/\/ingreso$/);
  await expect(pagina.getByRole('navigation')).toHaveCount(0);
});

test('la recarga restaura la vista con el token de la pestaña hasta cerrar sesión', async ({
  page: pagina,
}) => {
  await simularCuenta(pagina);
  await pagina.goto('/ingreso');
  await completarFormulario(pagina);
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await expect(pagina.getByRole('button', { name: 'Mi cuenta' })).toBeVisible();
  await pagina.getByRole('button', { name: /Notificaciones/ }).click();
  await expect(pagina).toHaveURL(/vista=notificaciones/);
  expect(await pagina.evaluate(() => sessionStorage.getItem('psicoapp.token'))).toBe(
    'ficticio-personal@example.test',
  );
  await pagina.reload();
  await expect(pagina).toHaveURL(/vista=notificaciones/);
  await expect(pagina.getByRole('button', { name: 'Mi cuenta' })).toBeVisible();
  await pagina.getByLabel('Cerrar sesión').click();
  await expect(pagina).toHaveURL(/\/ingreso$/);
  expect(await pagina.evaluate(() => sessionStorage.getItem('psicoapp.token'))).toBeNull();
  await pagina.reload();
  await expect(pagina).toHaveURL(/\/ingreso$/);
  await expect(pagina.getByLabel('Contraseña', { exact: true })).toHaveValue('');
  await expect(pagina.getByRole('navigation')).toHaveCount(0);
});

test('evita doble envío y limpia la contraseña al fallar el acceso', async ({ page: pagina }) => {
  let solicitudes = 0;
  let resolver!: () => void;
  const respuestaPendiente = new Promise<void>((resolve) => {
    resolver = resolve;
  });
  await pagina.route('**/api/identidad/ingreso', async (ruta) => {
    solicitudes++;
    expect(ruta.request().headers()['authorization'] ?? '').toBe('');
    expect(ruta.request().postDataJSON()).toEqual({
      correo: 'personal@example.test',
      clave: 'clave-ficticia',
    });
    await respuestaPendiente;
    await ruta.fulfill({ status: 401, json: {} });
  });
  await pagina.goto('/ingreso');
  await completarFormulario(pagina);
  await pagina.getByRole('button', { name: 'Mostrar contraseña' }).click();
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await expect(pagina.getByRole('button', { name: 'Iniciando sesión…' })).toBeDisabled();
  const ocultar = pagina.getByRole('button', { name: 'Ocultar contraseña' });
  await expect(ocultar).toBeVisible();
  await expect(ocultar).toBeDisabled();
  await expect(pagina.getByLabel('Correo institucional')).toHaveAttribute('readonly', '');
  await pagina.getByLabel('Contraseña', { exact: true }).press('Enter');
  resolver();
  await expect(pagina.getByRole('alert')).toHaveText('Correo o clave incorrectos.');
  await expect(pagina.getByRole('alert')).toBeFocused();
  await expect(pagina.getByLabel('Contraseña', { exact: true })).toHaveValue('');
  const mostrar = pagina.getByRole('button', { name: 'Mostrar contraseña' });
  await expect(mostrar).toBeVisible();
  await expect(mostrar).toBeEnabled();
  await expect(mostrar.locator('svg')).toBeVisible();
  expect(solicitudes).toBe(1);
});

test('el login se adapta a móvil y escritorio sin desbordamiento', async ({ page: pagina }) => {
  await pagina.setViewportSize({ width: 390, height: 844 });
  await pagina.goto('/ingreso');
  await expect(pagina.getByRole('button', { name: 'Entrar a mi espacio' })).toBeVisible();
  expect(await pagina.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
    390,
  );
  await pagina.screenshot({ path: 'capturas/ingreso-movil.png', fullPage: true });
  await pagina.setViewportSize({ width: 1440, height: 1000 });
  await pagina.screenshot({ path: 'capturas/ingreso-escritorio.png', fullPage: true });
});

test('una página HTML no se interpreta como identidad ni expone el error de JSON', async ({
  page: pagina,
}) => {
  await pagina.route('**/api/identidad/ingreso', (ruta) =>
    ruta.fulfill({
      status: 200,
      contentType: 'text/html',
      body: '<!doctype html><html><body>Frontend</body></html>',
    }),
  );
  await pagina.goto('/ingreso');
  await completarFormulario(pagina);
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await expect(pagina.getByRole('alert')).toHaveText(
    'La conexión devolvió una página web en lugar de datos de la plataforma. Reinicia el frontend y verifica su conexión con el backend.',
  );
  await expect(pagina.getByRole('navigation')).toHaveCount(0);
  await expect(pagina.getByLabel('Contraseña', { exact: true })).toHaveValue('');
});

test('un JSON inválido muestra un mensaje legible', async ({ page: pagina }) => {
  await pagina.route('**/api/identidad/ingreso', (ruta) =>
    ruta.fulfill({ status: 200, contentType: 'application/json', body: '{incorrecto' }),
  );
  await pagina.goto('/ingreso');
  await completarFormulario(pagina);
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await expect(pagina.getByRole('alert')).toHaveText(
    'El servicio devolvió datos inválidos. Vuelve a intentar en unos momentos.',
  );
});

test('un error de proxy sin JSON informa que el backend no está disponible', async ({
  page: pagina,
}) => {
  await pagina.route('**/api/identidad/ingreso', (ruta) =>
    ruta.fulfill({ status: 502, contentType: 'text/html', body: '<html>Error de conexión</html>' }),
  );
  await pagina.goto('/ingreso');
  await completarFormulario(pagina);
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await expect(pagina.getByRole('alert')).toHaveText(
    'No se pudo conectar con el backend. Comprueba que esté activo y vuelve a intentar.',
  );
});

for (const conservar of [false, true]) {
  test(conservar ? 'permite conservar la contraseña inicial con una confirmación explícita'
    : 'la contraseña inicial se cambia antes de consultar el espacio estudiantil', async ({
    page: pagina,
  }) => {
    let ingresos = 0;
    const claveInicial = 'ClaveInicial2026!';
    const nuevaClave = conservar ? claveInicial : 'ClavePersonal2026!';
    const solicitudes: { ruta: string; metodo: string; datos: any }[] = [];
    await pagina.route('**/api/**', async (ruta) => {
      const solicitud = ruta.request();
      const url = new URL(solicitud.url());
      solicitudes.push({
        ruta: url.pathname,
        metodo: solicitud.method(),
        datos: solicitud.postData() ? solicitud.postDataJSON() : null,
      });
      if (url.pathname === '/api/identidad/ingreso') {
        ingresos++;
        return ruta.fulfill({
          json: { token: `token-${ingresos}`, cambioClaveInicial: ingresos === 1 },
        });
      }
      if (url.pathname === '/api/identidad/yo')
        return ruta.fulfill({
          json: {
            id: 'estudiante', nombre: 'Estudiante', correo: 'estudiante@example.test',
            rol: 'ESTUDIANTE', puedeCrearCasos: false,
          },
        });
      if (url.pathname === '/api/identidad/clave-inicial' && solicitud.method() === 'PUT') {
        const datos = solicitud.postDataJSON();
        if (datos.clave === claveInicial && datos.conservarClaveInicial !== true) {
          return ruta.fulfill({ status: 409, json: { codigo: 'CONFIRMAR_CLAVE_INICIAL' } });
        }
        return ruta.fulfill({ status: 204 });
      }
      if (url.pathname === '/api/estudiante/actividades') return ruta.fulfill({ json: [] });
      if (url.pathname === '/api/estudiante/resultados') return ruta.fulfill({ json: [] });
      if (url.pathname === '/api/notificaciones') return ruta.fulfill({ json: [] });
      return ruta.fulfill({ status: 404, json: { detail: 'Ruta no simulada' } });
    });

    await pagina.goto('/ingreso');
    await pagina.getByLabel('Correo institucional').fill('estudiante@example.test');
    await pagina.getByLabel('Contraseña', { exact: true }).fill(claveInicial);
    await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
    await expect(pagina.getByLabel('Nueva contraseña')).toBeVisible();
    expect(solicitudes.some((solicitud) => solicitud.ruta === '/api/estudiante/actividades')).toBeFalsy();

    const campoNueva = pagina.getByLabel('Nueva contraseña');
    await campoNueva.fill(nuevaClave);
    await pagina.getByLabel('Confirmar contraseña').fill(nuevaClave);
    const guardar = pagina.getByRole('button', { name: 'Guardar y continuar' });
    await guardar.click();
    if (conservar) {
      const aviso = pagina.getByRole('dialog', { name: '¿Conservar tu contraseña inicial?' });
      const elegir = aviso.getByRole('button', { name: 'Elegir otra contraseña' });
      await expect(elegir).toBeFocused();
      await elegir.click();
      await expect(campoNueva).toBeFocused();
      expect(ingresos).toBe(1);
      expect(solicitudes.filter((solicitud) => solicitud.metodo === 'PUT').map((solicitud) => solicitud.datos))
        .toEqual([{ clave: claveInicial }]);
      await pagina.setViewportSize({ width: 390, height: 844 });
      await guardar.click();
      await expect(aviso).toBeInViewport();
      await aviso.getByRole('button', { name: 'Conservar contraseña' }).click();
    }
    await expect(pagina.getByRole('heading', { name: '¡Bienvenido!' })).toBeVisible();
    expect(ingresos).toBe(2);
    expect(solicitudes).toContainEqual({
      ruta: '/api/identidad/clave-inicial', metodo: 'PUT',
      datos: conservar ? { clave: nuevaClave, conservarClaveInicial: true } : { clave: nuevaClave },
    });
    expect(solicitudes.filter((solicitud) => solicitud.ruta === '/api/identidad/ingreso').at(-1)?.datos.clave).toBe(nuevaClave);
  });
}

test('la ruta real de API no devuelve la página de Angular', async ({ request: solicitud }) => {
  // Sin credenciales ni escrituras: también admite el backend detenido (error de proxy).
  const respuesta = await solicitud.get('/api/identidad/yo');
  if (respuesta.ok())
    expect(respuesta.headers()['content-type']).toMatch(/application\/json|\+json/);
  else expect([401, 403, 500, 502, 503, 504]).toContain(respuesta.status());
  expect(await respuesta.text()).not.toMatch(/<psico-raiz|<!doctype html>/i);
});
