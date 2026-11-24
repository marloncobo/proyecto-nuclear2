import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

declare global {
  interface Window {
    configuracionPsicoapp?: { urlApi?: string };
  }
}

function obtenerUrlApi(): string {
  return (window.configuracionPsicoapp?.urlApi ?? '').trim().replace(/\/$/, '');
}
export interface Opcion {
  id: string;
  texto: string;
  esCorrecta: boolean;
  justificacion: string;
}
export interface Pregunta {
  id: string;
  enunciado: string;
  orden: number;
  opciones: Opcion[];
}
export interface Escenario {
  id: string;
  titulo: string;
  contextoSituacional: string;
  orden: number;
  preguntas: Pregunta[];
}
export interface Rubrica {
  id: string;
  nombre: string;
  escalaMaxima: number;
  criterios: { id: string; nombre: string; ponderacion: number; preguntas: string[] }[];
}
export interface Caso {
  id: string;
  nombre: string;
  contexto: string;
  desarrolloSituacional: string;
  tiempoEstimadoMin: number;
  estado: string;
  escenarios: Escenario[];
  rubrica: Rubrica | null;
}
export interface ErrorValidacionCaso {
  codigo: string;
  ruta: string;
  mensaje: string;
}
export class PublicacionCasoInvalida extends Error {
  constructor(
    mensaje: string,
    readonly errores: ErrorValidacionCaso[],
  ) {
    super(mensaje);
    this.name = 'PublicacionCasoInvalida';
  }
}
export interface Sesion {
  id: string;
  idCaso: string;
  idEspacio: string;
  inicioPrevisto: string;
  finPrevisto: string;
  duracionMaximaMin: number;
  estado: string;
  modoNavegacion: string;
  lugar: string;
  mensaje: string;
  convocatorias: {
    id: string;
    idEstudiante: string;
    codigoAcceso: string | null;
    autorizada?: boolean;
  }[];
}
export interface Espacio {
  id: string;
  nombre: string;
  periodo: string;
}
export interface Usuario {
  id: string;
  nombre: string;
  correo: string;
  rol: string;
  puedeCrearCasos: boolean;
}
export type Rol = 'ADMINISTRADOR' | 'DOCENTE' | 'ESTUDIANTE';
export interface UsuarioAdministrado extends Usuario {
  activo: boolean;
}
export interface AvisoNotificacion {
  id: string;
  idSesion: string | null;
  idIntento: string | null;
  tipo: string;
  mensaje: string;
  fechaCreacion: string;
  leida: boolean;
}
@Injectable({ providedIn: 'root' })
export class ServicioApi {
  private readonly enrutador = inject(Router);
  private readonly urlApi = obtenerUrlApi();
  readonly usuario = signal<Usuario | null>(null);
  readonly conectado = signal(false);
  private autorizacion = '';
  private generacionSesion = 0;
  async ingresar(correo: string, clave: string, estudiante = false) {
    const generacionAnterior = this.generacionSesion;
    const generacionIngreso = ++this.generacionSesion;
    const anterior = this.autorizacion;
    try {
      const ingreso = await this.peticion<{ token: string }>('/identidad/ingreso', 'POST', {
        correo,
        clave,
      });
      if (this.generacionSesion !== generacionIngreso)
        throw new Error('La cuenta cambió. Vuelve a intentar la operación.');
      if (typeof ingreso.token !== 'string' || ingreso.token.trim() === '')
        throw new Error(
          'El servicio devolvió datos inválidos. Vuelve a intentar en unos momentos.',
        );
      this.autorizacion = 'Bearer ' + ingreso.token;
      const usuario = await this.consultar<Usuario>('/identidad/yo');
      if (this.generacionSesion !== generacionIngreso)
        throw new Error('La cuenta cambió. Vuelve a intentar la operación.');
      if (
        estudiante
          ? usuario.rol !== 'ESTUDIANTE'
          : !['DOCENTE', 'ADMINISTRADOR'].includes(usuario.rol)
      )
        throw new Error(
          estudiante
            ? 'Usa la cuenta estudiantil indicada en la convocatoria.'
            : 'Esta interfaz está disponible para docentes y administradores.',
        );
      this.usuario.set(usuario);
      this.conectado.set(true);
    } catch (error) {
      if (this.generacionSesion === generacionIngreso) {
        this.autorizacion = anterior;
        this.generacionSesion = generacionAnterior;
      }
      throw error;
    }
  }
  salir() {
    this.generacionSesion++;
    this.autorizacion = '';
    this.usuario.set(null);
    this.conectado.set(false);
  }
  consultar<T>(ruta: string) {
    return this.peticion<T>(ruta, 'GET');
  }
  listarUsuarios(rol = '', activo = '', busqueda = '') {
    const parametros = new URLSearchParams();
    if (rol) parametros.set('rol', rol);
    if (activo) parametros.set('activo', activo);
    if (busqueda.trim()) parametros.set('busqueda', busqueda.trim());
    return this.consultar<UsuarioAdministrado[]>('/identidad/usuarios?' + parametros);
  }
  consultarUsuario(id: string) {
    return this.consultar<UsuarioAdministrado>(`/identidad/usuarios/${encodeURIComponent(id)}`);
  }
  registrarDocente(datos: { nombre: string; correo: string; clave: string }) {
    return this.enviar<Usuario>('/identidad/docentes', datos);
  }
  cambiarRolUsuario(id: string, rol: Rol) {
    return this.enviar<Usuario>(
      `/identidad/usuarios/${encodeURIComponent(id)}/rol`,
      { rol },
      'PUT',
    );
  }
  editarDocente(id: string, datos: { correo: string; nombre: string }) {
    return this.enviar<UsuarioAdministrado>(
      `/identidad/docentes/${encodeURIComponent(id)}`,
      datos,
      'PUT',
    );
  }
  cambiarEstadoUsuario(id: string, activo: boolean) {
    return this.enviar<UsuarioAdministrado>(
      `/identidad/usuarios/${encodeURIComponent(id)}/estado`,
      { activo },
      'PUT',
    );
  }
  listarRoles() {
    return this.consultar<Rol[]>('/identidad/roles');
  }
  buscarEstudiante(correo: string) {
    return this.consultar<Usuario>(
      '/identidad/estudiantes?' + new URLSearchParams({ correo: correo.trim() }),
    );
  }
  consultarPermiso(rol: Rol) {
    return this.consultar<{ habilitado: boolean }>(`/identidad/roles/${rol}/creacion-casos`);
  }
  configurarPermiso(habilitado: boolean) {
    return this.enviar<void>('/identidad/roles/DOCENTE/creacion-casos', { habilitado }, 'PUT');
  }
  editarEspacio(id: string, datos: { nombre: string; periodo: string }) {
    return this.enviar<Espacio>(`/academico/espacios/${encodeURIComponent(id)}`, datos, 'PUT');
  }
  async listarNotificaciones(limite = 20, desplazamiento = 0) {
    const parametros = new URLSearchParams({
      limite: String(limite),
      desplazamiento: String(desplazamiento),
    });
    const notificaciones = await this.consultar<AvisoNotificacion[]>(`/notificaciones?${parametros}`);
    if (!Array.isArray(notificaciones))
      throw new Error('El servicio devolvió una lista de notificaciones inválida.');
    return notificaciones;
  }
  marcarNotificacionLeida(id: string) {
    return this.enviar<void>(`/notificaciones/${encodeURIComponent(id)}/leida`, undefined, 'PUT');
  }
  async enviar<T>(ruta: string, datos: unknown, metodo = 'POST'): Promise<T> {
    return this.peticion<T>(ruta, metodo, datos);
  }
  private async peticion<T>(
    ruta: string,
    metodo: string,
    datos?: unknown,
    cabeceras: Record<string, string> = {},
  ): Promise<T> {
    const autorizacion = this.autorizacion;
    const generacionSesion = this.generacionSesion;
    let respuesta: Response;
    try {
      respuesta = await fetch(this.urlApi + '/api' + ruta, {
        method: metodo,
        credentials: 'omit',
        signal: AbortSignal.timeout(15000),
        headers: {
          Accept: 'application/json',
          ...(autorizacion ? { Authorization: autorizacion } : {}),
          ...(datos !== undefined ? { 'Content-Type': 'application/json' } : {}),
          ...cabeceras,
        },
        body: datos !== undefined ? JSON.stringify(datos) : undefined,
      });
    } catch {
      throw new Error(
        'No se pudo conectar con el servicio. Comprueba que el backend está disponible.',
      );
    }
    const tipoContenido = respuesta.headers.get('content-type')?.toLowerCase() ?? '';
    const esJson = tipoContenido.includes('application/json') || tipoContenido.includes('+json');
    if (!respuesta.ok) {
      const problema = esJson ? await respuesta.json().catch(() => null) : null;
      const sesionExpirada =
        ruta !== '/identidad/ingreso' &&
        respuesta.status === 401 &&
        this.usuario() !== null &&
        generacionSesion === this.generacionSesion &&
        autorizacion !== '' &&
        autorizacion === this.autorizacion;
      if (sesionExpirada) {
        this.salir();
        await this.enrutador.navigate(['/ingreso'], { replaceUrl: true });
      }
      const mensajes: Record<number, string> = {
        401: sesionExpirada
          ? 'Tu sesión dejó de ser válida. Ingresa de nuevo.'
          : 'Correo o clave incorrectos.',
        403: 'Tu cuenta no tiene permiso para esta operación.',
        404: 'No se encontró el registro.',
        409: 'El estado actual no permite esta operación.',
        422: 'El caso no cumple los requisitos para publicarse.',
        500: 'El servicio no está disponible. Comprueba que el backend esté activo y vuelve a intentar.',
        502: 'No se pudo conectar con el backend. Comprueba que esté activo y vuelve a intentar.',
        503: 'El servicio no está disponible. Vuelve a intentar en unos momentos.',
        504: 'El backend tardó demasiado en responder. Vuelve a intentar.',
      };
      if (respuesta.status === 422 && Array.isArray(problema?.errores)) {
        const errores = problema.errores.filter(
          (error: unknown): error is ErrorValidacionCaso =>
            typeof error === 'object' &&
            error !== null &&
            typeof (error as ErrorValidacionCaso).codigo === 'string' &&
            typeof (error as ErrorValidacionCaso).ruta === 'string' &&
            typeof (error as ErrorValidacionCaso).mensaje === 'string',
        );
        if (errores.length > 0)
          throw new PublicacionCasoInvalida(
            'Este caso aún no está listo para publicarse.',
            errores,
          );
      }
      throw new Error(
        (typeof problema?.detail === 'string' ? problema.detail : null) ??
          mensajes[respuesta.status] ??
          'No se pudo completar la solicitud. Revisa los datos y la conexión.',
      );
    }
    if (respuesta.status === 204) return undefined as T;
    if (!esJson) {
      throw new Error(
        'La conexión devolvió una página web en lugar de datos de la plataforma. Reinicia el frontend y verifica su conexión con el backend.',
      );
    }
    try {
      return (await respuesta.json()) as T;
    } catch {
      throw new Error('El servicio devolvió datos inválidos. Vuelve a intentar en unos momentos.');
    }
  }
}

export interface Reporte {
  participacion: { idIntento: string; estado: string; inicio: string; fin?: string | null; idSesion: string };
  resultado: {
    calificacion: number;
    fecha: string;
    detalles: {
      idPregunta: string;
      contestada: boolean;
      correcta: boolean;
      puntos: number;
      retroalimentacion: string;
    }[];
  } | null;
  valoracionesGenerales: { id: string; mensaje: string; fecha: string }[];
}

export interface FilaReporteSesion {
  idEstudiante: string;
  nombreEstudiante: string;
  correoEstudiante: string;
  idIntento: string | null;
  estado: string;
  calificacion: number | null;
  preguntasRespondidas: number | null;
  preguntasTotales: number | null;
  ausencia: {
    fechaConsolidacion: string;
    detalles: { idPregunta: string; puntos: number; valoracion: string }[];
  } | null;
}
