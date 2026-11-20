import { Component, computed, effect, signal, inject, HostListener } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { NavigationEnd, Router } from '@angular/router';
import { Ingreso } from './ingreso';
import { EditorCaso } from './editor-caso';
import { SelectorEstudiantes } from './selector-estudiantes';
import { Administracion } from './administracion';
import { ServicioApi, Caso, Sesion, Espacio, Reporte, Usuario, FilaReporteSesion } from './servicio-api';
import { nombreEtiqueta } from './etiqueta-usuario';
@Component({
  selector: 'psico-raiz',
  imports: [FormsModule, DatePipe, Administracion, Ingreso, EditorCaso, SelectorEstudiantes],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class Aplicacion {
  readonly sesionEstudiante = signal<string | null>(null);
  readonly api = inject(ServicioApi);
  private readonly enrutador = inject(Router);
  readonly demostracion = signal(false);
  readonly vista = signal('inicio');
  readonly menuMovilAbierto = signal(false);
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
        ],
  );
  readonly casos = signal<Caso[]>(this.casosEjemplo());
  readonly sesiones = signal<Sesion[]>(this.sesionesEjemplo());
  readonly espacios = signal<Espacio[]>(this.espaciosEjemplo());
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
  identificadorCaso = '';
  identificadorSesion = '';
  identificadorReporte = '';
  readonly estudiantes = signal<Usuario[]>([]);
  readonly cargandoEstudiantes = signal(false);
  readonly reporteSesion = signal<FilaReporteSesion[] | null>(null);
  idSesionReporte = '';
  valoraciones: Record<string, string> = {};
  altaEstudiante = { nombre: '', correo: '' };
  convocadosSeleccionados: string[] = [];
  idEstudiante = '';
  nombreEspacio = '';
  periodo = '2026-2';
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
      const sesion = direccion.queryParams['sesion'];
      this.sesionEstudiante.set(direccion.root.children['primary']?.segments[0]?.path === 'ingreso'
        && typeof sesion === 'string' ? sesion : null);
      if (this.sesionEstudiante() !== null && (!this.api.conectado() || this.api.usuario()?.rol === 'ESTUDIANTE')) return;
      this.demostracion.set(!this.api.conectado() && direccion.queryParams['demostracion'] === '1');
      if (!this.api.conectado() && !this.demostracion()) {
        if (evento.urlAfterRedirects !== '/ingreso')
          void this.enrutador.navigate(['/ingreso'], { replaceUrl: true });
        return;
      }
      const vista = direccion.queryParams['vista'] ?? this.menu()[0].id;
      this.vista.set(this.menu().some((item) => item.id === vista) ? vista : this.menu()[0].id);
      if (evento.urlAfterRedirects.startsWith('/ingreso')) this.navegar(this.menu()[0].id);
    });
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
    if (!this.modal()) this.focoAnterior = document.activeElement as HTMLElement;
    this.seleccionado.set(caso);
    this.error.set('');
    this.modal.set(tipo);
    if (tipo === 'espacio') {
      this.espacioEditado.set(null);
      this.nombreEspacio = '';
      this.periodo = '2026-2';
    }
    if ((tipo === 'programar' || tipo === 'convocados') && this.api.conectado()) {
      void this.cargarEstudiantes();
    }
    if (tipo === 'programar') this.estudianteEncontrado.set(null);
    if (tipo === 'crear') this.borrador = caso ? structuredClone(caso) : EditorCaso.nuevo();
    setTimeout(() => document.querySelector<HTMLElement>('.modal')?.focus());
  }
  cerrar() {
    if (!this.ocupado()) {
      this.modal.set('');
      this.error.set('');
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
    this.estudianteEncontrado.set(null);
    this.espacioEditado.set(null);
    this.correoBusqueda = '';
    this.identificadorCaso = '';
    this.identificadorSesion = '';
    this.identificadorReporte = '';
    this.idEstudiante = '';
    this.nombreEspacio = '';
    this.periodo = '2026-2';
    this.borrador = null;
    this.estudiantes.set([]);
    this.reporteSesion.set(null);
    this.idSesionReporte = '';
    this.valoraciones = {};
    this.altaEstudiante = { nombre: '', correo: '' };
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
  async completarIngreso() {
    this.demostracion.set(false);
    await this.ejecutar(async () => {
      this.reiniciarDatosDeCuenta();
      this.modal.set('');
      this.aviso.set('Conexión establecida.');
      if (this.api.usuario()?.rol === 'ADMINISTRADOR') {
        this.aviso.set('');
        this.navegar('administracion');
      } else {
        this.navegar('inicio');
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
      }
    });
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
      const guardado = this.api.conectado()
        ? anterior
          ? await this.api.enviar<Caso>(`/casos/${anterior.id}`, caso, 'PUT')
          : await this.api.enviar<Caso>('/casos', caso)
        : caso;
      this.casos.update((casos) => anterior
        ? casos.map((actual) => actual.id === anterior.id ? guardado : actual)
        : [guardado, ...casos]);
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
  async consultarId(tipo: 'casos' | 'sesiones') {
    await this.ejecutar(async () => {
      const identificador =
        tipo === 'casos' ? this.identificadorCaso.trim() : this.identificadorSesion.trim();
      if (!identificador) throw new Error('Escribe un identificador.');
      const dato = await this.api.consultar<Caso & Sesion>(
        `/${tipo}/${encodeURIComponent(identificador)}`,
      );
      if (tipo === 'casos')
        this.casos.update((casos) => [dato, ...casos.filter((caso) => caso.id !== dato.id)]);
      else
        this.sesiones.update((sesiones) => [
          dato,
          ...sesiones.filter((sesion) => sesion.id !== dato.id),
        ]);
      if (tipo === 'casos') this.identificadorCaso = '';
      else this.identificadorSesion = '';
      this.aviso.set('Consulta completada.');
    });
  }
  editarEspacio(espacio: Espacio) {
    this.abrir('espacio');
    this.espacioEditado.set(espacio);
    this.nombreEspacio = espacio.nombre;
    this.periodo = espacio.periodo;
  }
  async cargarEstudiantes() {
    if (this.cargandoEstudiantes()) return;
    const usuario = this.api.usuario();
    this.cargandoEstudiantes.set(true);
    try {
      const estudiantes = await this.api.consultar<Usuario[]>('/identidad/estudiantes/listado');
      if (this.api.usuario() === usuario) this.estudiantes.set(estudiantes);
    } catch (error) {
      if (this.api.usuario() === usuario)
        this.error.set(error instanceof Error ? error.message : 'No se pudo cargar la lista.');
    } finally {
      this.cargandoEstudiantes.set(false);
    }
  }

  async registrarEstudiante() {
    await this.ejecutar(async () => {
      const estudiante = await this.api.enviar<Usuario>('/identidad/estudiantes', this.altaEstudiante);
      this.altaEstudiante = { nombre: '', correo: '' };
      this.estudiantes.update((lista) => lista.some((actual) => actual.id === estudiante.id)
        ? lista : [...lista, estudiante]);
      this.programacion.estudiantes = [...new Set([...this.programacion.estudiantes, estudiante.id])];
      this.abrir('programar');
      this.aviso.set('Estudiante registrado y agregado a la convocatoria.');
    });
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
    this.estudiantes.update(lista => lista.some(actual => actual.id === estudiante.id) ? lista : [...lista, estudiante]);
    this.programacion.estudiantes = [...new Set([...this.programacion.estudiantes, estudiante.id])];
    this.estudianteEncontrado.set(null);
    this.correoBusqueda = '';
  }

  editarConvocados(sesion: Sesion) {
    this.convocadosSeleccionados = this.convocadosActuales(sesion);
    this.sesionPendiente.set(sesion);
    this.abrir('convocados');
  }

  private convocadosActuales(sesion: Sesion) {
    return sesion.convocatorias.filter((convocatoria) => convocatoria.autorizada !== false)
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
      await this.api.enviar<Sesion>(`/sesiones/${sesion.id}/convocados`, this.cambiosConvocados(), 'PUT');
      const actualizada = await this.api.consultar<Sesion>(`/sesiones/${sesion.id}`);
      this.sesiones.update((sesiones) => sesiones.map((actual) => actual.id === actualizada.id ? actualizada : actual));
      this.modal.set('');
      this.aviso.set('Convocatoria actualizada.');
    });
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
      };
      if (new Date(datos.finPrevisto) <= new Date(datos.inicioPrevisto))
        throw new Error('La fecha final debe ser posterior al inicio.');
      const sesion = this.api.conectado()
        ? await this.api.enviar<Sesion>('/sesiones', datos)
        : { ...datos, id: crypto.randomUUID(), estado: 'SIN_INICIAR', convocatorias: [] };
      this.sesiones.update((sesiones) => [...sesiones, sesion]);
      this.programacion.estudiantes = [];
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
          `/reportes/casos/${encodeURIComponent(this.identificadorReporte.trim())}/estudiantes/${encodeURIComponent(this.idEstudiante.trim())}`,
        ),
      );
    });
  }

  async valorar(reporte: Reporte) {
    await this.ejecutar(async () => {
      const id = reporte.participacion.idIntento;
      const valoracion = await this.api.enviar<Reporte['valoracionesGenerales'][number]>(
        `/reportes/participaciones/${id}/valoraciones`, { mensaje: this.valoraciones[id].trim() },
      );
      this.resultado.update((reportes) => reportes?.map((actual) => actual.participacion.idIntento === id
        ? { ...actual, valoracionesGenerales: [...actual.valoracionesGenerales, valoracion] } : actual) ?? null);
      this.valoraciones[id] = '';
      this.aviso.set('Valoración publicada.');
    });
  }

  async consultarReporteSesion() {
    await this.ejecutar(async () => {
      this.reporteSesion.set(null);
      this.reporteSesion.set(await this.api.consultar<FilaReporteSesion[]>(
        `/reportes/sesiones/${encodeURIComponent(this.idSesionReporte)}`,
      ));
    });
  }

  descargarReporteSesion() {
    const filas = this.reporteSesion();
    if (!filas) return;
    const csv = ['Estudiante,Intento,Estado,Calificación', ...filas.map((fila) =>
      [fila.idEstudiante, fila.idIntento, fila.estado, fila.calificacion].join(','))].join('\r\n');
    const url = URL.createObjectURL(new Blob(['\uFEFF', csv], { type: 'text/csv;charset=utf-8' }));
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = 'participacion.csv';
    enlace.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
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
