import { Component, computed, effect, signal, inject, HostListener } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PanelNotificaciones } from './panel-notificaciones';
import { DatePipe } from '@angular/common';
import { NavigationEnd, Router } from '@angular/router';
import { Ingreso } from './ingreso';
import { EditorCaso } from './editor-caso';
import { SelectorEstudiantes } from './selector-estudiantes';
import { Administracion } from './administracion';
import { EspacioEstudiante } from './espacio-estudiante';
import { TutorialGuiado } from './tutorial-guiado';
import { pasosAplicacion, pasosModales } from './pasos-tutorial';
import {
  ServicioApi,
  Caso,
  Sesion,
  Espacio,
  Reporte,
  Usuario,
  FilaReporteSesion,
  ErrorValidacionCaso,
  PublicacionCasoInvalida,
  AvisoNotificacion,
} from './servicio-api';
import { nombreEtiqueta } from './etiqueta-usuario';
@Component({
  selector: 'psico-raiz',
  imports: [FormsModule, DatePipe, Administracion, Ingreso, EditorCaso, SelectorEstudiantes, EspacioEstudiante, TutorialGuiado, PanelNotificaciones],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class Aplicacion {
  readonly sesionEstudiante = signal<string | null>(null);
  readonly destinoEstudiante = signal<string | null>(null);
  readonly api = inject(ServicioApi);
  private readonly enrutador = inject(Router);
  readonly demostracion = signal(false);
  readonly vista = signal('inicio');
  readonly menuMovilAbierto = signal(false);
  readonly inicioTutorial = signal(false);
  readonly pasosModales = pasosModales;
  readonly pasosInterfaz = computed(() =>
    pasosAplicacion[this.api.usuario()?.rol === 'ADMINISTRADOR' ? 'administracion' : this.vista()],
  );
  readonly nombreEtiqueta = nombreEtiqueta;
  readonly menu = computed(() =>
    this.api.usuario()?.rol === 'ADMINISTRADOR'
      ? [{ id: 'administracion', nombre: 'Administración', icono: '◇' }]
      : [
          { id: 'inicio', nombre: 'Mi espacio', icono: '◈' },
          { id: 'casos', nombre: 'Biblioteca de casos', icono: '▤' },
          { id: 'sesiones', nombre: 'Sesiones', icono: '◷' },
          { id: 'espacios', nombre: 'Espacios académicos', icono: '◇' },
          { id: 'resultados', nombre: 'Resultados', icono: '↗' },
          { id: 'notificaciones', nombre: 'Notificaciones', icono: '◉' },
        ],
  );
  readonly casos = signal<Caso[]>(this.casosEjemplo());
  readonly sesiones = signal<Sesion[]>(this.sesionesEjemplo());
  readonly ahoraSesiones = signal(Date.now());
  readonly espacios = signal<Espacio[]>(this.espaciosEjemplo());
  readonly busquedaEspacios = signal('');
  readonly periodoEspacios = signal('');
  readonly periodosEspacios = computed(() =>
    [...new Set(this.espacios().map((espacio) => espacio.periodo))].sort((a, b) =>
      b.localeCompare(a, 'es'),
    ),
  );
  readonly espaciosVisibles = computed(() => {
    const consulta = this.busquedaEspacios().trim().toLocaleLowerCase('es');
    const periodo = this.periodoEspacios();
    return this.espacios().filter(
      (espacio) =>
        (!consulta || espacio.nombre.toLocaleLowerCase('es').includes(consulta)) &&
        (!periodo || espacio.periodo === periodo),
    );
  });
  readonly busqueda = signal('');
  readonly filtro = signal('TODOS');
  readonly casosVisibles = computed(() => {
    const consulta = this.busqueda().trim().toLocaleLowerCase('es');
    return this.casos().filter(
      (caso) =>
        (!consulta ||
          caso.nombre.toLocaleLowerCase('es').includes(consulta) ||
          caso.contexto.toLocaleLowerCase('es').includes(consulta)) &&
        (this.filtro() === 'TODOS' || caso.estado === this.filtro()),
    );
  });
  readonly publicados = computed(
    () => this.casos().filter((caso) => caso.estado === 'PUBLICADO').length,
  );
  readonly busquedaSesiones = signal('');
  readonly estadoSesiones = signal('');
  readonly espacioSesiones = signal('');
  private readonly sesionesPorPagina = 5;
  readonly paginaSesiones = signal(1);
  readonly sesionesVisibles = computed(() => {
    const consulta = this.busquedaSesiones().trim().toLocaleLowerCase('es');
    return this.sesiones().filter((sesion) => {
      const nombreCaso = this.nombreCaso(sesion.idCaso).toLocaleLowerCase('es');
      const lugar = (sesion.lugar ?? '').toLocaleLowerCase('es');
      return (
        (!consulta || nombreCaso.includes(consulta) || lugar.includes(consulta)) &&
        (!this.estadoSesiones() || sesion.estado === this.estadoSesiones()) &&
        (!this.espacioSesiones() || sesion.idEspacio === this.espacioSesiones())
      );
    });
  });
  readonly totalPaginasSesiones = computed(() =>
    Math.max(1, Math.ceil(this.sesionesVisibles().length / this.sesionesPorPagina)),
  );
  readonly paginaActualSesiones = computed(() =>
    Math.min(this.paginaSesiones(), this.totalPaginasSesiones()),
  );
  readonly sesionesPagina = computed(() => {
    const inicio = (this.paginaActualSesiones() - 1) * this.sesionesPorPagina;
    return this.sesionesVisibles().slice(inicio, inicio + this.sesionesPorPagina);
  });
  readonly modal = signal('');
  readonly seleccionado = signal<Caso | null>(null);
  readonly sesionPendiente = signal<Sesion | null>(null);
  readonly ocupado = signal(false);
  readonly aviso = signal('');
  readonly error = signal('');
  readonly erroresPublicacion = signal<ErrorValidacionCaso[]>([]);
  private focoAnterior: HTMLElement | null = null;
  readonly resultado = signal<Reporte[] | null>(null);
  readonly espacioEditado = signal<Espacio | null>(null);
  readonly estudiantes = signal<Usuario[]>([]);
  identificadorReporte = '';
  readonly cargandoEstudiantes = signal(false);
  readonly reporteSesion = signal<FilaReporteSesion[] | null>(null);
  readonly participantesDetalleSesion = signal<FilaReporteSesion[] | null>(null);
  readonly filtroParticipantesSesion = signal('');
  readonly participantesSesionFiltrados = computed(() => {
    const consulta = this.filtroParticipantesSesion().trim().toLocaleLowerCase('es');
    const participantes = this.participantesDetalleSesion() ?? [];
    if (!consulta) return participantes;
    return participantes.filter((fila) =>
      [fila.nombreEstudiante, fila.correoEstudiante].some(
        (dato) => dato.toLocaleLowerCase('es').includes(consulta),
      ),
    );
  });
  readonly notificaciones = signal<AvisoNotificacion[]>([]);
  readonly cargandoNotificaciones = signal(false);
  readonly errorNotificaciones = signal('');
  readonly marcandoNotificacion = signal<string | null>(null);
  readonly notificacionesNoLeidas = computed(
    () => this.notificaciones().filter((notificacion) => !notificacion.leida).length,
  );
  readonly estadoNotificaciones = signal('');
  readonly tipoNotificaciones = signal('');
  idSesionReporte = '';
  nombreEstudianteReporte = '';
  readonly busquedaResultados = signal('');
  readonly estadoResultados = signal('');
  readonly estadosResultados = computed(() => [
    ...new Set((this.reporteSesion() ?? []).map((fila) => fila.estado)),
  ]);
  readonly reporteSesionVisible = computed(() => {
    const consulta = this.busquedaResultados().trim().toLocaleLowerCase('es');
    return (this.reporteSesion() ?? []).filter(
      (fila) =>
        (!consulta ||
          fila.nombreEstudiante.toLocaleLowerCase('es').includes(consulta) ||
          fila.correoEstudiante.toLocaleLowerCase('es').includes(consulta)) &&
        (!this.estadoResultados() || fila.estado === this.estadoResultados()),
    );
  });
  valoraciones: Record<string, string> = {};
  altaEstudiante = { nombre: '', correo: '', clave: '' };
  convocadosSeleccionados: string[] = [];
  idEstudiante = '';
  nombreEspacio = '';
  periodo = '2026-2';
  estudiantesEspacio: string[] = [];
  private estudiantesPredeterminados: string[] = [];
  borrador: Caso | null = null;
  programacion = {
    idCaso: '',
    idEspacio: '',
    inicioPrevisto: '',
    finPrevisto: '',
    duracionMaximaMin: 30,
    modoNavegacion: 'FLEXIBLE',
    lugar: '',
    mensaje: '',
    estudiantes: [] as string[],
  };
  constructor() {
    let habiaCuenta = false;
    effect((limpiar) => {
      const usuario = this.api.usuario();
      if (this.vista() !== 'sesiones' || usuario?.rol !== 'DOCENTE') return;
      this.ahoraSesiones.set(Date.now());
      let consultando = false;
      const intervalo = setInterval(async () => {
        const ahora = Date.now();
        this.ahoraSesiones.set(ahora);
        if (consultando || this.ocupado() || !this.sesiones().some(
          (sesion) => sesion.estado === 'SIN_INICIAR' && Date.parse(sesion.inicioPrevisto) <= ahora,
        )) return;
        consultando = true;
        try {
          const sesiones = await this.api.consultar<Sesion[]>('/sesiones');
          if (this.api.usuario() === usuario && !this.ocupado()) this.sesiones.set(sesiones);
        } catch {
          // El siguiente ciclo vuelve a consultar el estado.
        } finally {
          consultando = false;
        }
      }, 5000);
      limpiar(() => clearInterval(intervalo));
    });
    effect(() => {
      if (this.api.conectado()) {
        habiaCuenta = true;
      } else if (habiaCuenta) {
        habiaCuenta = false;
        if (!this.demostracion()) {
          this.reiniciarDatosDeCuenta();
          this.modal.set('');
        }
      }
    });
    this.enrutador.events.subscribe((evento) => {
      if (!(evento instanceof NavigationEnd)) return;
      const direccion = this.enrutador.parseUrl(evento.urlAfterRedirects);
      const segmentos = direccion.root.children['primary']?.segments.map((segmento) => segmento.path) ?? [];
      const sesion = direccion.queryParams['sesion'];
      this.sesionEstudiante.set(
        direccion.root.children['primary']?.segments[0]?.path === 'ingreso' &&
          typeof sesion === 'string'
          ? sesion
          : null,
      );
      this.destinoEstudiante.set(
        segmentos[0] === 'estudiante' && segmentos[1] === 'actividades' && segmentos[2]
          ? segmentos[2]
          : null,
      );
      if (
        this.sesionEstudiante() !== null &&
        (!this.api.conectado() || this.api.usuario()?.rol === 'ESTUDIANTE')
      )
        return;
      this.demostracion.set(!this.api.conectado() && direccion.queryParams['demostracion'] === '1');
      if (!this.api.conectado() && !this.demostracion()) {
        if (evento.urlAfterRedirects !== '/ingreso')
          void this.enrutador.navigate(['/ingreso'], {
            replaceUrl: true,
            queryParams: segmentos[0] === 'estudiante' && segmentos[1] === 'actividades' && segmentos[2]
              ? { sesion: segmentos[2] }
              : undefined,
          });
        return;
      }
      const vista = direccion.queryParams['vista'] ?? this.menu()[0].id;
      this.vista.set(this.menu().some((item) => item.id === vista) ? vista : this.menu()[0].id);
      if (evento.urlAfterRedirects.startsWith('/ingreso')) {
        if (this.api.usuario()?.rol === 'ESTUDIANTE') {
          void this.enrutador.navigate(['/estudiante', 'actividades'], { replaceUrl: true });
        } else {
          this.navegar(this.menu()[0].id);
        }
      }
    });
    if (this.api.usuario()?.rol === 'DOCENTE') {
      this.reiniciarDatosDeCuenta();
      void this.ejecutar(() => this.cargarDatosDocente());
    }
  }
  navegar(vista: string) {
    const cerrarMenu = this.menuMovilAbierto();
    this.menuMovilAbierto.set(false);
    void this.enrutador
      .navigate(['/'], {
        queryParams: { vista, ...(this.demostracion() ? { demostracion: '1' } : {}) },
      })
      .then(() => {
        window.scrollTo(0, 0);
        if (cerrarMenu)
          document.querySelector<HTMLElement>('#principal')?.focus({ preventScroll: true });
      });
    this.busqueda.set('');
    this.error.set('');
    if (vista === 'notificaciones' && this.api.conectado()) void this.cargarNotificaciones();
  }
  iniciarDemostracion() {
    this.reiniciarDatosDeCuenta();
    this.casos.set(this.casosEjemplo());
    this.sesiones.set(this.sesionesEjemplo());
    this.espacios.set(this.espaciosEjemplo());
    this.demostracion.set(true);
    this.error.set('');
    this.aviso.set(
      'Modo demostración: los datos son ficticios y los cambios no se guardan en el servidor.',
    );
    this.navegar('inicio');
  }
  irAlIngreso() {
    if (this.ocupado()) return;
    this.demostracion.set(false);
    this.modal.set('');
    this.error.set('');
    void this.enrutador.navigate(['/ingreso']);
  }
  abrir(tipo: string, caso: Caso | null = null) {
    if (tipo === 'ingreso' && !this.api.conectado()) {
      this.irAlIngreso();
      return;
    }
    const modalAnterior = this.modal();
    if (!modalAnterior) this.focoAnterior = document.activeElement as HTMLElement;
    this.seleccionado.set(caso);
    this.error.set('');
    this.erroresPublicacion.set([]);
    this.modal.set(tipo);
    if (tipo === 'espacio') {
      this.espacioEditado.set(null);
      this.nombreEspacio = '';
      this.periodo = '2026-2';
      this.estudiantesEspacio = [];
    }
    if (tipo === 'programar')
      this.seleccionarEspacioSesion(this.programacion.idEspacio);
    if ((tipo === 'programar' || tipo === 'convocados' || tipo === 'espacio') && this.api.conectado()) {
      void this.cargarEstudiantes();
    }
    if (tipo === 'crear') this.borrador = caso ? structuredClone(caso) : this.recuperarBorradorLocal();
    setTimeout(() => document.querySelector<HTMLElement>('.modal')?.focus());
  }
  private claveBorradorLocal() {
    const usuario = this.api.usuario();
    if (usuario && this.api.conectado()) return `psicoapp.borrador-caso.v1.cuenta.${usuario.id}`;
    return this.demostracion() ? 'psicoapp.borrador-caso.v1.demostracion' : null;
  }

  private recuperarBorradorLocal(): Caso {
    const clave = this.claveBorradorLocal();
    if (clave) {
      try {
        const guardado = localStorage.getItem(clave);
        const caso: Caso | null = guardado ? JSON.parse(guardado) : null;
        if (caso?.estado === 'BORRADOR' && Array.isArray(caso.escenarios)) return caso;
      } catch {
        // Un borrador ilegible o el almacenamiento bloqueado no impiden usar el editor.
      }
    }
    return EditorCaso.nuevo();
  }

  guardarBorradorLocal(caso: Caso) {
    if (this.seleccionado()) return;
    const clave = this.claveBorradorLocal();
    if (!clave) return;
    try {
      localStorage.setItem(clave, JSON.stringify(caso));
    } catch {
      // El formulario sigue disponible si el navegador no permite guardar localmente.
    }
  }

  reiniciarBorrador() {
    if (this.ocupado() || this.seleccionado() || !this.borrador) return;
    if (!window.confirm('¿Borrar el borrador de este caso y empezar de cero?')) return;
    this.eliminarBorradorLocal(this.claveBorradorLocal(), JSON.stringify(this.borrador));
    this.borrador = EditorCaso.nuevo();
    this.error.set('');
  }

  private eliminarBorradorLocal(clave: string | null, copia: string) {
    if (!clave) return;
    try {
      if (localStorage.getItem(clave) === copia) localStorage.removeItem(clave);
    } catch {
      // El editor sigue disponible si el almacenamiento local está bloqueado.
    }
  }

  cerrar() {
    if (!this.ocupado()) {
      if (this.modal() === 'alta-estudiante')
        this.altaEstudiante = { nombre: '', correo: '', clave: '' };
      this.modal.set('');
      this.error.set('');
      this.erroresPublicacion.set([]);
      this.focoAnterior?.focus();
    }
  }
  @HostListener('document:keydown.escape')
  cerrarConEscape() {
    if (this.modal()) this.cerrar();
    if (this.menuMovilAbierto()) {
      this.menuMovilAbierto.set(false);
      document.querySelector<HTMLElement>('#alternar-menu')?.focus();
    }
  }
  mantenerFoco(evento: Event) {
    const teclado = evento as KeyboardEvent;
    const elementos = Array.from(
      document.querySelectorAll<HTMLElement>(
        '.modal :is(button, input, textarea, select, summary, [tabindex="0"]):not(:disabled)',
      ),
    ).filter((elemento) =>
      !elemento.closest('dialog:not([open])') &&
      (elemento.matches('summary') || !elemento.closest('details:not([open])')),
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
  private sesionesEjemplo(): Sesion[] {
    return [
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
    ];
  }
  private espaciosEjemplo(): Espacio[] {
    return [{ id: 'demo-espacio', nombre: 'Psicología social', periodo: '2026-2' }];
  }
  private reiniciarDatosDeCuenta() {
    this.casos.set([]);
    this.sesiones.set([]);
    this.espacios.set([]);
    this.resultado.set(null);
    this.seleccionado.set(null);
    this.sesionPendiente.set(null);
    this.espacioEditado.set(null);
    this.limpiarFiltrosSesiones();
    this.busquedaEspacios.set('');
    this.periodoEspacios.set('');
    this.busqueda.set('');
    this.filtro.set('TODOS');
    this.idEstudiante = '';
    this.nombreEspacio = '';
    this.periodo = '2026-2';
    this.estudiantesEspacio = [];
    this.estudiantesPredeterminados = [];
    this.borrador = null;
    this.estudiantes.set([]);
    this.notificaciones.set([]);
    this.estadoNotificaciones.set('');
    this.tipoNotificaciones.set('');
    this.errorNotificaciones.set('');
    this.reporteSesion.set(null);
    this.participantesDetalleSesion.set(null);
    this.idSesionReporte = '';
    this.identificadorReporte = '';
    this.nombreEstudianteReporte = '';
    this.busquedaResultados.set('');
    this.estadoResultados.set('');
    this.valoraciones = {};
    this.altaEstudiante = { nombre: '', correo: '', clave: '' };
    this.convocadosSeleccionados = [];
    this.programacion = {
      idCaso: '',
      idEspacio: '',
      inicioPrevisto: '',
      finPrevisto: '',
      duracionMaximaMin: 30,
      modoNavegacion: 'FLEXIBLE',
      lugar: '',
      mensaje: '',
      estudiantes: [],
    };
  }
  async ejecutar(accion: () => Promise<void>) {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.error.set('');
    this.erroresPublicacion.set([]);
    try {
      await accion();
    } catch (error) {
      if (error instanceof PublicacionCasoInvalida) this.erroresPublicacion.set(error.errores);
      this.error.set(
        error instanceof Error ? error.message : 'No fue posible completar la operación.',
      );
    } finally {
      this.ocupado.set(false);
    }
  }
  async completarIngreso() {
    this.demostracion.set(false);
    this.inicioTutorial.set(true);
    await this.ejecutar(async () => {
      this.reiniciarDatosDeCuenta();
      this.modal.set('');
      this.aviso.set('Conexión establecida.');
      if (this.api.usuario()?.rol === 'ESTUDIANTE') {
        const destino = this.sesionEstudiante();
        await this.enrutador.navigate(
          destino ? ['/estudiante', 'actividades', destino] : ['/estudiante', 'actividades'],
          { replaceUrl: true },
        );
      } else if (this.api.usuario()?.rol === 'ADMINISTRADOR') {
        this.aviso.set('');
        this.navegar('administracion');
      } else {
        this.navegar('inicio');
        await this.cargarDatosDocente();
      }
    });
  }

  private async cargarDatosDocente() {
    const usuario = this.api.usuario();
    const [espacios, casos, sesiones] = await Promise.all([
      this.api.consultar<Espacio[]>('/academico/espacios'),
      this.api.consultar<Caso[]>('/casos'),
      this.api.consultar<Sesion[]>('/sesiones'),
    ]);
    if (this.api.usuario() !== usuario) return;
    this.espacios.set(espacios);
    this.casos.set(casos);
    this.sesiones.set(sesiones);
    await this.cargarNotificaciones();
  }

  async cargarNotificaciones() {
    if (this.cargandoNotificaciones() || this.marcandoNotificacion() || !this.api.conectado()) return;
    const usuario = this.api.usuario();
    this.cargandoNotificaciones.set(true);
    this.errorNotificaciones.set('');
    try {
      const notificaciones = await this.api.listarNotificaciones(100);
      if (this.api.usuario() === usuario) this.notificaciones.set(notificaciones);
    } catch (error) {
      if (this.api.usuario() === usuario)
        this.errorNotificaciones.set(
          error instanceof Error ? error.message : 'No fue posible consultar las notificaciones.',
        );
    } finally {
      this.cargandoNotificaciones.set(false);
    }
  }

  async marcarNotificacionLeida(notificacion?: AvisoNotificacion) {
    if (notificacion?.leida || this.marcandoNotificacion() || this.cargandoNotificaciones()) return;
    const usuario = this.api.usuario();
    this.marcandoNotificacion.set(notificacion?.id ?? 'todas');
    this.errorNotificaciones.set('');
    try {
      if (notificacion) await this.api.marcarNotificacionLeida(notificacion.id);
      else await this.api.marcarTodasNotificacionesLeidas();
      if (this.api.usuario() !== usuario) return;
      this.notificaciones.update((lista) =>
        lista.map((actual) =>
          !notificacion || actual.id === notificacion.id ? { ...actual, leida: true } : actual,
        ),
      );
    } catch (error) {
      if (this.api.usuario() === usuario) this.errorNotificaciones.set(
        error instanceof Error ? error.message : 'No fue posible marcar las notificaciones.',
      );
    } finally {
      this.marcandoNotificacion.set(null);
    }
  }

  salir() {
    if (this.ocupado()) return;
    this.api.salir();
    this.reiniciarDatosDeCuenta();
    this.modal.set('');
    this.error.set('');
    void this.enrutador.navigate(['/ingreso'], { replaceUrl: true });
  }
  async guardarCaso(caso: Caso) {
    await this.ejecutar(async () => {
      const anterior = this.seleccionado();
      const claveLocal = anterior ? null : this.claveBorradorLocal();
      const copiaEnviada = claveLocal ? JSON.stringify(caso) : '';
      const guardado = this.api.conectado()
        ? anterior
          ? await this.api.enviar<Caso>(`/casos/${anterior.id}`, caso, 'PUT')
          : await this.api.enviar<Caso>('/casos', caso)
        : caso;
      this.eliminarBorradorLocal(claveLocal, copiaEnviada);
      this.casos.update((casos) =>
        anterior
          ? casos.map((actual) => (actual.id === anterior.id ? guardado : actual))
          : [guardado, ...casos],
      );
      this.modal.set('');
      this.navegar('casos');
      this.aviso.set(anterior ? 'Borrador actualizado.' : 'Borrador guardado.');
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
  limpiarFiltrosSesiones() {
    this.busquedaSesiones.set('');
    this.estadoSesiones.set('');
    this.espacioSesiones.set('');
    this.paginaSesiones.set(1);
  }
  editarEspacio(espacio: Espacio) {
    this.abrir('espacio');
    this.espacioEditado.set(espacio);
    this.nombreEspacio = espacio.nombre;
    this.periodo = espacio.periodo;
    this.estudiantesEspacio = [...(espacio.estudiantes ?? [])];
  }

  seleccionarEspacioSesion(id: string) {
    const manuales = this.programacion.estudiantes.filter(
      (estudiante) => !this.estudiantesPredeterminados.includes(estudiante),
    );
    this.programacion.idEspacio = id;
    const predeterminados = this.espacioProgramado()?.estudiantes ?? [];
    this.estudiantesPredeterminados = predeterminados.filter((estudiante) => !manuales.includes(estudiante));
    this.programacion.estudiantes = [...new Set([...manuales, ...this.estudiantesPredeterminados])];
  }

  espacioProgramado() {
    return this.espacios().find((espacio) => espacio.id === this.programacion.idEspacio);
  }
  limpiarFiltrosEspacios() {
    this.busquedaEspacios.set('');
    this.periodoEspacios.set('');
  }
  limpiarFiltrosResultados() {
    this.busquedaResultados.set('');
    this.estadoResultados.set('');
  }
  async cargarEstudiantes() {
    if (this.cargandoEstudiantes()) return;
    const usuario = this.api.usuario();
    this.cargandoEstudiantes.set(true);
    try {
      const estudiantes = await this.api.consultar<Usuario[]>('/identidad/estudiantes/listado');
      if (this.api.usuario() === usuario) {
        this.estudiantes.set(estudiantes);
        const disponibles = new Set(estudiantes.map((estudiante) => estudiante.id));
        if (this.modal() === 'programar') {
          this.programacion.estudiantes = this.programacion.estudiantes.filter(
            (id) => disponibles.has(id) || !this.estudiantesPredeterminados.includes(id),
          );
        }
        if (this.modal() === 'espacio')
          this.estudiantesEspacio = this.estudiantesEspacio.filter((id) => disponibles.has(id));
      }
    } catch (error) {
      if (this.api.usuario() === usuario)
        this.error.set(error instanceof Error ? error.message : 'No se pudo cargar la lista.');
    } finally {
      this.cargandoEstudiantes.set(false);
    }
  }

  async registrarEstudiante() {
    await this.ejecutar(async () => {
      const estudiante = await this.api.registrarEstudiante(this.altaEstudiante);
      this.altaEstudiante = { nombre: '', correo: '', clave: '' };
      this.estudiantes.update((lista) =>
        lista.some((actual) => actual.id === estudiante.id) ? lista : [...lista, estudiante],
      );
      this.modal.set('');
      this.aviso.set('Estudiante registrado. Ya puedes seleccionarlo al programar una sesión.');
      const menuAbierto = this.menuMovilAbierto();
      this.menuMovilAbierto.set(false);
      setTimeout(() => {
        const destino = menuAbierto
          ? document.getElementById('alternar-menu')
          : this.focoAnterior;
        destino?.focus();
      });
    });
  }

  editarConvocados(sesion: Sesion) {
    this.convocadosSeleccionados = this.convocadosActuales(sesion);
    this.sesionPendiente.set(sesion);
    this.abrir('convocados');
  }

  async abrirDetalleSesion(sesion: Sesion) {
    this.sesionPendiente.set(sesion);
    this.participantesDetalleSesion.set(null);
    this.filtroParticipantesSesion.set('');
    this.abrir('detalle-sesion');
    if (!this.api.conectado()) return;

    await this.ejecutar(async () => {
      const id = encodeURIComponent(sesion.id);
      const [detalle, filas] = await Promise.all([
        this.api.consultar<Sesion>(`/sesiones/${id}`),
        this.api.consultar<FilaReporteSesion[]>(`/reportes/sesiones/${id}`),
      ]);
      if (this.sesionPendiente() !== sesion) return;
      this.sesionPendiente.set(detalle);
      this.sesiones.update((sesiones) => sesiones.map((actual) => actual.id === detalle.id ? detalle : actual));
      this.participantesDetalleSesion.set(filas.filter((fila) => fila.idIntento !== null));
    });
  }

  private convocadosActuales(sesion: Sesion) {
    return sesion.convocatorias
      .filter((convocatoria) => convocatoria.autorizada !== false)
      .map((convocatoria) => convocatoria.idEstudiante);
  }

  cambiosConvocados() {
    const sesion = this.sesionPendiente();
    const anteriores = sesion ? this.convocadosActuales(sesion) : [];
    return {
      agregar: this.convocadosSeleccionados.filter((id) => !anteriores.includes(id)),
      retirar: anteriores.filter((id) => !this.convocadosSeleccionados.includes(id)),
    };
  }

  async guardarConvocados() {
    await this.ejecutar(async () => {
      const sesion = this.sesionPendiente();
      if (!sesion) return;
      await this.api.enviar<Sesion>(
        `/sesiones/${sesion.id}/convocados`,
        this.cambiosConvocados(),
        'PUT',
      );
      const actualizada = await this.api.consultar<Sesion>(`/sesiones/${sesion.id}`);
      this.sesiones.update((sesiones) =>
        sesiones.map((actual) => (actual.id === actualizada.id ? actualizada : actual)),
      );
      this.modal.set('');
      this.aviso.set('Convocatoria actualizada.');
    });
  }
  async guardarEspacio() {
    await this.ejecutar(async () => {
      const datos = { nombre: this.nombreEspacio, periodo: this.periodo, estudiantes: this.estudiantesEspacio };
      const anterior = this.espacioEditado();
      if (
        anterior &&
        !this.api.conectado() &&
        (datos.nombre.trim() !== anterior.nombre || datos.periodo.trim() !== anterior.periodo) &&
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
      };
      if (new Date(datos.finPrevisto) <= new Date(datos.inicioPrevisto))
        throw new Error('La fecha final debe ser posterior al inicio.');
      const sesion = this.api.conectado()
        ? await this.api.enviar<Sesion>('/sesiones', datos)
        : { ...datos, id: crypto.randomUUID(), estado: 'SIN_INICIAR', convocatorias: [] };
      this.sesiones.update((sesiones) => [...sesiones, sesion]);
      this.programacion.estudiantes = [];
      this.estudiantesPredeterminados = [];
      this.modal.set('');
      this.navegar('sesiones');
      this.aviso.set(
        'Sesión programada. En conexión real, el servidor gestiona las convocatorias.',
      );
    });
  }
  permiteInicioAnticipado(sesion: Sesion) {
    return this.demostracion() || this.ahoraSesiones() < Date.parse(sesion.inicioPrevisto);
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
          `/reportes/casos/${encodeURIComponent(this.identificadorReporte.trim())}/estudiantes/${encodeURIComponent(this.idEstudiante.trim())}`,
        ),
      );
    });
  }

  async valorar(reporte: Reporte) {
    await this.ejecutar(async () => {
      const id = reporte.participacion.idIntento;
      const valoracion = await this.api.enviar<Reporte['valoracionesGenerales'][number]>(
        `/reportes/participaciones/${id}/valoraciones`,
        { mensaje: this.valoraciones[id].trim() },
      );
      this.resultado.update(
        (reportes) =>
          reportes?.map((actual) =>
            actual.participacion.idIntento === id
              ? { ...actual, valoracionesGenerales: [...actual.valoracionesGenerales, valoracion] }
              : actual,
          ) ?? null,
      );
      this.valoraciones[id] = '';
      this.aviso.set('Valoración publicada.');
    });
  }

  async consultarReporteSesion() {
    await this.ejecutar(async () => {
      this.limpiarFiltrosResultados();
      this.reporteSesion.set(null);
      this.resultado.set(null);
      this.nombreEstudianteReporte = '';
      this.reporteSesion.set(
        await this.api.consultar<FilaReporteSesion[]>(
          `/reportes/sesiones/${encodeURIComponent(this.idSesionReporte)}`,
        ),
      );
    });
  }

  puedeAutorizarReintento(fila: FilaReporteSesion, filas: FilaReporteSesion[]) {
    const sesion = this.sesiones().find((actual) => actual.id === this.idSesionReporte);
    return (
      sesion?.estado === 'EN_CURSO' &&
      filas.filter((actual) => actual.idEstudiante === fila.idEstudiante).at(-1) === fila
    );
  }

  async autorizarReintento(fila: FilaReporteSesion) {
    await this.ejecutar(async () => {
      await this.api.enviar<void>(
        `/sesiones/${this.idSesionReporte}/reintentos/${fila.idEstudiante}`,
        {},
        'PUT',
      );
      this.aviso.set(`Reintento autorizado para ${fila.nombreEstudiante}.`);
    });
  }

  async consultarResultados(fila: FilaReporteSesion) {
    const sesion = this.sesiones().find((actual) => actual.id === this.idSesionReporte);
    if (!sesion) {
      this.error.set(
        'No se encontró la sesión seleccionada. Actualiza la página e inténtalo de nuevo.',
      );
      return;
    }
    this.identificadorReporte = sesion.idCaso;
    this.idEstudiante = fila.idEstudiante;
    this.nombreEstudianteReporte = fila.nombreEstudiante;
    await this.consultarReporte();
  }

  descargarReporteSesion() {
    const filas = this.reporteSesion();
    if (!filas) return;
    const escapar = (valor: unknown) => `"${String(valor ?? '').replaceAll('"', '""')}"`;
    const csv = [
      'Estudiante,Correo,Identificador,Intento,Estado,Calificación,Preguntas respondidas,Preguntas totales,Preguntas ausentes',
      ...filas.map((fila) =>
        [
          fila.nombreEstudiante,
          fila.correoEstudiante,
          fila.idEstudiante,
          fila.idIntento,
          fila.estado,
          fila.calificacion,
          fila.preguntasRespondidas,
          fila.preguntasTotales,
          fila.ausencia?.detalles.length ?? 0,
        ]
          .map(escapar)
          .join(','),
      ),
    ].join('\r\n');
    const url = URL.createObjectURL(new Blob(['\uFEFF', csv], { type: 'text/csv;charset=utf-8' }));
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = 'participacion.csv';
    enlace.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
  nombreCaso(id: string) {
    return this.casos().find((caso) => caso.id === id)?.nombre ?? 'Caso no disponible';
  }
  nombreEspacioAcademico(id: string) {
    const espacio = this.espacios().find((actual) => actual.id === id);
    return espacio ? `${espacio.nombre} · ${espacio.periodo}` : 'Espacio no disponible';
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
  cantidadPreguntas(caso: Caso) {
    return caso.escenarios.reduce((total, escenario) => total + escenario.preguntas.length, 0);
  }
  etiquetaErrorPublicacion(ruta: string) {
    const partes: string[] = [];
    const escenario = ruta.match(/escenarios\[(\d+)]/);
    const pregunta = ruta.match(/preguntas\[(\d+)]/);
    const opcion = ruta.match(/opciones\[(\d+)]/);
    const criterio = ruta.match(/criterios\[(\d+)]/);
    if (escenario) partes.push(`Escenario ${Number(escenario[1]) + 1}`);
    if (pregunta) partes.push(`Pregunta ${Number(pregunta[1]) + 1}`);
    if (opcion) partes.push(`Opción ${Number(opcion[1]) + 1}`);
    if (ruta.startsWith('rubrica')) partes.push('Rúbrica');
    if (criterio) partes.push(`Criterio ${Number(criterio[1]) + 1}`);
    const campo =
      ruta
        .split('.')
        .at(-1)
        ?.replace(/\[\d+]/g, '') ?? ruta;
    const nombres: Record<string, string> = {
      nombre: 'Nombre',
      desarrolloSituacional: 'Desarrollo de la situación',
      contexto: 'Tema o contexto',
      tiempoEstimadoMin: 'Tiempo estimado',
      escenarios: 'Escenarios',
      titulo: 'Título',
      contextoSituacional: 'Contexto del escenario',
      orden: 'Orden',
      preguntas: 'Preguntas',
      enunciado: 'Enunciado',
      opciones: 'Opciones de respuesta',
      texto: 'Texto de la opción',
      justificacion: 'Justificación',
      rubrica: 'Rúbrica',
      criterios: 'Criterios',
      ponderacion: 'Ponderación',
    };
    const nombreCampo = nombres[campo];
    if (nombreCampo && partes.at(-1) !== nombreCampo) partes.push(nombreCampo);
    return partes.length > 0 ? partes.join(' · ') : 'Contenido del caso';
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
