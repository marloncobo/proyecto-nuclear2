import { test, expect, Page } from '@playwright/test';

async function simularCuenta(pagina: Page, rol = 'DOCENTE') {
  await pagina.route('**/api/**', (ruta) => {
    if (new URL(ruta.request().url()).pathname.endsWith('/yo')) {
      const autorizacion = ruta.request().headers()['authorization'] ?? '';
      const correo = Buffer.from(autorizacion.slice(6), 'base64').toString().split(':')[0];
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
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await expect(pagina.getByText('Escribe un correo institucional válido.')).toBeVisible();
  await expect(pagina.getByText('Escribe tu contraseña.')).toBeVisible();
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

test('al cambiar de cuenta se reinician consultas y programación', async ({ page: pagina }) => {
  await simularCuenta(pagina);
  await pagina.goto('/ingreso');
  await completarFormulario(pagina);
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();

  await pagina.getByRole('button', { name: 'Biblioteca de casos', exact: true }).click();
  await pagina.getByLabel('Consultar un caso existente').fill('caso-primera-cuenta');
  await pagina.getByRole('button', { name: 'Sesiones', exact: true }).click();
  await pagina.getByRole('button', { name: 'Programar sesión' }).click();
  await pagina.getByLabel('Lugar').fill('Aula de la primera cuenta');
  await pagina.getByLabel('Mensaje de convocatoria').fill('Mensaje privado de la primera cuenta');
  await pagina.keyboard.press('Escape');
  await pagina.getByRole('button', { name: 'Resultados', exact: true }).click();
  await pagina.getByLabel('Identificador del caso').fill('caso-reporte-primera');
  await pagina.getByLabel('Identificador del estudiante').fill('estudiante-primera');

  await pagina.getByRole('button', { name: 'Mi cuenta' }).click();
  await pagina
    .getByRole('dialog')
    .getByRole('button', { name: 'Cerrar sesión', exact: true })
    .click();
  await completarFormulario(pagina, 'segunda@example.test');
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await expect(pagina.getByText('Segunda cuenta', { exact: true })).toBeVisible();

  await pagina.getByRole('button', { name: 'Biblioteca de casos', exact: true }).click();
  await expect(pagina.getByLabel('Consultar un caso existente')).toHaveValue('');
  await pagina.getByRole('button', { name: 'Sesiones', exact: true }).click();
  await pagina.getByRole('button', { name: 'Programar sesión' }).click();
  await expect(pagina.getByLabel('Lugar')).toHaveValue('');
  await expect(pagina.getByLabel('Mensaje de convocatoria')).toHaveValue('');
  await pagina.keyboard.press('Escape');
  await pagina.getByRole('button', { name: 'Resultados', exact: true }).click();
  await expect(pagina.getByLabel('Identificador del caso')).toHaveValue('');
  await expect(pagina.getByLabel('Identificador del estudiante')).toHaveValue('');
});

test('un 401 de la cuenta activa cierra la sesión y un 403 no lo hace', async ({
  page: pagina,
}) => {
  let estado: 'valida' | 'prohibida' | 'revocada' = 'valida';
  await pagina.route('**/api/**', (ruta) => {
    const url = new URL(ruta.request().url());
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
    if (estado === 'prohibida') return ruta.fulfill({ status: 403, json: {} });
    if (estado === 'revocada') return ruta.fulfill({ status: 401, json: {} });
    return ruta.fulfill({ json: {} });
  });
  await pagina.goto('/ingreso');
  await completarFormulario(pagina);
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await pagina.getByRole('button', { name: 'Biblioteca de casos', exact: true }).click();
  await pagina.getByLabel('Consultar un caso existente').fill('caso-prueba');

  estado = 'prohibida';
  await pagina.getByRole('button', { name: /^Consultar/ }).click();
  await expect(pagina.getByRole('alert')).toHaveText('Tu cuenta no tiene permiso para esta operación.');
  await expect(pagina).toHaveURL(/vista=casos/);
  await expect(pagina.getByRole('navigation')).toBeVisible();

  estado = 'revocada';
  await pagina.getByRole('button', { name: /^Consultar/ }).click();
  await expect(pagina).toHaveURL(/\/ingreso$/);
  await expect(pagina.getByRole('navigation')).toHaveCount(0);
});

test('la recarga exige autenticación sin guardar credenciales en el navegador', async ({
  page: pagina,
}) => {
  await simularCuenta(pagina);
  await pagina.goto('/ingreso');
  await completarFormulario(pagina);
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await expect(pagina.getByRole('button', { name: 'Mi cuenta' })).toBeVisible();
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
  await pagina.route('**/api/identidad/yo', async (ruta) => {
    solicitudes++;
    expect(ruta.request().headers()['authorization']).toBe(
      'Basic ' + Buffer.from('personal@example.test:clave-ficticia').toString('base64'),
    );
    await respuestaPendiente;
    await ruta.fulfill({ status: 401, json: {} });
  });
  await pagina.goto('/ingreso');
  await completarFormulario(pagina);
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await expect(pagina.getByRole('button', { name: 'Iniciando sesión…' })).toBeDisabled();
  await expect(pagina.getByLabel('Correo institucional')).toHaveAttribute('readonly', '');
  await pagina.getByLabel('Contraseña', { exact: true }).press('Enter');
  resolver();
  await expect(pagina.getByRole('alert')).toHaveText('Correo o clave incorrectos.');
  await expect(pagina.getByRole('alert')).toBeFocused();
  await expect(pagina.getByLabel('Contraseña', { exact: true })).toHaveValue('');
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
  await pagina.route('**/api/identidad/yo', (ruta) =>
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
  await pagina.route('**/api/identidad/yo', (ruta) =>
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
  await pagina.route('**/api/identidad/yo', (ruta) =>
    ruta.fulfill({ status: 502, contentType: 'text/html', body: '<html>Error de conexión</html>' }),
  );
  await pagina.goto('/ingreso');
  await completarFormulario(pagina);
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
  await expect(pagina.getByRole('alert')).toHaveText(
    'No se pudo conectar con el backend. Comprueba que esté activo y vuelve a intentar.',
  );
});

test('la ruta real de API no devuelve la página de Angular', async ({ request: solicitud }) => {
  // Sin credenciales ni escrituras: también admite el backend detenido (error de proxy).
  const respuesta = await solicitud.get('/api/identidad/yo');
  if (respuesta.ok())
    expect(respuesta.headers()['content-type']).toMatch(/application\/json|\+json/);
  else expect([401, 403, 500, 502, 503, 504]).toContain(respuesta.status());
  expect(await respuesta.text()).not.toMatch(/<psico-raiz|<!doctype html>/i);
});
