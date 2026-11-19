import { Component, ElementRef, OnInit, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ServicioApi, Rol, UsuarioAdministrado } from './servicio-api';

@Component({
  selector: 'psico-administracion',
  imports: [FormsModule],
  templateUrl: './administracion.html',
  styleUrl: './administracion.css',
})
export class Administracion implements OnInit {
  readonly api = inject(ServicioApi);
  readonly usuarios = signal<UsuarioAdministrado[]>([]);
  readonly roles = signal<Rol[]>([]);
  readonly seleccionado = signal<UsuarioAdministrado | null>(null);
  readonly vistaModal = signal<'' | 'detalle' | 'registro' | 'rol' | 'estado'>('');
  nuevoDocente = { nombre: '', correo: '', clave: '' };
  nuevoRol: Rol = 'DOCENTE';
  confirmarCambio = false;
  mostrarClave = false;
  readonly ocupado = signal(false);
  readonly cargado = signal(false);
  readonly error = signal('');
  readonly aviso = signal('');
  rol = '';
  estado = '';
  nombre = '';
  correo = '';
  editando = false;
  readonly ventanaDetalle = viewChild<ElementRef<HTMLDialogElement>>('ventanaDetalle');
  private origenDetalle: HTMLElement | null = null;

  ngOnInit() {
    void this.cargar();
  }
  async ejecutar(accion: () => Promise<void>) {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.error.set('');
    this.aviso.set('');
    try {
      await accion();
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No se pudo completar la operación.');
    } finally {
      this.ocupado.set(false);
    }
  }
  async cargar() {
    await this.ejecutar(async () => {
      this.cargado.set(false);
      const [usuarios, roles] = await Promise.all([
        this.api.listarUsuarios(this.rol, this.estado),
        this.api.listarRoles(),
      ]);
      this.usuarios.set(usuarios);
      this.roles.set(roles);
      this.seleccionado.set(null);
      this.cargado.set(true);
    });
  }
  async filtrar() {
    await this.ejecutar(async () => {
      const usuarios = await this.api.listarUsuarios(this.rol, this.estado);
      this.usuarios.set(usuarios);
      this.seleccionado.set(null);
    });
  }
  async consultar(usuario: UsuarioAdministrado, evento: Event) {
    if (this.ocupado()) return;
    this.origenDetalle = evento.currentTarget as HTMLElement;
    await this.ejecutar(async () => {
      const detalle = await this.api.consultarUsuario(usuario.id);
      this.seleccionado.set(detalle);
      this.nombre = detalle.nombre;
      this.correo = detalle.correo;
      this.editando = false;
    });
    if (!this.error() && this.seleccionado()?.id === usuario.id) {
      this.abrirVentana('detalle');
    }
  }
  abrirVentana(
    tipo: 'detalle' | 'registro' | 'rol' | 'estado',
    evento?: Event,
    usuario?: UsuarioAdministrado,
  ) {
    if (this.ocupado()) return;
    if (evento && !this.vistaModal()) this.origenDetalle = evento.currentTarget as HTMLElement;
    if (usuario) this.seleccionado.set(usuario);
    if (tipo === 'registro') {
      this.nuevoDocente = { nombre: '', correo: '', clave: '' };
      this.mostrarClave = false;
    }
    if (tipo === 'rol' && this.seleccionado()) this.nuevoRol = this.seleccionado()!.rol as Rol;
    this.confirmarCambio = false;
    this.error.set('');
    this.aviso.set('');
    this.vistaModal.set(tipo);
    setTimeout(() => {
      const ventana = this.ventanaDetalle()?.nativeElement;
      if (!ventana?.isConnected || !this.vistaModal()) return;
      if (!ventana.open) ventana.showModal();
      ventana.querySelector<HTMLElement>('#detalle-usuario')?.focus();
    });
  }
  cerrarDetalle(evento?: Event) {
    evento?.preventDefault();
    if (this.ocupado()) return;
    this.ventanaDetalle()?.nativeElement.close();
    this.vistaModal.set('');
    this.seleccionado.set(null);
    this.nuevoDocente = { nombre: '', correo: '', clave: '' };
    this.mostrarClave = false;
    this.confirmarCambio = false;
    this.editando = false;
    this.error.set('');
    this.aviso.set('');
    setTimeout(() => {
      if (this.origenDetalle?.isConnected) this.origenDetalle.focus();
      else document.querySelector<HTMLElement>('#actualizar-usuarios')?.focus();
    });
  }
  cerrarDesdeFondo(evento: MouseEvent) {
    const ventana = this.ventanaDetalle()?.nativeElement;
    if (!ventana || evento.target !== ventana) return;
    const limites = ventana.getBoundingClientRect();
    if (
      evento.clientX < limites.left ||
      evento.clientX > limites.right ||
      evento.clientY < limites.top ||
      evento.clientY > limites.bottom
    )
      this.cerrarDetalle();
  }
  mantenerFocoDetalle(evento: Event) {
    const teclado = evento as KeyboardEvent;
    if (teclado.key !== 'Tab') return;
    const ventana = this.ventanaDetalle()?.nativeElement;
    if (!ventana) return;
    const controles = Array.from(
      ventana.querySelectorAll<HTMLElement>(
        'button:not(:disabled), input:not(:disabled), select:not(:disabled)',
      ),
    );
    const primero = controles[0];
    const ultimo = controles.at(-1);
    if (!primero) {
      evento.preventDefault();
      return;
    }
    if (
      teclado.shiftKey &&
      (document.activeElement === primero || document.activeElement?.id === 'detalle-usuario')
    ) {
      evento.preventDefault();
      ultimo?.focus();
    } else if (!teclado.shiftKey && document.activeElement === ultimo) {
      evento.preventDefault();
      primero.focus();
    }
  }
  editarDetalle(usuario: UsuarioAdministrado) {
    this.nombre = usuario.nombre;
    this.correo = usuario.correo;
    this.editando = true;
    this.error.set('');
    this.aviso.set('');
    setTimeout(() =>
      this.ventanaDetalle()?.nativeElement.querySelector<HTMLInputElement>('input')?.focus(),
    );
  }
  cancelarEdicion() {
    this.editando = false;
    this.error.set('');
    setTimeout(() =>
      this.ventanaDetalle()?.nativeElement.querySelector<HTMLElement>('.editar-detalle')?.focus(),
    );
  }
  async guardar() {
    const usuario = this.seleccionado();
    if (!usuario || usuario.rol !== 'DOCENTE') return;
    await this.ejecutar(async () => {
      const actualizado = await this.api.editarDocente(usuario.id, {
        nombre: this.nombre,
        correo: this.correo,
      });
      this.actualizar(actualizado);
      this.editando = false;
      this.aviso.set('Datos del docente actualizados.');
      setTimeout(() => document.querySelector<HTMLElement>('#detalle-usuario')?.focus());
    });
  }
  private actualizar(usuario: UsuarioAdministrado) {
    this.usuarios.update((usuarios) =>
      usuarios
        .map((anterior) => (anterior.id === usuario.id ? usuario : anterior))
        .filter(
          (actual) =>
            (!this.rol || actual.rol === this.rol) &&
            (!this.estado || String(actual.activo) === this.estado),
        ),
    );
    if (this.seleccionado()?.id === usuario.id) this.seleccionado.set(usuario);
  }
  async cambiarEstado() {
    if (this.ocupado()) return;
    const usuario = this.seleccionado();
    if (!usuario || usuario.id === this.api.usuario()?.id) return;
    await this.ejecutar(async () => {
      const actualizado = await this.api.cambiarEstadoUsuario(usuario.id, !usuario.activo);
      this.actualizar(actualizado);
    });
    if (!this.error()) {
      this.cerrarDetalle();
      this.aviso.set(
        usuario.activo ? 'Cuenta desactivada. Su historial se conserva.' : 'Cuenta reactivada.',
      );
    }
  }
  async registrarDocente() {
    if (this.ocupado()) return;
    const clave = this.nuevoDocente.clave;
    if (clave.length < 8 || clave.length > 72 || new TextEncoder().encode(clave).length > 72) {
      this.error.set(
        'La contraseña debe tener al menos 8 caracteres y ocupar como máximo 72 bytes.',
      );
      return;
    }
    await this.ejecutar(async () => {
      const creado = await this.api.registrarDocente({
        nombre: this.nuevoDocente.nombre.trim(),
        correo: this.nuevoDocente.correo.trim(),
        clave,
      });
      const usuario: UsuarioAdministrado = { ...creado, activo: true };
      this.usuarios.update((usuarios) =>
        [usuario, ...usuarios].filter(
          (actual) =>
            (!this.rol || actual.rol === this.rol) &&
            (!this.estado || String(actual.activo) === this.estado),
        ),
      );
    });
    if (!this.error()) {
      this.cerrarDetalle();
      this.aviso.set('Docente registrado. Puedes encontrar su cuenta con el filtro de docentes.');
    }
  }
  async cambiarRol() {
    if (this.ocupado()) return;
    const usuario = this.seleccionado();
    if (
      !usuario ||
      usuario.id === this.api.usuario()?.id ||
      usuario.rol === this.nuevoRol ||
      !this.confirmarCambio
    )
      return;
    await this.ejecutar(async () => {
      const actualizado = await this.api.cambiarRolUsuario(usuario.id, this.nuevoRol);
      this.actualizar({ ...actualizado, activo: usuario.activo });
    });
    if (!this.error()) {
      this.abrirVentana('detalle');
      this.aviso.set('Rol actualizado. Los permisos de la cuenta corresponden a su nuevo rol.');
    }
  }
}
