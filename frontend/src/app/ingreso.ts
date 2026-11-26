import {
  Component,
  computed,
  effect,
  input,
  ElementRef,
  inject,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { AvisoNotificacion, ConfirmacionClaveInicialRequerida, Reporte, ServicioApi } from './servicio-api';
import { TutorialGuiado } from './tutorial-guiado';
import { pasosIngreso } from './pasos-tutorial';

interface Intento {
  id: string;
  limite: string;
  estado: string;
  modoNavegacion?: 'FLEXIBLE' | 'RESTRINGIDA';
  preguntaActual: string;
  respuestas: Record<string, string>;
}

interface Simulacion {
  nombre: string;
  intento: Intento;
  preguntas: {
    id: string;
    escenario: string;
    contexto: string;
    enunciado: string;
    opciones: { id: string; texto: string }[];
  }[];
}

@Component({
  selector: 'psico-ingreso',
  imports: [FormsModule, DatePipe, TutorialGuiado],
  templateUrl: './ingreso.html',
  styleUrl: './ingreso.css',
})
export class Ingreso {
  private readonly api = inject(ServicioApi);
  readonly autenticado = output<void>();
  readonly pasosTutorial = pasosIngreso;
  readonly explorar = output<void>();
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly mostrarClave = signal(false);
  readonly requiereCambioClave = signal(false);
  readonly avisoError = viewChild<ElementRef<HTMLElement>>('avisoError');
  correo = '';
  clave = '';
  nuevaClave = '';
  confirmarNuevaClave = '';

  readonly sesionEstudiante = input<string | null>(null);
  codigo = '';
  idSesion = '';

  constructor() {
    effect(() => {
      this.idSesion = this.sesionEstudiante() ?? '';
    });
    effect((alLimpiar) => {
      const intento = this.intento();
      if (intento?.estado !== 'EN_PROGRESO') return;

      let segundos = 0;
      const intervalo = setInterval(() => {
        this.ahora.set(Date.now());
        if (++segundos % 5 === 0) void this.actualizarIntento(intento.id);
      }, 1000);
      alLimpiar(() => clearInterval(intervalo));
    });
  }

  readonly simulacion = signal<Simulacion | null>(null);
  readonly intento = signal<Intento | null>(null);
  readonly resultado = signal<Reporte['resultado']>(null);
  readonly valoraciones = signal<Reporte['valoracionesGenerales']>([]);
  readonly notificaciones = signal<AvisoNotificacion[]>([]);
  readonly cargandoNotificaciones = signal(false);
  readonly errorNotificaciones = signal('');
  readonly marcandoNotificacion = signal<string | null>(null);
  readonly notificacionesNoLeidas = computed(
    () => this.notificaciones().filter((notificacion) => !notificacion.leida).length,
  );
  readonly confirmarEntrega = signal(false);
  private readonly ahora = signal(Date.now());
  private actualizandoIntento = false;
  readonly segundosRestantes = computed(() =>
    Math.max(0, Math.ceil((Date.parse(this.intento()?.limite ?? '') - this.ahora()) / 1000)),
  );
  readonly tiempoRestante = computed(() => {
    const segundos = this.segundosRestantes();
    return [Math.floor(segundos / 60), segundos % 60]
      .map((parte) => String(parte).padStart(2, '0'))
      .join(':');
  });
  readonly restringida = computed(() => this.intento()?.modoNavegacion === 'RESTRINGIDA');
  readonly respondidas = computed(() => Object.keys(this.intento()?.respuestas ?? {}).length);
  readonly porcentajeProgreso = computed(() => {
    const total = this.simulacion()?.preguntas.length ?? 0;
    return total ? Math.round((this.respondidas() / total) * 100) : 0;
  });
  readonly indice = computed(
    () =>
      this.simulacion()?.preguntas.findIndex(
        (pregunta) => pregunta.id === this.intento()?.preguntaActual,
      ) ?? -1,
  );
  readonly pregunta = computed(() => this.simulacion()?.preguntas[this.indice()]);
  preguntaRespondida(idPregunta: string) {
    return Boolean(this.intento()?.respuestas?.[idPregunta]);
  }
  async iniciar() {
    await this.ejecutar(async () => {
      const codigoAcceso = this.codigo.trim();
      await this.api.ingresar(this.correo.trim(), codigoAcceso, true);
      const simulacion = await this.api.enviar<Simulacion>('/participaciones/ingreso', {
        idSesion: this.idSesion.trim(),
        codigoAcceso,
      });
      this.codigo = '';
      this.ahora.set(Date.now());
      this.simulacion.set(simulacion);
      this.intento.set(simulacion.intento);
      if (simulacion.intento.estado !== 'EN_PROGRESO')
        await this.cargarResultado(simulacion.intento.id);
      await this.cargarNotificaciones();
    });
  }

  async cargarNotificaciones() {
    if (this.cargandoNotificaciones()) return;
    this.cargandoNotificaciones.set(true);
    this.errorNotificaciones.set('');
    try {
      this.notificaciones.set(await this.api.listarNotificaciones());
    } catch (error) {
      this.errorNotificaciones.set(
        error instanceof Error ? error.message : 'No fue posible consultar las notificaciones.',
      );
    } finally {
      this.cargandoNotificaciones.set(false);
    }
  }

  async marcarNotificacionLeida(notificacion: AvisoNotificacion) {
    if (notificacion.leida || this.marcandoNotificacion()) return;
    this.marcandoNotificacion.set(notificacion.id);
    this.errorNotificaciones.set('');
    try {
      await this.api.marcarNotificacionLeida(notificacion.id);
      this.notificaciones.update((lista) =>
        lista.map((actual) =>
          actual.id === notificacion.id ? { ...actual, leida: true } : actual,
        ),
      );
    } catch (error) {
      this.errorNotificaciones.set(
        error instanceof Error ? error.message : 'No fue posible marcar la notificación.',
      );
    } finally {
      this.marcandoNotificacion.set(null);
    }
  }

  async responder(idPregunta: string, idOpcion: string) {
    await this.operar('respuestas', { idPregunta, idOpcion }, 'PUT');
  }

  async operar(accion: string, datos: unknown = {}, metodo = 'POST') {
    await this.ejecutar(async () => {
      const intento = this.intento();
      if (!intento) return;
      this.intento.set(
        await this.api.enviar<Intento>(`/participaciones/${intento.id}/${accion}`, datos, metodo),
      );
      if (accion === 'finalizar') {
        this.confirmarEntrega.set(false);
        await this.cargarResultado(intento.id);
      }
    });
  }

  private async actualizarIntento(id: string) {
    if (this.actualizandoIntento || this.ocupado() || this.intento()?.id !== id) return;
    this.actualizandoIntento = true;
    try {
      const lectura = this.intento();
      const intento = await this.api.consultar<Intento>(`/participaciones/${id}`);
      if (this.ocupado() || this.intento() !== lectura || intento.estado === 'EN_PROGRESO') return;
      await this.ejecutar(async () => {
        this.intento.set(intento);
        this.confirmarEntrega.set(false);
        await this.cargarResultado(id);
      });
    } catch {
      // La siguiente consulta vuelve a comprobar el estado del intento.
    } finally {
      this.actualizandoIntento = false;
    }
  }

  enunciadoResultado(idPregunta: string) {
    return this.simulacion()?.preguntas.find((pregunta) => pregunta.id === idPregunta)?.enunciado;
  }

  async consultarResultado() {
    await this.ejecutar(async () => {
      const intento = this.intento();
      if (intento) await this.cargarResultado(intento.id);
    });
  }

  private async cargarResultado(id: string) {
    const evaluacion = await this.api.consultar<{ resultado: Reporte['resultado'] }>(
      `/evaluaciones/${id}`,
    );
    this.resultado.set(evaluacion.resultado);
    this.valoraciones.set(
      await this.api.consultar<Reporte['valoracionesGenerales']>(
        `/evaluaciones/${id}/valoraciones`,
      ),
    );
  }

  private async ejecutar(accion: () => Promise<void>) {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.error.set('');
    try {
      await accion();
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No se pudo completar la operación.');
    } finally {
      this.ocupado.set(false);
    }
  }
  async ingresar(formulario: NgForm) {
    if (this.ocupado()) return;
    this.error.set('');
    if (formulario.invalid || !this.correo.trim()) {
      formulario.control.markAllAsTouched();
      return;
    }
    this.ocupado.set(true);
    try {
      const ingreso = await this.api.ingresar(this.correo.trim(), this.clave);
      this.clave = '';
      if (ingreso.cambioClaveInicial) {
        this.requiereCambioClave.set(true);
        return;
      }
      this.autenticado.emit();
    } catch (error) {
      this.clave = '';
      this.mostrarClave.set(false);
      this.error.set(error instanceof Error ? error.message : 'No fue posible iniciar sesión.');
      setTimeout(() => this.avisoError()?.nativeElement.focus());
    } finally {
      this.ocupado.set(false);
    }
  }

  async cambiarClaveInicial(formulario: NgForm, confirmacion: HTMLDialogElement, conservar = false) {
    if (this.ocupado()) return;
    this.error.set('');
    if (formulario.invalid || this.nuevaClave !== this.confirmarNuevaClave) {
      formulario.control.markAllAsTouched();
      if (this.nuevaClave !== this.confirmarNuevaClave)
        this.error.set('Las contraseñas nuevas no coinciden.');
      return;
    }
    this.ocupado.set(true);
    const nuevaClave = this.nuevaClave;
    try {
      await this.api.cambiarClaveInicial(nuevaClave, conservar);
      this.api.salir();
      const ingreso = await this.api.ingresar(this.correo.trim(), nuevaClave);
      if (ingreso.cambioClaveInicial) throw new Error('No fue posible completar el cambio de contraseña.');
      this.nuevaClave = '';
      this.confirmarNuevaClave = '';
      this.requiereCambioClave.set(false);
      this.autenticado.emit();
    } catch (error) {
      if (!conservar && error instanceof ConfirmacionClaveInicialRequerida) confirmacion.showModal();
      else this.error.set(error instanceof Error && error.message ? error.message : 'No fue posible cambiar la contraseña.');
    } finally {
      this.ocupado.set(false);
    }
  }
}
