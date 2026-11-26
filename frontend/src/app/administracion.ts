import { Component, ElementRef, OnInit, computed, effect, inject, input, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ServicioApi, Rol, UsuarioAdministrado } from './servicio-api';
import { EtiquetaUsuario, nombreEtiqueta } from './etiqueta-usuario';
import { TutorialGuiado } from './tutorial-guiado';
import { pasosModalesAdministracion, pasosModalesEstudiantesDocente } from './pasos-tutorial';

@Component({
  selector: 'psico-administracion',
  imports: [FormsModule, EtiquetaUsuario, TutorialGuiado],
  templateUrl: './administracion.html',
  styleUrl: './administracion.css',
})
export class Administracion implements OnInit {
  readonly soloEstudiantes = input(false);
  readonly api = inject(ServicioApi);
  readonly usuarios = signal<UsuarioAdministrado[]>([]);
  readonly busqueda = signal('');
  readonly busquedaAplicada = signal('');
  readonly requiereActualizar = signal(false);
  readonly requiereRecargarDetalle = signal(false);
  readonly rolAplicado = signal('');
  readonly estadoAplicado = signal('');
  private readonly usuariosPorPagina = 10;
  private readonly paginaSolicitada = signal(1);
  readonly totalPaginas = computed(() => Math.max(1, Math.ceil(this.usuarios().length / this.usuariosPorPagina)));
  readonly paginaActual = computed(() => Math.min(this.paginaSolicitada(), this.totalPaginas()));
  readonly usuariosVisibles = computed(() => {
    const inicio = (this.paginaActual() - 1) * this.usuariosPorPagina;
    return this.usuarios().slice(inicio, inicio + this.usuariosPorPagina);
  });
  readonly nombreEtiqueta = nombreEtiqueta;
  readonly pasosTutorial = computed(() =>
    this.soloEstudiantes() ? pasosModalesEstudiantesDocente : pasosModalesAdministracion,
  );
  readonly confirmarPermiso = signal(false);

  prepararCambioPermiso(confirmar: boolean) {
    this.confirmarPermiso.set(confirmar);
    this.error.set('');
    const selector = confirmar ? '#confirmar-permiso-docente' : '#cambiar-permiso-docente';
    setTimeout(() => this.ventanaDetalle()?.nativeElement.querySelector<HTMLElement>(selector)?.focus());
  }

  async cambiarPermiso() {
    const usuario = this.seleccionado();
    if (usuario?.rol !== 'DOCENTE' || !this.confirmarPermiso()) return;
    await this.ejecutar(async () => {
      const actualizado = await this.api.configurarPermiso(usuario.id, !usuario.puedeCrearCasos);
      this.actualizar(actualizado);
      this.confirmarPermiso.set(false);
      this.aviso.set('Permiso de creación actualizado.');
      setTimeout(() => this.ventanaDetalle()?.nativeElement
        .querySelector<HTMLElement>('#detalle-usuario')?.focus());
    });
  }

  readonly roles = signal<Rol[]>([]);
  readonly seleccionado = signal<UsuarioAdministrado | null>(null);
  readonly vistaModal = signal<'' | 'detalle' | 'registro' | 'rol' | 'estado'>('');
  nuevoPerfil = { nombre: '', correo: '', clave: '' };
  tipoRegistro: 'DOCENTE' | 'ESTUDIANTE' = 'DOCENTE';
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

  constructor() {
    effect((limpiar) => {
      if (!this.aviso() || this.vistaModal()) return;
      const temporizador = setTimeout(() => this.aviso.set(''), 8000);
      limpiar(() => clearTimeout(temporizador));
    });
  }
  cambiarPagina(desplazamiento: number) {
    const pagina = this.paginaActual() + desplazamiento;
    if (pagina >= 1 && pagina <= this.totalPaginas()) this.paginaSolicitada.set(pagina);
  }
  private async consultarListado(rol: string, estado: string, busqueda: string) {
    if (!this.soloEstudiantes()) return this.api.listarUsuarios(rol, estado, busqueda);
    const estudiantes = await this.api.listarEstudiantesParaEditar();
    const consulta = busqueda.toLocaleLowerCase('es');
    return estudiantes.filter((estudiante) =>
      (!estado || estudiante.activo === (estado === 'true')) &&
      [estudiante.nombre, estudiante.correo].some((dato) => dato.toLocaleLowerCase('es').includes(consulta)),
    );
  }

  private async refrescarListado() {
    try {
      const usuarios = await this.consultarListado(
        this.rolAplicado(),
        this.estadoAplicado(),
        this.busquedaAplicada(),
      );
      this.usuarios.set(usuarios);
      this.paginaSolicitada.set(this.paginaActual());
      this.requiereActualizar.set(false);
    } catch {
      // La escritura ya se confirmó: no presentar un fallo de lectura como si no se hubiese guardado.
      this.requiereActualizar.set(true);
    }
  }
  async limpiarFiltros() {
    if (this.ocupado()) return;
    this.rol = '';
    this.estado = '';
    this.busqueda.set('');
    await this.filtrar();
  }

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
      const rol = this.rol,
        estado = this.estado,
        busqueda = this.busqueda().trim();
      this.cargado.set(false);
      const [usuarios, roles] = await Promise.all([
        this.consultarListado(rol, estado, busqueda),
        this.soloEstudiantes() ? [] : this.api.listarRoles(),
      ]);
      this.usuarios.set(usuarios);
      this.paginaSolicitada.set(1);
      this.rolAplicado.set(rol);
      this.estadoAplicado.set(estado);
      this.busquedaAplicada.set(busqueda);
      this.requiereActualizar.set(false);
      this.roles.set(roles);
      this.seleccionado.set(null);
      this.cargado.set(true);
    });
  }
  async filtrar() {
    await this.ejecutar(async () => {
      const rol = this.rol,
        estado = this.estado,
        busqueda = this.busqueda().trim();
      const usuarios = await this.consultarListado(rol, estado, busqueda);
      this.usuarios.set(usuarios);
      this.paginaSolicitada.set(1);
      this.rolAplicado.set(rol);
      this.estadoAplicado.set(estado);
      this.busquedaAplicada.set(busqueda);
      this.requiereActualizar.set(false);
      this.seleccionado.set(null);
    });
  }
  async consultar(usuario: UsuarioAdministrado, evento: Event) {
    if (this.ocupado()) return;
    this.origenDetalle = evento.currentTarget as HTMLElement;
    await this.ejecutar(async () => {
      const detalle = this.soloEstudiantes() ? usuario : await this.api.consultarUsuario(usuario.id);
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
    if (this.ocupado() || (this.soloEstudiantes() && (tipo === 'rol' || tipo === 'estado'))) return;
    if (evento && !this.vistaModal()) this.origenDetalle = evento.currentTarget as HTMLElement;
    if (usuario) this.seleccionado.set(usuario);
    if (tipo === 'registro') {
      this.tipoRegistro = this.soloEstudiantes() ? 'ESTUDIANTE' : 'DOCENTE';
      this.nuevoPerfil = { nombre: '', correo: '', clave: '' };
      this.mostrarClave = false;
    }
    if (tipo === 'rol' && this.seleccionado()) this.nuevoRol = this.seleccionado()!.rol as Rol;
    this.confirmarCambio = false;
    this.confirmarPermiso.set(false);
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
    this.nuevoPerfil = { nombre: '', correo: '', clave: '' };
    this.mostrarClave = false;
    this.confirmarCambio = false;
    this.confirmarPermiso.set(false);
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
    ).filter((elemento) => !elemento.closest('dialog:not([open])'));
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
  puedeEditar(usuario: UsuarioAdministrado) {
    if (this.soloEstudiantes()) return usuario.rol === 'ESTUDIANTE';
    return usuario.rol !== 'ADMINISTRADOR' || usuario.id === this.api.usuario()?.id;
  }

  estadoCuenta(activo: boolean | undefined) {
    return activo === true ? 'ACTIVO' : activo === false ? 'INACTIVO' : 'NO_DISPONIBLE';
  }

  editarDetalle(usuario: UsuarioAdministrado) {
    if (!this.puedeEditar(usuario)) return;
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
    if (!usuario || !this.puedeEditar(usuario)) return;
    await this.ejecutar(async () => {
      const datos = {
        nombre: this.nombre.trim(),
        correo: this.correo.trim(),
      };
      const actualizado = this.soloEstudiantes()
        ? await this.api.editarEstudiante(usuario.id, datos)
        : await this.api.editarUsuario(usuario.id, datos);
      this.actualizar(actualizado);
      await this.refrescarListado();
      this.editando = false;
      this.aviso.set(this.soloEstudiantes()
        ? 'Datos del estudiante actualizados.'
        : usuario.rol === 'DOCENTE' ? 'Datos del docente actualizados.' : 'Datos actualizados.');
      setTimeout(() => document.querySelector<HTMLElement>('#detalle-usuario')?.focus());
    });
  }

  private actualizar(usuario: UsuarioAdministrado) {
    this.usuarios.update((usuarios) =>
      usuarios.map((anterior) => (anterior.id === usuario.id ? usuario : anterior)),
    );
    if (this.seleccionado()?.id === usuario.id) this.seleccionado.set(usuario);
  }
  async cambiarEstado() {
    if (this.ocupado() || this.soloEstudiantes()) return;
    const usuario = this.seleccionado();
    if (!usuario || usuario.id === this.api.usuario()?.id) return;
    await this.ejecutar(async () => {
      const actualizado = await this.api.cambiarEstadoUsuario(usuario.id, !usuario.activo);
      this.actualizar(actualizado);
      await this.refrescarListado();
    });
    if (!this.error()) {
      this.cerrarDetalle();
      this.aviso.set(
        usuario.activo ? 'Cuenta desactivada. Su historial se conserva.' : 'Cuenta reactivada.',
      );
    }
  }
  async registrarPerfil() {
    if (this.ocupado()) return;
    const clave = this.nuevoPerfil.clave;
    if (clave.length < 8 || clave.length > 72 || new TextEncoder().encode(clave).length > 72) {
      this.error.set(
        'La contraseña debe tener al menos 8 caracteres y ocupar como máximo 72 bytes.',
      );
      return;
    }
    await this.ejecutar(async () => {
      const datos = {
        nombre: this.nuevoPerfil.nombre.trim(),
        correo: this.nuevoPerfil.correo.trim(),
        clave,
      };
      if (this.soloEstudiantes() || this.tipoRegistro === 'ESTUDIANTE') await this.api.registrarEstudiante(datos);
      else await this.api.registrarDocente(datos);
      await this.refrescarListado();
    });
    if (!this.error()) {
      this.cerrarDetalle();
      this.aviso.set(
        this.tipoRegistro === 'ESTUDIANTE'
          ? 'Estudiante registrado con contraseña inicial.'
          : 'Docente registrado. Puedes encontrar su cuenta con el filtro de docentes.',
      );
    }
  }
  async cambiarRol() {
    if (this.ocupado() || this.soloEstudiantes()) return;
    const usuario = this.seleccionado();
    if (
      !usuario ||
      usuario.id === this.api.usuario()?.id ||
      usuario.rol === this.nuevoRol ||
      !this.confirmarCambio
    )
      return;
    await this.ejecutar(async () => {
      const respuesta = await this.api.cambiarRolUsuario(usuario.id, this.nuevoRol);
      this.actualizar({ ...respuesta, activo: usuario.activo });
      this.requiereRecargarDetalle.set(false);
      try {
        this.actualizar(await this.api.consultarUsuario(usuario.id));
      } catch {
        this.requiereRecargarDetalle.set(true);
      }
      await this.refrescarListado();
    });
    if (!this.error()) {
      this.abrirVentana('detalle');
      this.aviso.set(
        this.requiereRecargarDetalle()
          ? 'Rol actualizado. No se pudo recargar el detalle; vuelve a consultar su estado.'
          : 'Rol actualizado. Los permisos de la cuenta corresponden a su nuevo rol.',
      );
    }
  }
  async recargarDetalle() {
    const usuario = this.seleccionado();
    if (!usuario) return;
    await this.ejecutar(async () => {
      this.actualizar(await this.api.consultarUsuario(usuario.id));
      this.requiereRecargarDetalle.set(false);
    });
  }
}
