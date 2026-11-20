import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Caso, Escenario, Pregunta } from './servicio-api';

@Component({
  selector: 'psico-editor-caso',
  imports: [FormsModule],
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
  `],
})
export class EditorCaso {
  readonly caso = input.required<Caso>();
  readonly ocupado = input(false);
  readonly guardar = output<Caso>();

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
}
