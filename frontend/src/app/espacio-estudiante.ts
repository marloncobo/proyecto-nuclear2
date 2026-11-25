import { Component, computed, effect, inject, input, model, signal } from '@angular/core';
import { PanelNotificaciones } from './panel-notificaciones';
import { DatePipe } from '@angular/common';
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
  imports: [DatePipe, TutorialGuiado, PanelNotificaciones, CambioClave],
  templateUrl: './espacio-estudiante.html',
  styleUrl: './espacio-estudiante.css',
})
export class EspacioEstudiante {
  readonly sesionDestino = input<string | null>(null);
  readonly tutorialInicial = model(false);
  readonly pasosTutorial = pasosEstudiante;
  readonly api = inject(ServicioApi);
  private readonly enrutador = inject(Router);
  readonly vista = signal<'actividades' | 'simulacion' | 'resultados' | 'notificaciones'>('actividades');
  readonly actividades = signal<ActividadEstudiante[]>([]);
  readonly simulacion = signal<SimulacionEstudiante | null>(null);
  readonly intento = signal<IntentoEstudiante | null>(null);
  readonly resultados = signal<ResultadoEstudiante[]>([]);
  readonly resultadoSeleccionado = signal<ResultadoEstudiante | null>(null);
  readonly notificaciones = signal<AvisoNotificacion[]>([]);
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
  readonly noLeidas = computed(() => this.notificaciones().filter((aviso) => !aviso.leida).length);
  readonly indice = computed(() =>
    this.simulacion()?.preguntas.findIndex((pregunta) => pregunta.id === this.intento()?.preguntaActual) ?? -1,
  );
  readonly pregunta = computed(() => this.simulacion()?.preguntas[this.indice()]);
  readonly respondidas = computed(() => Object.keys(this.intento()?.respuestas ?? {}).length);
  readonly segundosRestantes = computed(() =>
    Math.max(0, Math.ceil((Math.min(
      Date.parse(this.intento()?.limite ?? ''),
      Date.parse(this.simulacion()?.actividad.finPrevisto ?? ''),
    ) - this.ahora()) / 1000)),
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
          (actividad) => actividad.estadoSesion === 'SIN_INICIAR' &&
            Date.parse(actividad.inicioPrevisto) <= ahora && Date.parse(actividad.finPrevisto) > ahora,
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
      if (!intento || intento.estado !== 'EN_PROGRESO') return;
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
      actividad.accion === 'RESULTADO'
    ) return false;
    return this.ahora() >= Date.parse(actividad.finPrevisto);
  }

  tiempoIngresoRestante(actividad: ActividadEstudiante) {
    const segundos = Math.max(0, Math.ceil((Date.parse(actividad.finPrevisto) - this.ahora()) / 1000));
    return this.formatearCuentaRegresiva(segundos);
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
      ]);
      this.actividades.set(actividades);
      this.resultados.set(resultados);
      this.notificaciones.set(notificaciones);
    });
  }

  async actualizarActividades() {
    await this.ejecutar(async () => {
      const [actividades, resultados] = await Promise.all([
        this.api.listarActividadesEstudiante(),
        this.api.listarResultadosEstudiante(),
      ]);
      this.actividades.set(actividades);
      this.resultados.set(resultados);
    });
  }

  async navegar(vista: 'actividades' | 'resultados' | 'notificaciones') {
    this.vista.set(vista);
    this.error.set('');
    await this.enrutador.navigate(['/estudiante', vista]);
    if (vista === 'actividades') await this.actualizarActividades();
    if (vista === 'resultados') this.resultados.set(await this.api.listarResultadosEstudiante());
    if (vista === 'notificaciones') {
      await this.ejecutar(async () => this.notificaciones.set(await this.api.listarNotificaciones(100)));
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
        await this.cargarInicio();
        this.verResultado(actualizado.id);
      }
    });
  }

  verResultado(idIntento: string) {
    const resultado = this.resultados().find((item) => item.idIntento === idIntento) ?? null;
    this.resultadoSeleccionado.set(resultado);
    this.vista.set('resultados');
    void this.enrutador.navigate(['/estudiante', 'resultados']);
  }

  async marcarLeida(aviso?: AvisoNotificacion) {
    if (aviso?.leida) return;
    await this.ejecutar(async () => {
      if (aviso) await this.api.marcarNotificacionLeida(aviso.id);
      else await this.api.marcarTodasNotificacionesLeidas();
      this.notificaciones.update((lista) =>
        lista.map((item) => (!aviso || item.id === aviso.id ? { ...item, leida: true } : item)),
      );
    });
  }

  salir() {
    this.api.salir();
    void this.enrutador.navigate(['/ingreso'], { replaceUrl: true });
  }

  private async refrescarIntento(id: string) {
    if (this.ocupado() || this.intento()?.id !== id) return;
    try {
      const intento = await this.api.consultar<IntentoEstudiante>(`/participaciones/${id}`);
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
