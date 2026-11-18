import { Component, OnInit, inject, signal } from '@angular/core';
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
  readonly permisos = signal<{ rol: Rol; habilitado: boolean }[]>([]);
  readonly seleccionado = signal<UsuarioAdministrado | null>(null);
  readonly confirmacion = signal<UsuarioAdministrado | null>(null);
  readonly confirmarPermiso = signal(false);
  readonly ocupado = signal(false);
  readonly cargado = signal(false);
  readonly error = signal('');
  readonly aviso = signal('');
  rol = '';
  estado = '';
  nombre = '';
  correo = '';
  editando = false;

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
      const permisos = await Promise.all(
        roles.map(async (rol) => ({ rol, ...(await this.api.consultarPermiso(rol)) })),
      );
      this.usuarios.set(usuarios);
      this.roles.set(roles);
      this.permisos.set(permisos);
      this.seleccionado.set(null);
      this.confirmacion.set(null);
      this.confirmarPermiso.set(false);
      this.cargado.set(true);
    });
  }
  async filtrar() {
    await this.ejecutar(async () => {
      const usuarios = await this.api.listarUsuarios(this.rol, this.estado);
      this.usuarios.set(usuarios);
      this.seleccionado.set(null);
      this.confirmacion.set(null);
    });
  }
  async consultar(usuario: UsuarioAdministrado) {
    await this.ejecutar(async () => {
      const detalle = await this.api.consultarUsuario(usuario.id);
      this.seleccionado.set(detalle);
      this.nombre = detalle.nombre;
      this.correo = detalle.correo;
      this.editando = false;
      setTimeout(() => document.querySelector<HTMLElement>('#detalle-usuario')?.focus());
    });
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
    const usuario = this.confirmacion();
    if (!usuario) return;
    await this.ejecutar(async () => {
      const actualizado = await this.api.cambiarEstadoUsuario(usuario.id, !usuario.activo);
      this.actualizar(actualizado);
      this.confirmacion.set(null);
      this.aviso.set(
        actualizado.activo ? 'Cuenta reactivada.' : 'Cuenta desactivada. Su historial se conserva.',
      );
    });
  }
  async cambiarPermiso() {
    const anterior = this.permisos().find((permiso) => permiso.rol === 'DOCENTE');
    if (!anterior) return;
    await this.ejecutar(async () => {
      await this.api.configurarPermiso(!anterior.habilitado);
      this.permisos.update((permisos) =>
        permisos.map((permiso) =>
          permiso.rol === 'DOCENTE' ? { ...permiso, habilitado: !anterior.habilitado } : permiso,
        ),
      );
      this.usuarios.update((usuarios) =>
        usuarios.map((usuario) =>
          usuario.rol === 'DOCENTE'
            ? { ...usuario, puedeCrearCasos: !anterior.habilitado }
            : usuario,
        ),
      );
      const seleccionado = this.seleccionado();
      if (seleccionado?.rol === 'DOCENTE')
        this.seleccionado.set({ ...seleccionado, puedeCrearCasos: !anterior.habilitado });
      this.confirmarPermiso.set(false);
      this.aviso.set('Permiso actualizado para todos los docentes.');
    });
  }
}
