import { Component, ElementRef, inject, output, signal, viewChild } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ServicioApi } from './servicio-api';

@Component({
  selector: 'psico-ingreso',
  imports: [FormsModule],
  templateUrl: './ingreso.html',
  styleUrl: './ingreso.css',
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
