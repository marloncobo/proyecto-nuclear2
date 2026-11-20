import { Component, computed, effect, input, ElementRef, inject, output, signal, viewChild } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { Reporte, ServicioApi } from './servicio-api';

interface Intento {
  id: string;
  limite: string;
  estado: string;
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
  imports: [FormsModule, DatePipe],
  templateUrl: './ingreso.html',
  styleUrl: './ingreso.css',
  styles: [`
    .ingreso-estudiante { max-width: 48rem; margin: 3rem auto; padding: clamp(1rem, 5vw, 3rem); background: white;
      border: 1px solid var(--borde); border-radius: 1.25rem; }
    .ingreso-estudiante img { width: 9rem; }
    .ingreso-estudiante :is(form, label, fieldset) { display: grid; gap: .6rem; }
    .ingreso-estudiante form { gap: 1rem; }
    .ingreso-estudiante input:not([type=radio]) { padding: .7rem; border: 1px solid #82928d; border-radius: .45rem; }
    .ingreso-estudiante fieldset label { display: flex; align-items: baseline; padding: .35rem; }
    .ingreso-estudiante :is(button, a) { color: var(--azul); }
    .ingreso-estudiante button { padding: .65rem 1rem; border: 1px solid var(--azul); border-radius: .45rem; background: var(--salvia); }
    .ingreso-estudiante footer { display: flex; flex-wrap: wrap; gap: .75rem; margin-top: 1.5rem; }
    .ingreso-estudiante [role=alert] { color: #a22126; }
  `],
})
export class Ingreso {
  private readonly api = inject(ServicioApi);
  readonly autenticado = output<void>();
  readonly explorar = output<void>();
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly mostrarClave = signal(false);
  readonly avisoError = viewChild<ElementRef<HTMLElement>>('avisoError');
  correo = '';
  clave = '';

  readonly sesionEstudiante = input<string | null>(null);
  codigo = '';
  idSesion = '';

  constructor() {
    effect(() => {
      this.idSesion = this.sesionEstudiante() ?? '';
    });
  }

  readonly simulacion = signal<Simulacion | null>(null);
  readonly intento = signal<Intento | null>(null);
  readonly resultado = signal<Reporte['resultado']>(null);
  readonly valoraciones = signal<Reporte['valoracionesGenerales']>([]);
  readonly confirmarEntrega = signal(false);
  readonly indice = computed(() =>
    this.simulacion()?.preguntas.findIndex((pregunta) => pregunta.id === this.intento()?.preguntaActual) ?? -1,
  );
  readonly pregunta = computed(() => this.simulacion()?.preguntas[this.indice()]);
  async iniciar() {
    await this.ejecutar(async () => {
      const codigoAcceso = this.codigo.trim();
      await this.api.ingresar(this.correo.trim(), codigoAcceso, true);
      const simulacion = await this.api.enviar<Simulacion>('/participaciones/ingreso', {
        idSesion: this.idSesion.trim(), codigoAcceso,
      });
      this.codigo = '';
      this.simulacion.set(simulacion);
      this.intento.set(simulacion.intento);
      if (simulacion.intento.estado !== 'EN_PROGRESO') await this.cargarResultado(simulacion.intento.id);
    });
  }

  async responder(idPregunta: string, idOpcion: string) {
    await this.operar('respuestas', { idPregunta, idOpcion }, 'PUT');
  }

  async operar(accion: string, datos: unknown = {}, metodo = 'POST') {
    await this.ejecutar(async () => {
      const intento = this.intento();
      if (!intento) return;
      this.intento.set(await this.api.enviar<Intento>(
        `/participaciones/${intento.id}/${accion}`, datos, metodo,
      ));
      if (accion === 'finalizar') {
        this.confirmarEntrega.set(false);
        await this.cargarResultado(intento.id);
      }
    });
  }

  async consultarResultado() {
    await this.ejecutar(async () => {
      const intento = this.intento();
      if (intento) await this.cargarResultado(intento.id);
    });
  }

  private async cargarResultado(id: string) {
    const evaluacion = await this.api.consultar<{ resultado: Reporte['resultado'] }>(`/evaluaciones/${id}`);
    this.resultado.set(evaluacion.resultado);
    this.valoraciones.set(await this.api.consultar<Reporte['valoracionesGenerales']>(`/evaluaciones/${id}/valoraciones`));
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
      await this.api.ingresar(this.correo.trim(), this.clave);
      this.clave = '';
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
}
