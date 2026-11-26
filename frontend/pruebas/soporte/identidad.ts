import { Page } from '@playwright/test';

export async function simularApi(pagina: Page, rol = 'ADMINISTRADOR') {
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
    espacio: { id: 'espacio-real', nombre: 'Psicología social', periodo: '2026-2', estudiantes: [] as string[] },
    conflictoEspacio: false,
    conflictoDocente: false,
    conflictoRegistro: false,
    rechazarRol: false,
    desactivarDuranteCambioRol: false,
    falloListado: false,
    falloDetalle: false,
    notificaciones: [] as {
      id: string;
      idSesion: string | null;
      idIntento: string | null;
      tipo: string;
      mensaje: string;
      fechaCreacion: string;
      leida: boolean;
    }[],
    solicitudes: [] as {
      ruta: string;
      metodo: string;
      datos: any;
      autorizacion: string | undefined;
    }[],
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
      autorizacion: solicitud.headers()['authorization'],
    });
    if (url.pathname.endsWith('/ingreso'))
      return ruta.fulfill({ json: { token: 'token-ficticio', tipo: 'Bearer' } });
    if (url.pathname.endsWith('/yo'))
      return ruta.fulfill({
        json:
          estado.rol === 'ADMINISTRADOR'
            ? administrador
            : estado.rol === 'DOCENTE'
              ? docente
              : estudiante,
      });
    if (url.pathname === '/api/identidad/usuarios') {
      if (estado.falloListado)
        return ruta.fulfill({ status: 503, json: { detail: 'No se pudo consultar el listado' } });
      const busqueda = url.searchParams.get('busqueda')?.trim().toLowerCase() ?? '';
      return ruta.fulfill({
        json: estado.usuarios.filter(
          (usuario) =>
            (!url.searchParams.get('rol') || usuario.rol === url.searchParams.get('rol')) &&
            (!url.searchParams.get('activo') ||
              String(usuario.activo) === url.searchParams.get('activo')) &&
            (!busqueda || (usuario.nombre + ' ' + usuario.correo).toLowerCase().includes(busqueda)),
        ),
      });
    }
    if (url.pathname === '/api/identidad/roles')
      return ruta.fulfill({ json: ['ADMINISTRADOR', 'DOCENTE', 'ESTUDIANTE'] });
    if (url.pathname === '/api/notificaciones' && metodo === 'GET') {
      const inicio = Number(url.searchParams.get('desplazamiento') ?? 0);
      const limite = Number(url.searchParams.get('limite') ?? 20);
      return ruta.fulfill({ json: estado.notificaciones.slice(inicio, inicio + limite) });
    }
    if (url.pathname === '/api/notificaciones/no-leidas' && metodo === 'GET')
      return ruta.fulfill({ json: estado.notificaciones.filter((aviso) => !aviso.leida).length });
    if (url.pathname === '/api/estudiante/actividades' && metodo === 'GET')
      return ruta.fulfill({ json: [] });
    if (url.pathname === '/api/estudiante/resultados' && metodo === 'GET')
      return ruta.fulfill({ json: [] });
    if (url.pathname === '/api/notificaciones/leidas' && metodo === 'PUT') {
      estado.notificaciones.forEach((aviso) => { aviso.leida = true; });
      return ruta.fulfill({ status: 204 });
    }
    if (url.pathname.startsWith('/api/notificaciones/') && url.pathname.endsWith('/leida')) {
      const id = url.pathname.split('/')[3];
      const notificacion = estado.notificaciones.find((actual) => actual.id === id);
      if (!notificacion) return ruta.fulfill({ status: 404, json: {} });
      notificacion.leida = true;
      return ruta.fulfill({ status: 204 });
    }
    if (url.pathname === '/api/identidad/estudiantes/listado')
      return ruta.fulfill({ json: estado.usuarios.filter((usuario) => usuario.rol === 'ESTUDIANTE' && usuario.activo) });
    if (metodo === 'GET' && ['/api/casos', '/api/sesiones'].includes(url.pathname))
      return ruta.fulfill({ json: [] });
    if (url.pathname === '/api/identidad/estudiantes' && metodo === 'POST') {
      const nuevo = {
        id: 'estudiante-nuevo',
        nombre: datos.nombre,
        correo: datos.correo,
        rol: 'ESTUDIANTE',
        puedeCrearCasos: false,
        activo: true,
      };
      estado.usuarios.push(nuevo);
      return ruta.fulfill({ status: 201, json: nuevo });
    }
    if (url.pathname.startsWith('/api/identidad/usuarios/')) {
      const usuario = estado.usuarios.find((usuario) => usuario.id === url.pathname.split('/')[4]);
      if (!usuario) return ruta.fulfill({ status: 404, json: {} });
      if (metodo === 'GET' && estado.falloDetalle)
        return ruta.fulfill({ status: 503, json: { detail: 'No se pudo consultar el detalle' } });
      if (metodo === 'PUT' && url.pathname.endsWith('/creacion-casos')) {
        if (usuario.rol !== 'DOCENTE') return ruta.fulfill({ status: 400, json: {} });
        usuario.puedeCrearCasos = datos.habilitado;
        return ruta.fulfill({ json: usuario });
      }
      if (metodo === 'PUT' && url.pathname.endsWith('/rol')) {
        if (estado.rechazarRol)
          return ruta.fulfill({
            status: 400,
            json: { detail: 'El personal requiere una contraseña' },
          });
        usuario.rol = datos.rol;
        if (datos.rol !== 'DOCENTE') usuario.puedeCrearCasos = false;
        if (estado.desactivarDuranteCambioRol) usuario.activo = false;
        const { activo, ...respuesta } = usuario;
        return ruta.fulfill({ json: respuesta });
      }
      if (metodo === 'PUT') usuario.activo = datos.activo;
      return ruta.fulfill({ json: usuario });
    }
    if (url.pathname === '/api/identidad/docentes' && metodo === 'POST') {
      if (estado.conflictoRegistro)
        return ruta.fulfill({ status: 409, json: { detail: 'El correo ya está registrado' } });
      const nuevo = {
        id: 'docente-nuevo',
        nombre: datos.nombre,
        correo: datos.correo,
        rol: 'DOCENTE',
        puedeCrearCasos: false,
        activo: true,
      };
      estado.usuarios.push(nuevo);
      const { activo, ...respuesta } = nuevo;
      return ruta.fulfill({ status: 201, json: respuesta });
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

export async function ingresar(pagina: Page, correo = 'admin@example.test') {
  await pagina.goto('/');
  await pagina.getByLabel('Correo institucional').fill(correo);
  await pagina.getByLabel('Contraseña', { exact: true }).fill('clave-ficticia');
  await pagina.getByRole('button', { name: 'Entrar a mi espacio' }).click();
}
