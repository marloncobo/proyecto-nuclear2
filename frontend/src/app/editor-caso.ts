import { DecimalPipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Caso, Escenario, Pregunta, Rubrica } from './servicio-api';

@Component({
  selector: 'psico-editor-caso',
  imports: [DecimalPipe, FormsModule],
  templateUrl: './editor-caso.html',
  styles: [`
    form, fieldset, label { display: grid; gap: .65rem; min-width: 0; }
    form { margin-top: 1rem; }
    fieldset { border: 1px solid var(--borde); border-radius: .6rem; }
    label { font-weight: 600; }
    input, textarea, select { width: 100%; border: 1px solid #82928d; border-radius: .45rem; padding: .65rem; }
    button { justify-self: start; color: var(--azul); background: var(--salvia);
      padding: .65rem 1rem; border: 1px solid var(--azul); border-radius: .45rem; }
    .primario { color: white; background: var(--azul); }
    .dos-columnas { display: grid; gap: .65rem; grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr)); }
    .desplegable { min-width: 0; border: 1px solid var(--borde); border-radius: .6rem; }
    .desplegable summary {
      padding: .75rem .85rem; cursor: pointer; color: var(--azul);
      font-size: 1.125rem; font-weight: 750; overflow-wrap: anywhere;
    }
    .desplegable summary:focus-visible { outline: 2px solid #669098; outline-offset: -2px; }
    .desplegable[open] summary { border-bottom: 1px solid var(--borde); }
    .contenido-desplegable { display: grid; gap: .65rem; padding: .75rem .85rem; }
    .suma-ponderaciones {
      justify-self: end; display: grid; gap: .15rem; padding: .35rem .65rem;
      border: 1px solid currentColor; border-radius: .45rem; text-align: center;
      font-size: .75rem; color: #873900; background: #fff1df;
    }
    .suma-ponderaciones.completa { color: #20553b; background: #e8f4eb; }
    .suma-ponderaciones strong { font-size: 1.1rem; font-variant-numeric: tabular-nums; }
    .error-guardado {
      margin: 0; padding: .65rem .85rem; border-radius: .45rem;
      color: #8c443a; background: #f7e9e5; font-size: .875rem; overflow-wrap: anywhere;
    }
    .preguntas-criterio legend { font-weight: 600; }
    .grupo-preguntas { display: grid; gap: .2rem; }
    .grupo-preguntas strong { color: var(--azul); font-size: .875rem; overflow-wrap: anywhere; }
    .opcion-pregunta {
      grid-template-columns: auto minmax(0, 1fr); align-items: start;
      padding: .35rem 0; cursor: pointer; font-weight: 400; overflow-wrap: anywhere;
    }
    .opcion-pregunta input {
      width: 1.1rem; height: 1.1rem; margin: .1rem 0 0; padding: 0;
      accent-color: var(--azul); cursor: pointer;
    }
  `],
})
export class EditorCaso {
  readonly caso = input.required<Caso>();
  readonly ocupado = input(false);
  readonly error = input('');
  readonly cambio = output<Caso>();
  readonly guardar = output<Caso>();

  notificarCambio() {
    this.cambio.emit(this.caso());
  }

  sumaPonderaciones() {
    const total = this.caso().rubrica?.criterios.reduce(
      (suma, criterio) => suma + (criterio.ponderacion ?? 0), 0,
    ) ?? 0;
    return Number(total.toFixed(12));
  }

  static nuevo(): Caso {
    const pregunta = EditorCaso.preguntaNueva(1);
    return {
      id: crypto.randomUUID(),
      nombre: '',
      contexto: '',
      desarrolloSituacional: '',
      tiempoEstimadoMin: 40,
      estado: 'BORRADOR',
      escenarios: [{ id: crypto.randomUUID(), titulo: '', contextoSituacional: '', orden: 1, preguntas: [pregunta] }],
      rubrica: {
        id: crypto.randomUUID(), nombre: 'Comprensión del escenario', escalaMaxima: 5,
        criterios: [{ id: crypto.randomUUID(), nombre: 'Análisis de la situación', ponderacion: 1, preguntas: [pregunta.id] }],
      },
    };
  }

  private static preguntaNueva(orden: number): Pregunta {
    return {
      id: crypto.randomUUID(), enunciado: '', orden,
      opciones: [true, false].map((esCorrecta) => ({
        id: crypto.randomUUID(), texto: '', esCorrecta, justificacion: '',
      })),
    };
  }

  agregarEscenario() {
    const escenarios = this.caso().escenarios;
    const escenario = {
      id: crypto.randomUUID(), titulo: '', contextoSituacional: '',
      orden: escenarios.length + 1, preguntas: [],
    };
    escenarios.push(escenario);
    this.agregarPregunta(escenario);
  }

  agregarPregunta(escenario: Escenario) {
    const pregunta = EditorCaso.preguntaNueva(escenario.preguntas.length + 1);
    escenario.preguntas.push(pregunta);
    const criterios = this.caso().rubrica?.criterios;
    if (criterios?.length === 1) criterios[0].preguntas.push(pregunta.id);
  }

  quitarEscenario(indice: number) {
    const escenarios = this.caso().escenarios;
    const [eliminado] = escenarios.splice(indice, 1);
    escenarios.forEach((escenario, posicion) => escenario.orden = posicion + 1);
    this.quitarReferencias(eliminado.preguntas.map((pregunta) => pregunta.id));
  }

  quitarPregunta(escenario: Escenario, indice: number) {
    const [eliminada] = escenario.preguntas.splice(indice, 1);
    escenario.preguntas.forEach((pregunta, posicion) => pregunta.orden = posicion + 1);
    this.quitarReferencias([eliminada.id]);
  }

  private quitarReferencias(ids: string[]) {
    for (const criterio of this.caso().rubrica?.criterios ?? []) {
      criterio.preguntas = criterio.preguntas.filter((id) => !ids.includes(id));
    }
  }

  agregarOpcion(pregunta: Pregunta) {
    pregunta.opciones.push({ id: crypto.randomUUID(), texto: '', esCorrecta: false, justificacion: '' });
  }

  marcarCorrecta(pregunta: Pregunta, id: string) {
    pregunta.opciones.forEach((opcion) => opcion.esCorrecta = opcion.id === id);
  }

  agregarRubrica() {
    this.caso().rubrica = { id: crypto.randomUUID(), nombre: '', escalaMaxima: 5, criterios: [] };
    this.agregarCriterio();
  }

  agregarCriterio() {
    this.caso().rubrica?.criterios.push({ id: crypto.randomUUID(), nombre: '', ponderacion: 0, preguntas: [] });
  }

  asignarPregunta(criterio: Rubrica['criterios'][number], idPregunta: string, seleccionada: boolean) {
    criterio.preguntas = seleccionada
      ? [...criterio.preguntas, idPregunta]
      : criterio.preguntas.filter((id) => id !== idPregunta);
  }
}
