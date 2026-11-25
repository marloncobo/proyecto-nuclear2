import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ServicioApi } from './servicio-api';

@Component({
  selector: 'psico-cambio-clave',
  imports: [FormsModule],
  template: `
    <h3>Cambiar contraseña</h3>
    <form #formulario="ngForm" (ngSubmit)="guardar(formulario)">
      <label>
        Contraseña actual
        <input type="password" name="actual" [(ngModel)]="actual"
          autocomplete="current-password" maxlength="72" required />
      </label>
      <label>
        Nueva contraseña
        <input type="password" name="nueva" [(ngModel)]="nueva"
          autocomplete="new-password" minlength="8" maxlength="72" required />
      </label>
      <label>
        Confirmar nueva contraseña
        <input type="password" name="confirmacion" [(ngModel)]="confirmacion"
          autocomplete="new-password" maxlength="72" required />
      </label>
      @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
      @if (aviso()) { <p class="aviso" role="status">{{ aviso() }}</p> }
      <button type="submit" [disabled]="ocupado() || formulario.invalid">Guardar contraseña</button>
    </form>
  `,
  styles: `
    :host { display: block; margin: 1.25rem 0; }
    h3 { margin: 0 0 .75rem; }
    form { gap: .8rem; }
    button {
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
  actual = '';
  nueva = '';
  confirmacion = '';

  reiniciar() {
    this.generacion++;
    this.actual = this.nueva = this.confirmacion = '';
    this.error.set('');
    this.aviso.set('');
    this.ocupado.set(false);
  }

  async guardar(formulario: NgForm) {
    if (formulario.invalid || this.ocupado()) return;
    this.error.set('');
    this.aviso.set('');
    if (this.nueva !== this.confirmacion) {
      this.error.set('Las contraseñas nuevas no coinciden.');
      return;
    }
    const generacion = this.generacion;
    this.ocupado.set(true);
    try {
      await this.api.cambiarClave(this.actual, this.nueva);
      if (this.generacion !== generacion) return;
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
