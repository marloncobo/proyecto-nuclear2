import { Injectable, signal } from '@angular/core';
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
  convocatorias: { id: string; idEstudiante: string; codigoAcceso: string | null }[];
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
@Injectable({ providedIn: 'root' })
export class ServicioApi {
  readonly usuario = signal<Usuario | null>(null);
  readonly conectado = signal(false);
  private autorizacion = '';
  async ingresar(correo: string, clave: string) {
    const anterior = this.autorizacion;
    this.autorizacion =
      'Basic ' +
      btoa(
        Array.from(new TextEncoder().encode(correo + ':' + clave), (valor) =>
          String.fromCharCode(valor),
        ).join(''),
      );
    try {
      const usuario = await this.consultar<Usuario>('/identidad/yo');
      if (!['DOCENTE', 'ADMINISTRADOR'].includes(usuario.rol))
        throw new Error('Esta interfaz está disponible para docentes y administradores.');
      this.usuario.set(usuario);
      this.conectado.set(true);
    } catch (error) {
      this.autorizacion = anterior;
      throw error;
    }
  }
  salir() {
    this.autorizacion = '';
    this.usuario.set(null);
    this.conectado.set(false);
  }
  consultar<T>(ruta: string) {
    return this.peticion<T>(ruta, 'GET');
  }
  listarUsuarios(rol = '', activo = '') {
    const parametros = new URLSearchParams();
    if (rol) parametros.set('rol', rol);
    if (activo) parametros.set('activo', activo);
    return this.consultar<UsuarioAdministrado[]>('/identidad/usuarios?' + parametros);
  }
  consultarUsuario(id: string) {
    return this.consultar<UsuarioAdministrado>(`/identidad/usuarios/${encodeURIComponent(id)}`);
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
  consultarPermiso(rol: Rol) {
    return this.consultar<{ habilitado: boolean }>(`/identidad/roles/${rol}/creacion-casos`);
  }
  configurarPermiso(habilitado: boolean) {
    return this.enviar<void>('/identidad/roles/DOCENTE/creacion-casos', { habilitado }, 'PUT');
  }
  buscarEstudiante(correo: string) {
    return this.consultar<Usuario>(
      '/identidad/estudiantes?' + new URLSearchParams({ correo: correo.trim() }),
    );
  }
  editarEspacio(id: string, datos: { nombre: string; periodo: string }) {
    return this.enviar<Espacio>(`/academico/espacios/${encodeURIComponent(id)}`, datos, 'PUT');
  }
  async enviar<T>(ruta: string, datos: unknown, metodo = 'POST'): Promise<T> {
    const autorizacion = this.autorizacion;
    const csrf = await this.consultar<{ nombreCabecera: string; token: string }>('/identidad/csrf');
    if (autorizacion !== this.autorizacion)
      throw new Error('La cuenta cambió. Vuelve a intentar la operación.');
    return this.peticion<T>(ruta, metodo, datos, { [csrf.nombreCabecera]: csrf.token });
  }
  private async peticion<T>(
    ruta: string,
    metodo: string,
    datos?: unknown,
    cabeceras: Record<string, string> = {},
  ): Promise<T> {
    let respuesta: Response;
    try {
      respuesta = await fetch('/api' + ruta, {
        method: metodo,
        credentials: 'same-origin',
        signal: AbortSignal.timeout(15000),
        headers: {
          Authorization: this.autorizacion,
          ...(datos !== undefined ? { 'Content-Type': 'application/json' } : {}),
          ...cabeceras,
        },
        body: datos !== undefined ? JSON.stringify(datos) : undefined,
      });
    } catch {
      throw new Error(
        'No se pudo conectar. Comprueba que el backend está disponible en el puerto 8080.',
      );
    }
    if (!respuesta.ok) {
      const problema = await respuesta.json().catch(() => null);
      const mensajes: Record<number, string> = {
        401: 'Correo o clave incorrectos.',
        403: 'Tu cuenta no tiene permiso para esta operación.',
        404: 'No se encontró el registro.',
        409: 'El estado actual no permite esta operación.',
        422: 'El caso no cumple los requisitos para publicarse.',
      };
      throw new Error(
        problema?.detail ??
          mensajes[respuesta.status] ??
          'No se pudo completar la solicitud. Revisa los datos y la conexión.',
      );
    }
    return respuesta.status === 204 ? (undefined as T) : ((await respuesta.json()) as T);
  }
}

export interface Reporte {
  participacion: { idIntento: string; estado: string; inicio: string; idSesion: string };
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
