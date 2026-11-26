import {
  afterNextRender, Component, computed, effect, ElementRef, inject, Injector, input, model, signal,
  viewChild,
} from '@angular/core';
import { PanelNotificaciones } from './panel-notificaciones';
import { DatePipe, DecimalPipe } from '@angular/common';
import { CambioClave } from './cambio-clave';
import { Router } from '@angular/router';
import { TutorialGuiado } from './tutorial-guiado';
import { pasosEstudiante } from './pasos-tutorial';
import {
  ActividadEstudiante,
  AvisoNotificacion,
  IntentoEstudiante,
  ResultadoEstudiante,
  ServicioApi,
  SimulacionEstudiante,
} from './servicio-api';

@Component({
  selector: 'psico-espacio-estudiante',
  imports: [DatePipe, DecimalPipe, TutorialGuiado, PanelNotificaciones, CambioClave],
  templateUrl: './espacio-estudiante.html',
  styleUrl: './espacio-estudiante.css',
})
export class EspacioEstudiante {
  readonly sesionDestino = input<string | null>(null);
  readonly tutorialInicial = model(false);
  readonly pasosTutorial = pasosEstudiante;
  readonly api = inject(ServicioApi);
  private readonly enrutador = inject(Router);
  private readonly inyector = inject(Injector);
  private readonly tituloDetalleAviso = viewChild<ElementRef<HTMLElement>>('tituloDetalleAviso');
  private readonly tituloNotificaciones = viewChild<ElementRef<HTMLElement>>('tituloNotificaciones');
  private readonly tituloActividadNotificada = viewChild<ElementRef<HTMLElement>>('tituloActividadNotificada');
  private readonly tituloResultado = viewChild<ElementRef<HTMLElement>>('tituloResultado');
  private controlAviso: HTMLButtonElement | null = null;
  readonly vista = signal<'actividades' | 'simulacion' | 'resultados' | 'notificaciones'>('actividades');
  readonly actividades = signal<ActividadEstudiante[]>([]);
  readonly simulacion = signal<SimulacionEstudiante | null>(null);
  readonly intento = signal<IntentoEstudiante | null>(null);
  readonly resultados = signal<ResultadoEstudiante[]>([]);
  readonly resultadoSeleccionado = signal<ResultadoEstudiante | null>(null);
  readonly notificaciones = signal<AvisoNotificacion[]>([]);
  readonly idAvisoSeleccionado = signal<string | null>(null);
  readonly idActividadNotificada = signal<string | null>(null);
  readonly avisoSeleccionado = computed(() =>
    this.notificaciones().find((aviso) => aviso.id === this.idAvisoSeleccionado()) ?? null,
  );
  readonly resultadoAviso = computed(() => {
    const id = this.avisoSeleccionado()?.idIntento;
    return id ? this.resultados().find((resultado) => resultado.idIntento === id) : undefined;
  });
  readonly actividadAviso = computed(() => {
    const id = this.avisoSeleccionado()?.idSesion;
    return id ? this.actividades().find((actividad) => actividad.idSesion === id) : undefined;
  });
  readonly recursoAviso = computed(() => {
    const aviso = this.avisoSeleccionado();
    if (!aviso) return null;
    if (aviso.idIntento) {
      const etiqueta = aviso.tipo === 'VALORACION' ? 'Ver retroalimentación' : 'Consultar resultado';
      return { tipo: 'resultado', id: aviso.idIntento, etiqueta };
    }
    if (aviso.tipo === 'RESULTADO' || aviso.tipo === 'VALORACION' || !aviso.idSesion) return null;
    return { tipo: 'actividad', id: aviso.idSesion, etiqueta: 'Ver actividad' };
  });
  readonly noLeidas = signal<number | null>(null);
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly ahora = signal(Date.now());
  private destinoAbierto = '';

  readonly pendientes = computed(() =>
    this.actividades().filter(
      (actividad) => actividad.accion !== 'RESULTADO' && actividad.estadoSesion !== 'FINALIZADO',
    ).length,
  );
  readonly actividadesOrdenadas = computed(() => {
    const prioridad = (actividad: ActividadEstudiante) => {
      if (actividad.accion === 'CONTINUAR' && !this.plazoIngresoTerminado(actividad)) return 0;
      if (actividad.accion === 'INICIAR' && !this.plazoIngresoTerminado(actividad)) return 1;
      return actividad.estadoSesion === 'FINALIZADO' || actividad.accion === 'RESULTADO' ? 3 : 2;
    };
    return [...this.actividades()].sort((a, b) => prioridad(a) - prioridad(b));
  });
  readonly actividadesVisibles = computed(() => {
    const id = this.idActividadNotificada();
    return id ? this.actividades().filter((actividad) => actividad.idSesion === id) : this.actividadesOrdenadas();
  });
  readonly referenciasSesion = computed(() => {
    const grupos = new Map<string, string[]>();
    for (const actividad of this.actividades()) {
      const minuto = Math.floor(Date.parse(actividad.inicioPrevisto) / 60_000);
      const clave = JSON.stringify([actividad.nombreCaso, minuto]);
      const grupo = grupos.get(clave) ?? [];
      grupo.push(actividad.idSesion);
      grupos.set(clave, grupo);
    }
    const referencias = new Map<string, string>();
    for (const grupo of grupos.values()) {
      if (grupo.length < 2) continue;
      grupo.sort().forEach((id, indice) => referencias.set(id, `Sesión ${indice + 1} de ${grupo.length}`));
    }
    return referencias;
  });
  readonly finalizadas = computed(() => this.resultados().length);
  readonly indice = computed(() =>
    this.simulacion()?.preguntas.findIndex((pregunta) => pregunta.id === this.intento()?.preguntaActual) ?? -1,
  );
  readonly pregunta = computed(() => this.simulacion()?.preguntas[this.indice()]);
  readonly respondidas = computed(() => Object.keys(this.intento()?.respuestas ?? {}).length);
  readonly segundosRestantes = computed(() =>
    this.segundosHasta(this.intento()?.limite ?? ''),
  );
  readonly tiempoRestante = computed(() => this.formatearCuentaRegresiva(this.segundosRestantes()));

  constructor() {
    void this.cargarInicio();
    effect((limpiar) => {
      if (this.vista() !== 'actividades') return;
      this.ahora.set(Date.now());
      let pulsos = 0;
      let consultando = false;
      const intervalo = setInterval(async () => {
        const ahora = Date.now();
        this.ahora.set(ahora);
        if (++pulsos % 5 !== 0 || consultando || this.ocupado() || !this.actividades().some(
          (actividad) => actividad.accion === 'CONTINUAR' ||
            (actividad.estadoSesion === 'SIN_INICIAR' &&
              Date.parse(actividad.inicioPrevisto) <= ahora && Date.parse(actividad.finPrevisto) > ahora),
        )) return;
        consultando = true;
        try {
          const actividades = await this.api.listarActividadesEstudiante();
          if (this.vista() === 'actividades' && !this.ocupado()) this.actividades.set(actividades);
        } catch {
          // El siguiente ciclo vuelve a consultar el estado.
        } finally {
          consultando = false;
        }
      }, 1000);
      limpiar(() => clearInterval(intervalo));
    });
    effect(() => {
      const destino = this.sesionDestino();
      if (destino && destino !== this.destinoAbierto) {
        this.destinoAbierto = destino;
        void this.abrirActividad(destino);
      }
    });
    effect((limpiar) => {
      const intento = this.intento();
      if (this.vista() !== 'simulacion' || !intento || intento.estado !== 'EN_PROGRESO') return;
      this.ahora.set(Date.now());
      let pulsos = 0;
      const intervalo = setInterval(() => {
        this.ahora.set(Date.now());
        if (++pulsos % 5 === 0) void this.refrescarIntento(intento.id);
      }, 1000);
      limpiar(() => clearInterval(intervalo));
    });
  }

  plazoIngresoTerminado(actividad: ActividadEstudiante) {
    if (
      actividad.estadoSesion === 'FINALIZADO' ||
      actividad.accion === 'RESULTADO' || actividad.accion === 'CONTINUAR'
    ) return false;
    return this.ahora() >= Date.parse(actividad.finPrevisto);
  }

  cuentaRegresivaHasta(fecha: string) {
    return this.formatearCuentaRegresiva(this.segundosHasta(fecha));
  }

  plazoVencido(fecha: string | null) {
    return fecha !== null && this.segundosHasta(fecha) === 0;
  }

  private segundosHasta(fecha: string) {
    return Math.max(0, Math.ceil((Date.parse(fecha) - this.ahora()) / 1000) || 0);
  }

  private formatearCuentaRegresiva(segundos: number) {
    const minutos = String(Math.floor(segundos / 60)).padStart(2, '0');
    return `${minutos}:${String(segundos % 60).padStart(2, '0')}`;
  }

  async cargarInicio() {
    await this.ejecutar(async () => {
      const [actividades, resultados, notificaciones] = await Promise.all([
        this.api.listarActividadesEstudiante(),
        this.api.listarResultadosEstudiante(),
        this.api.listarNotificaciones(100),
        this.actualizarNoLeidas(),
      ]);
      this.actividades.set(actividades);
      this.resultados.set(resultados);
      this.notificaciones.set(notificaciones);
    });
  }

  private async actualizarNoLeidas() {
    try {
      this.noLeidas.set(await this.api.contarNotificacionesNoLeidas());
    } catch {
      // El listado de avisos sigue disponible si falla el contador.
    }
  }

  async actualizarActividades() {
    await this.ejecutar(() => this.cargarActividadesYResultados());
  }

  private async cargarActividadesYResultados() {
    const [actividades, resultados] = await Promise.all([
      this.api.listarActividadesEstudiante(),
      this.api.listarResultadosEstudiante(),
    ]);
    this.actividades.set(actividades);
    this.resultados.set(resultados);
  }

  async navegar(vista: 'actividades' | 'resultados' | 'notificaciones') {
    this.idActividadNotificada.set(null);
    this.vista.set(vista);
    this.error.set('');
    await this.enrutador.navigate(['/estudiante', vista]);
    if (vista === 'actividades') await this.actualizarActividades();
    if (vista === 'resultados') this.resultados.set(await this.api.listarResultadosEstudiante());
    if (vista === 'notificaciones') {
      await this.ejecutar(async () => {
        await Promise.all([
          this.actualizarNoLeidas(),
          this.api.listarNotificaciones(100).then((avisos) => this.notificaciones.set(avisos)),
        ]);
      });
    }
  }

  async abrirActividad(idSesion: string) {
    await this.ejecutar(async () => {
      const simulacion = await this.api.consultarActividadEstudiante(idSesion);
      this.simulacion.set(simulacion);
      this.intento.set(simulacion.intento);
      if (simulacion.intento?.estado === 'EN_PROGRESO') {
        this.vista.set('simulacion');
        await this.enrutador.navigate(['/estudiante', 'actividades', idSesion]);
      } else if (simulacion.actividad.accion === 'RESULTADO' && simulacion.intento) {
        this.verResultado(simulacion.intento.id);
      } else {
        this.vista.set('actividades');
      }
    });
  }

  async iniciar(actividad: ActividadEstudiante) {
    await this.ejecutar(async () => {
      const simulacion = await this.api.consultarActividadEstudiante(actividad.idSesion);
      const intento = await this.api.iniciarActividadEstudiante(actividad.idSesion);
      this.simulacion.set({ ...simulacion, intento });
      this.intento.set(intento);
      this.ahora.set(Date.now());
      this.vista.set('simulacion');
      await this.enrutador.navigate(['/estudiante', 'actividades', actividad.idSesion]);
    });
  }

  async responder(idPregunta: string, idOpcion: string) {
    await this.operar('respuestas', { idPregunta, idOpcion }, 'PUT');
  }

  avanzar(dialogo: HTMLDialogElement, idPregunta: string) {
    if (this.intento()?.respuestas[idPregunta]) void this.operar('avanzar');
    else dialogo.showModal();
  }

  confirmarAccion(dialogo: HTMLDialogElement, accion: 'avanzar' | 'finalizar') {
    dialogo.close();
    if (this.segundosRestantes() > 0) void this.operar(accion);
  }

  async operar(accion: string, datos: unknown = {}, metodo = 'POST') {
    const intento = this.intento();
    if (!intento) return;
    await this.ejecutar(async () => {
      const actualizado = await this.api.enviar<IntentoEstudiante>(
        `/participaciones/${intento.id}/${accion}`,
        datos,
        metodo,
      );
      this.intento.set(actualizado);
      if (actualizado.estado !== 'EN_PROGRESO') {
        try {
          await this.cargarActividadesYResultados();
        } finally {
          this.verResultado(actualizado.id);
        }
      }
    });
  }

  verResultado(idIntento: string) {
    const resultado = this.resultados().find((item) => item.idIntento === idIntento) ?? null;
    this.resultadoSeleccionado.set(resultado);
    this.vista.set('resultados');
    void this.enrutador.navigate(['/estudiante', 'resultados']);
  }

  seleccionarAviso(aviso: AvisoNotificacion, control: HTMLButtonElement) {
    this.controlAviso = control;
    this.idAvisoSeleccionado.set(aviso.id);
    this.error.set('');
    afterNextRender(() => this.tituloDetalleAviso()?.nativeElement.focus(), { injector: this.inyector });
  }

  cerrarDetalleAviso() {
    this.idAvisoSeleccionado.set(null);
    this.error.set('');
    if (this.controlAviso?.isConnected) this.controlAviso.focus();
    else this.tituloNotificaciones()?.nativeElement.focus();
    this.controlAviso = null;
  }

  async abrirRecursoAviso() {
    const recurso = this.recursoAviso();
    if (!recurso) return;
    const idAviso = this.idAvisoSeleccionado();
    await this.ejecutar(async () => {
      if (recurso.tipo === 'resultado') {
        const resultados = await this.api.listarResultadosEstudiante();
        if (this.vista() !== 'notificaciones' || this.idAvisoSeleccionado() !== idAviso) return;
        this.resultados.set(resultados);
        if (!resultados.some((resultado) => resultado.idIntento === recurso.id)) {
          throw new Error('El resultado de este aviso ya no está disponible.');
        }
        this.verResultado(recurso.id);
        afterNextRender(() => this.tituloResultado()?.nativeElement.focus(), { injector: this.inyector });
        return;
      }

      const actividades = await this.api.listarActividadesEstudiante();
      if (this.vista() !== 'notificaciones' || this.idAvisoSeleccionado() !== idAviso) return;
      this.actividades.set(actividades);
      if (!actividades.some((actividad) => actividad.idSesion === recurso.id)) {
        throw new Error('La actividad de este aviso ya no está disponible.');
      }
      this.idActividadNotificada.set(recurso.id);
      this.vista.set('actividades');
      afterNextRender(() => this.tituloActividadNotificada()?.nativeElement.focus(), { injector: this.inyector });
      await this.enrutador.navigate(['/estudiante', 'actividades']);
    });
  }

  async marcarLeida(aviso?: AvisoNotificacion) {
    if (aviso?.leida) return;
    await this.ejecutar(async () => {
      if (aviso) await this.api.marcarNotificacionLeida(aviso.id);
      else await this.api.marcarTodasNotificacionesLeidas();
      this.notificaciones.update((lista) =>
        lista.map((item) => (!aviso || item.id === aviso.id ? { ...item, leida: true } : item)),
      );
      this.noLeidas.update((total) => {
        if (!aviso) return 0;
        return total === null ? null : Math.max(0, total - 1);
      });
      await this.actualizarNoLeidas();
    });
  }

  salir() {
    this.api.salir();
    void this.enrutador.navigate(['/ingreso'], { replaceUrl: true });
  }

  private async refrescarIntento(id: string) {
    const anterior = this.intento();
    if (this.ocupado() || anterior?.id !== id) return;
    try {
      const intento = await this.api.consultar<IntentoEstudiante>(`/participaciones/${id}`);
      if (this.vista() !== 'simulacion' || this.ocupado() || this.intento() !== anterior) return;
      this.intento.set(intento);
      if (intento.estado !== 'EN_PROGRESO') {
        await this.cargarInicio();
        this.verResultado(id);
      }
    } catch {
      // El siguiente pulso vuelve a consultar el estado durable del intento.
    }
  }

  private async ejecutar(accion: () => Promise<void>) {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.error.set('');
    try {
      await accion();
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No fue posible completar la operación.');
    } finally {
      this.ocupado.set(false);
    }
  }
}
