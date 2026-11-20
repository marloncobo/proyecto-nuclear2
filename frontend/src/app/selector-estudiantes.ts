import { Component, computed, input, model, signal } from '@angular/core';
import { Usuario } from './servicio-api';

@Component({
  selector: 'psico-selector-estudiantes',
  template: `
    <label>
      Filtrar por nombre o correo
      <input type="search" [value]="filtro()" (input)="filtro.set($any($event.target).value)" />
    </label>
    <p aria-live="polite">{{ seleccion().length }} {{ seleccion().length === 1 ? 'seleccionado' : 'seleccionados' }}</p>
    <ul>
      @for (estudiante of visibles(); track estudiante.id) {
        <li>
          <label>
            <input type="checkbox" [checked]="seleccion().includes(estudiante.id)"
              (change)="seleccionar(estudiante.id, $any($event.target).checked)" />
            <span>{{ estudiante.nombre }}<small>{{ estudiante.correo }}</small></span>
          </label>
        </li>
      } @empty {
        <li>{{ filtro().trim() ? 'No hay coincidencias.' : 'No hay estudiantes disponibles.' }}</li>
      }
    </ul>
  `,
  styles: [`
    :host { display: block; }
    label { display: grid; gap: .35rem; }
    input[type='search'] { width: 100%; padding: .55rem; border: 1px solid #82928d; border-radius: .45rem; }
    p { margin: .55rem 0; }
    ul { list-style: none; padding: 0; margin: 0; max-height: 14rem; overflow-y: auto; }
    li { padding: .4rem 0; overflow-wrap: anywhere; }
    li label { grid-template-columns: auto 1fr; gap: .6rem; align-items: center; cursor: pointer; }
    small { display: block; }
  `],
})
export class SelectorEstudiantes {
  readonly estudiantes = input.required<Usuario[]>();
  readonly seleccion = model.required<string[]>();
  readonly filtro = signal('');
  readonly visibles = computed(() => {
    const texto = this.filtro().trim().toLocaleLowerCase();
    return this.estudiantes().filter((estudiante) =>
      [estudiante.nombre, estudiante.correo].some((dato) => dato.toLocaleLowerCase().includes(texto)),
    );
  });

  seleccionar(id: string, marcado: boolean) {
    this.seleccion.update((ids) => marcado ? [...new Set([...ids, id])] : ids.filter((actual) => actual !== id));
  }
}
