import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ServicioApi } from './servicio-api';

@Component({
  selector: 'psico-cambio-clave',
  imports: [FormsModule],
  template: `
    <h3>Cambiar contraseña</h3>
    <form #formulario="ngForm" (ngSubmit)="guardar(formulario)">
      @for (campo of campos; track campo.nombre) {
        <div class="fila-clave">
          <label [for]="'cambio-clave-' + campo.nombre">{{ campo.etiqueta }}</label>
          <span class="campo-clave">
            <input [id]="'cambio-clave-' + campo.nombre" [type]="visible[campo.nombre] ? 'text' : 'password'"
              [name]="campo.nombre" [(ngModel)]="claves[campo.nombre]"
              [attr.autocomplete]="campo.autocompletar" [minlength]="campo.minimo"
              maxlength="72" required />
            <button type="button" class="alternar-clave"
              [attr.aria-label]="(visible[campo.nombre] ? 'Ocultar ' : 'Mostrar ') + campo.descripcion"
              [attr.aria-pressed]="visible[campo.nombre]"
              (click)="visible[campo.nombre] = !visible[campo.nombre]">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none"
                stroke="currentColor" stroke-width="1.75" stroke-linecap="round"
                stroke-linejoin="round" aria-hidden="true">
                <path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6Z" />
                <circle cx="12" cy="12" r="2.5" />
                @if (visible[campo.nombre]) { <path d="m4 3 16 18" /> }
              </svg>
            </button>
          </span>
        </div>
      }
      @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
      @if (aviso()) { <p class="aviso" role="status">{{ aviso() }}</p> }
      <button type="submit" [disabled]="ocupado() || formulario.invalid">Guardar contraseña</button>
    </form>
  `,
  styles: `
    :host { display: block; margin: 1.25rem 0; }
    h3 { margin: 0 0 .75rem; }
    form { gap: .8rem; }
    .fila-clave { display: grid; gap: .5rem; }
    .campo-clave { position: relative; display: block; }
    .campo-clave input { width: 100%; padding-inline-end: 3.5rem; }
    .campo-clave input::-ms-reveal { display: none; }
    .alternar-clave {
      position: absolute; inset-inline-end: .4rem; top: 50%; transform: translateY(-50%);
      display: grid; place-items: center; width: 40px; height: 40px;
      padding: 0; border: 0; border-radius: 5px; color: var(--azul); background: var(--salvia);
    }
    button[type='submit'] {
      justify-self: start; padding: .65rem 1rem; border: 0; border-radius: 8px;
      background: var(--azul); color: white; font-weight: 700;
    }
    p { margin: 0; padding: .65rem .85rem; border-radius: 8px; font-size: .875rem; }
    .error { background: #f7e9e5; color: #8c443a; }
    .aviso { background: var(--salvia); color: #194a50; }
  `,
})
export class CambioClave {
  private readonly api = inject(ServicioApi);
  private generacion = 0;
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly aviso = signal('');
  readonly campos = [
    { nombre: 'actual', etiqueta: 'Contraseña actual', descripcion: 'contraseña actual', autocompletar: 'current-password', minimo: 0 },
    { nombre: 'nueva', etiqueta: 'Nueva contraseña', descripcion: 'nueva contraseña', autocompletar: 'new-password', minimo: 8 },
    { nombre: 'confirmacion', etiqueta: 'Confirmar nueva contraseña', descripcion: 'confirmación de la nueva contraseña', autocompletar: 'new-password', minimo: 0 },
  ] as const;
  claves = { actual: '', nueva: '', confirmacion: '' };
  visible = { actual: false, nueva: false, confirmacion: false };

  reiniciar() {
    this.generacion++;
    this.claves = { actual: '', nueva: '', confirmacion: '' };
    this.visible = { actual: false, nueva: false, confirmacion: false };
    this.error.set('');
    this.aviso.set('');
    this.ocupado.set(false);
  }

  async guardar(formulario: NgForm) {
    if (formulario.invalid || this.ocupado()) return;
    this.error.set('');
    this.aviso.set('');
    if (this.claves.nueva !== this.claves.confirmacion) {
      this.error.set('Las contraseñas nuevas no coinciden.');
      return;
    }
    const generacion = this.generacion;
    this.ocupado.set(true);
    try {
      await this.api.cambiarClave(this.claves.actual, this.claves.nueva);
      if (this.generacion !== generacion) return;
      this.visible = { actual: false, nueva: false, confirmacion: false };
      formulario.resetForm({ actual: '', nueva: '', confirmacion: '' });
      this.aviso.set('Contraseña actualizada.');
    } catch (error) {
      if (this.generacion === generacion)
        this.error.set(error instanceof Error ? error.message : 'No se pudo cambiar la contraseña.');
    } finally {
      if (this.generacion === generacion) this.ocupado.set(false);
    }
  }
}
