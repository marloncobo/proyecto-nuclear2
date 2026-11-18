import { Component, computed, signal, inject, HostListener } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Administracion } from './administracion';
import { ServicioApi, Caso, Sesion, Espacio, Reporte, Usuario } from './servicio-api';
@Component({
  selector: 'psico-raiz',
  imports: [FormsModule, DatePipe, Administracion],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class Aplicacion {
  readonly api = inject(ServicioApi);
  private readonly enrutador = inject(Router);
  private readonly ruta = inject(ActivatedRoute);
  readonly vista = signal('inicio');
  readonly menu = computed(() =>
    this.api.usuario()?.rol === 'ADMINISTRADOR'
      ? [{ id: 'administracion', nombre: 'Administración', icono: '◇' }]
      : [
          { id: 'inicio', nombre: 'Mi espacio', icono: '◈' },
          { id: 'casos', nombre: 'Biblioteca de casos', icono: '▤' },
          { id: 'sesiones', nombre: 'Sesiones', icono: '◷' },
          { id: 'espacios', nombre: 'Espacios académicos', icono: '◇' },
          { id: 'resultados', nombre: 'Resultados', icono: '↗' },
        ],
  );
  readonly casos = signal<Caso[]>(this.casosEjemplo());
  readonly sesiones = signal<Sesion[]>([
    {
      id: 'demo-sesion',
      idCaso: 'demo-1',
      idEspacio: 'demo-espacio',
      inicioPrevisto: '2026-11-18T14:00:00-05:00',
      finPrevisto: '2026-11-18T15:00:00-05:00',
      duracionMaximaMin: 40,
      estado: 'SIN_INICIAR',
      modoNavegacion: 'FLEXIBLE',
      lugar: 'Aula 302',
      mensaje: 'Una mirada a las decisiones que tomamos en grupo.',
      convocatorias: [],
    },
  ]);
  readonly espacios = signal<Espacio[]>([
    { id: 'demo-espacio', nombre: 'Psicología social', periodo: '2026-2' },
  ]);
  readonly busqueda = signal('');
  readonly filtro = signal('TODOS');
  readonly casosVisibles = computed(() =>
    this.casos().filter(
      (caso) =>
        caso.nombre.toLocaleLowerCase().includes(this.busqueda().toLocaleLowerCase()) &&
        (this.filtro() === 'TODOS' || caso.estado === this.filtro()),
    ),
  );
  readonly publicados = computed(
    () => this.casos().filter((caso) => caso.estado === 'PUBLICADO').length,
  );
  readonly modal = signal('');
  readonly seleccionado = signal<Caso | null>(null);
  readonly sesionPendiente = signal<Sesion | null>(null);
  readonly ocupado = signal(false);
  readonly aviso = signal('');
  readonly error = signal('');
  private focoAnterior: HTMLElement | null = null;
  readonly resultado = signal<Reporte[] | null>(null);
  readonly espacioEditado = signal<Espacio | null>(null);
  readonly estudianteEncontrado = signal<Usuario | null>(null);
  correoBusqueda = '';
  correo = '';
  clave = '';
  identificador = '';
  idEstudiante = '';
  nombreEspacio = '';
  periodo = '2026-2';
  formulario = this.formularioVacio();
  programacion = {
    idCaso: '',
    idEspacio: '',
    inicioPrevisto: '',
    finPrevisto: '',
    duracionMaximaMin: 30,
    modoNavegacion: 'FLEXIBLE',
    lugar: '',
    mensaje: '',
    estudiantes: '',
  };
  constructor() {
    this.ruta.queryParamMap.subscribe((parametros) => {
      const vista = parametros.get('vista') ?? 'inicio';
      this.vista.set(this.menu().some((item) => item.id === vista) ? vista : 'inicio');
    });
  }
  navegar(vista: string) {
    void this.enrutador.navigate([], { queryParams: { vista } });
    this.busqueda.set('');
    this.error.set('');
  }
  abrir(tipo: string, caso: Caso | null = null) {
    if (!this.modal()) this.focoAnterior = document.activeElement as HTMLElement;
    this.seleccionado.set(caso);
    this.error.set('');
    this.modal.set(tipo);
    if (tipo === 'espacio') {
      this.espacioEditado.set(null);
      this.nombreEspacio = '';
      this.periodo = '2026-2';
    }
    if (tipo === 'programar') this.estudianteEncontrado.set(null);
    if (tipo === 'crear') this.formulario = this.formularioVacio();
    setTimeout(() => document.querySelector<HTMLElement>('.modal')?.focus());
  }
  cerrar() {
    if (!this.ocupado()) {
      this.modal.set('');
      this.error.set('');
      this.clave = '';
      this.focoAnterior?.focus();
    }
  }
  @HostListener('document:keydown.escape')
  cerrarConEscape() {
    if (this.modal()) this.cerrar();
  }
  mantenerFoco(evento: Event) {
    const teclado = evento as KeyboardEvent;
    const elementos = Array.from(
      document.querySelectorAll<HTMLElement>(
        '.modal button:not(:disabled), .modal input, .modal textarea, .modal select',
      ),
    );
    const primero = elementos[0];
    const ultimo = elementos.at(-1);
    if (
      teclado.shiftKey &&
      (document.activeElement === primero ||
        document.activeElement === document.querySelector('.modal'))
    ) {
      evento.preventDefault();
      ultimo?.focus();
    } else if (!teclado.shiftKey && document.activeElement === ultimo) {
      evento.preventDefault();
      primero?.focus();
    }
  }
  private formularioVacio() {
    return {
      nombre: '',
      contexto: '',
      desarrolloSituacional: '',
      tiempoEstimadoMin: 40,
      titulo: '',
      contextoSituacional: '',
      enunciado: '',
      correcta: '',
      incorrecta: '',
      justificacionCorrecta: '',
      justificacionIncorrecta: '',
    };
  }
  async ejecutar(accion: () => Promise<void>) {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.error.set('');
    try {
      await accion();
    } catch (error) {
      this.error.set(
        error instanceof Error ? error.message : 'No fue posible completar la operación.',
      );
    } finally {
      this.ocupado.set(false);
    }
  }
  async ingresar() {
    await this.ejecutar(async () => {
      await this.api.ingresar(this.correo, this.clave);
      this.clave = '';
      this.casos.set([]);
      this.sesiones.set([]);
      this.espacios.set([]);
      this.resultado.set(null);
      this.estudianteEncontrado.set(null);
      this.correoBusqueda = '';
      this.programacion.estudiantes = '';
      this.espacioEditado.set(null);
      this.modal.set('');
      this.aviso.set('Conexión establecida. Consulta tus casos y sesiones por su identificador.');
      if (this.api.usuario()?.rol === 'ADMINISTRADOR') {
        this.aviso.set('Conexión establecida. Gestiona los usuarios y permisos de la institución.');
        this.navegar('administracion');
      } else {
        this.navegar('inicio');
        this.espacios.set(await this.api.consultar<Espacio[]>('/academico/espacios'));
      }
    });
  }
  salir() {
    if (this.ocupado()) return;
    this.api.salir();
    this.estudianteEncontrado.set(null);
    this.correoBusqueda = '';
    this.programacion.estudiantes = '';
    this.espacioEditado.set(null);
    this.modal.set('');
    this.error.set('');
    this.casos.set(this.casosEjemplo());
    this.sesiones.set([]);
    this.espacios.set([]);
    this.resultado.set(null);
    this.aviso.set('Estás en modo demostración. Los datos son ficticios.');
    this.navegar('inicio');
  }
  async guardarCaso() {
    await this.ejecutar(async () => {
      const datos = this.formulario;
      const idPregunta = crypto.randomUUID();
      const caso: Caso = {
        id: crypto.randomUUID(),
        nombre: datos.nombre,
        contexto: datos.contexto,
        desarrolloSituacional: datos.desarrolloSituacional,
        tiempoEstimadoMin: datos.tiempoEstimadoMin,
        estado: 'BORRADOR',
        escenarios: [
          {
            id: crypto.randomUUID(),
            titulo: datos.titulo,
            contextoSituacional: datos.contextoSituacional,
            orden: 1,
            preguntas: [
              {
                id: idPregunta,
                enunciado: datos.enunciado,
                orden: 1,
                opciones: [
                  {
                    id: crypto.randomUUID(),
                    texto: datos.correcta,
                    esCorrecta: true,
                    justificacion: datos.justificacionCorrecta,
                  },
                  {
                    id: crypto.randomUUID(),
                    texto: datos.incorrecta,
                    esCorrecta: false,
                    justificacion: datos.justificacionIncorrecta,
                  },
                ],
              },
            ],
          },
        ],
        rubrica: {
          id: crypto.randomUUID(),
          nombre: 'Comprensión del escenario',
          escalaMaxima: 5,
          criterios: [
            {
              id: crypto.randomUUID(),
              nombre: 'Análisis de la situación',
              ponderacion: 1,
              preguntas: [idPregunta],
            },
          ],
        },
      };
      const creado = this.api.conectado() ? await this.api.enviar<Caso>('/casos', caso) : caso;
      this.casos.update((casos) => [creado, ...casos]);
      this.modal.set('');
      this.navegar('casos');
      this.aviso.set(
        this.api.conectado()
          ? 'Borrador guardado en el servidor.'
          : 'Borrador de demostración creado en memoria.',
      );
    });
  }
  async cambiarEstado(caso: Caso, accion: 'publicar' | 'archivar') {
    await this.ejecutar(async () => {
      if (this.api.conectado()) await this.api.enviar(`/casos/${caso.id}/${accion}`, {});
      const actualizado = { ...caso, estado: accion === 'publicar' ? 'PUBLICADO' : 'ARCHIVADO' };
      this.casos.update((casos) =>
        casos.map((elemento) => (elemento.id === caso.id ? actualizado : elemento)),
      );
      this.seleccionado.set(actualizado);
      this.aviso.set(
        accion === 'publicar'
          ? 'Caso publicado. Ya puedes programar una sesión.'
          : 'Caso archivado. Su contenido se conserva.',
      );
    });
  }
  async consultarId(tipo: 'casos' | 'sesiones') {
    await this.ejecutar(async () => {
      if (!this.identificador.trim()) throw new Error('Escribe un identificador.');
      const dato = await this.api.consultar<Caso & Sesion>(
        `/${tipo}/${encodeURIComponent(this.identificador.trim())}`,
      );
      if (tipo === 'casos')
        this.casos.update((casos) => [dato, ...casos.filter((caso) => caso.id !== dato.id)]);
      else
        this.sesiones.update((sesiones) => [
          dato,
          ...sesiones.filter((sesion) => sesion.id !== dato.id),
        ]);
      this.identificador = '';
      this.aviso.set('Consulta completada.');
    });
  }
  editarEspacio(espacio: Espacio) {
    this.abrir('espacio');
    this.espacioEditado.set(espacio);
    this.nombreEspacio = espacio.nombre;
    this.periodo = espacio.periodo;
  }
  async buscarEstudiante() {
    await this.ejecutar(async () => {
      this.estudianteEncontrado.set(null);
      this.estudianteEncontrado.set(await this.api.buscarEstudiante(this.correoBusqueda));
    });
  }
  agregarEstudiante() {
    const estudiante = this.estudianteEncontrado();
    if (!estudiante) return;
    const estudiantes = this.programacion.estudiantes
      .split(',')
      .map((valor) => valor.trim())
      .filter(Boolean);
    this.programacion.estudiantes = [...new Set([...estudiantes, estudiante.id])].join(', ');
    this.estudianteEncontrado.set(null);
    this.correoBusqueda = '';
  }
  async guardarEspacio() {
    await this.ejecutar(async () => {
      const datos = { nombre: this.nombreEspacio, periodo: this.periodo };
      const anterior = this.espacioEditado();
      if (
        anterior &&
        !this.api.conectado() &&
        this.sesiones().some((sesion) => sesion.idEspacio === anterior.id)
      )
        throw new Error('El espacio ya está asociado a una sesión.');
      const espacio = this.api.conectado()
        ? anterior
          ? await this.api.editarEspacio(anterior.id, datos)
          : await this.api.enviar<Espacio>('/academico/espacios', datos)
        : { ...datos, id: anterior?.id ?? crypto.randomUUID() };
      this.espacios.update((espacios) =>
        anterior
          ? espacios.map((actual) => (actual.id === espacio.id ? espacio : actual))
          : [...espacios, espacio],
      );
      this.modal.set('');
      this.nombreEspacio = '';
      this.aviso.set(anterior ? 'Espacio académico actualizado.' : 'Espacio académico creado.');
      setTimeout(() => this.focoAnterior?.focus());
    });
  }
  async programar() {
    await this.ejecutar(async () => {
      const datos = {
        ...this.programacion,
        inicioPrevisto: new Date(this.programacion.inicioPrevisto).toISOString(),
        finPrevisto: new Date(this.programacion.finPrevisto).toISOString(),
        estudiantes: this.programacion.estudiantes
          .split(',')
          .map((valor) => valor.trim())
          .filter(Boolean),
      };
      if (new Date(datos.finPrevisto) <= new Date(datos.inicioPrevisto))
        throw new Error('La fecha final debe ser posterior al inicio.');
      const sesion = this.api.conectado()
        ? await this.api.enviar<Sesion>('/sesiones', datos)
        : { ...datos, id: crypto.randomUUID(), estado: 'SIN_INICIAR', convocatorias: [] };
      this.sesiones.update((sesiones) => [...sesiones, sesion]);
      this.modal.set('');
      this.navegar('sesiones');
      this.aviso.set(
        'Sesión programada. En conexión real, el servidor gestiona las convocatorias.',
      );
    });
  }
  async transicion(sesion: Sesion) {
    await this.ejecutar(async () => {
      const accion = sesion.estado === 'SIN_INICIAR' ? 'iniciar' : 'finalizar';
      const actualizada = this.api.conectado()
        ? await this.api.enviar<Sesion>(`/sesiones/${sesion.id}/${accion}`, {})
        : { ...sesion, estado: accion === 'iniciar' ? 'EN_CURSO' : 'FINALIZADO' };
      this.sesiones.update((sesiones) =>
        sesiones.map((elemento) => (elemento.id === sesion.id ? actualizada : elemento)),
      );
    });
  }
  async confirmarTransicion() {
    const sesion = this.sesionPendiente();
    if (sesion) {
      await this.transicion(sesion);
      if (!this.error()) this.cerrar();
    }
  }
  async consultarReporte() {
    await this.ejecutar(async () => {
      this.resultado.set(null);
      this.resultado.set(
        await this.api.consultar<Reporte[]>(
          `/reportes/casos/${encodeURIComponent(this.identificador.trim())}/estudiantes/${encodeURIComponent(this.idEstudiante.trim())}`,
        ),
      );
    });
  }
  nombreCaso(id: string) {
    return this.casos().find((caso) => caso.id === id)?.nombre ?? 'Caso ' + id;
  }
  estadoLegible(estado: string) {
    return (
      (
        {
          BORRADOR: 'Borrador',
          PUBLICADO: 'Publicado',
          ARCHIVADO: 'Archivado',
          SIN_INICIAR: 'Programada',
          EN_CURSO: 'En curso',
          FINALIZADO: 'Finalizada',
        } as Record<string, string>
      )[estado] ?? estado
    );
  }
  representar(dato: unknown) {
    return JSON.stringify(dato, null, 2);
  }
  private casosEjemplo(): Caso[] {
    return [
      {
        id: 'demo-1',
        nombre: 'El poder de pertenecer',
        contexto: 'Influencia social',
        desarrolloSituacional:
          'Un grupo universitario se enfrenta a una decisión colectiva. Explora cómo la presión del grupo transforma las elecciones individuales y el sentido de pertenencia.',
        tiempoEstimadoMin: 40,
        estado: 'PUBLICADO',
        escenarios: [],
        rubrica: null,
      },
      {
        id: 'demo-2',
        nombre: 'Más allá de la primera impresión',
        contexto: 'Percepción y cognición',
        desarrolloSituacional:
          'Una nueva integrante llega a un equipo. Analiza cómo los sesgos y las primeras impresiones influyen en la forma de relacionarse.',
        tiempoEstimadoMin: 30,
        estado: 'BORRADOR',
        escenarios: [],
        rubrica: null,
      },
      {
        id: 'demo-3',
        nombre: 'Cuando nadie interviene',
        contexto: 'Comportamiento prosocial',
        desarrolloSituacional:
          'Una situación cotidiana invita a reflexionar sobre la responsabilidad compartida, la empatía y el efecto espectador.',
        tiempoEstimadoMin: 35,
        estado: 'PUBLICADO',
        escenarios: [],
        rubrica: null,
      },
    ];
  }
}
