import { Component, computed, input } from '@angular/core';

export function nombreEtiqueta(valor: string): string {
  const nombres: Record<string, string> = {
    ADMINISTRADOR: 'Administrador',
    DOCENTE: 'Docente',
    ESTUDIANTE: 'Estudiante',
    ACTIVO: 'Activo',
    INACTIVO: 'Inactivo',
  };
  return nombres[valor] ?? valor;
}

@Component({
  selector: 'psico-etiqueta-usuario',
  template: `<span
    class="etiqueta"
    [class.administrador]="valor() === 'ADMINISTRADOR'"
    [class.docente]="valor() === 'DOCENTE'"
    [class.estudiante]="valor() === 'ESTUDIANTE'"
    [class.activo]="valor() === 'ACTIVO'"
    [class.inactivo]="valor() === 'INACTIVO'"
    >{{ texto() }}</span
  >`,
  styleUrl: './etiqueta-usuario.css',
})
export class EtiquetaUsuario {
  readonly valor = input.required<string>();
  readonly texto = computed(() => nombreEtiqueta(this.valor()));
}
