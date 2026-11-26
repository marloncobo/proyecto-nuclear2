import { Component, computed, input, linkedSignal, model, output, TemplateRef } from '@angular/core';
import { DatePipe, NgTemplateOutlet } from '@angular/common';
import { FormsModule } from '@angular/forms';
import type { AvisoNotificacion } from './servicio-api';

@Component({
  selector: 'psico-panel-notificaciones',
  imports: [DatePipe, FormsModule, NgTemplateOutlet],
  templateUrl: './panel-notificaciones.html',
  styleUrl: './panel-notificaciones.css',
})
export class PanelNotificaciones {
  readonly notificaciones = input.required<readonly AvisoNotificacion[]>();
  readonly cargandoNotificaciones = input(false);
  readonly errorNotificaciones = input('');
  readonly marcandoNotificacion = input<string | null>(null);
  readonly ocupado = input(false);
  readonly totalNoLeidas = input<number | null>(null);
  readonly detalleAviso = input<TemplateRef<unknown> | null>(null);
  readonly idSeleccionada = input<string | null>(null);
  readonly estadoNotificaciones = model('');
  readonly tipoNotificaciones = model('');
  readonly marcarLeida = output<AvisoNotificacion>();
  readonly marcarTodasLeidas = output<void>();
  readonly seleccionarAviso = output<{ aviso: AvisoNotificacion; control: HTMLButtonElement }>();
  readonly cambiarListado = output<void>();
  private readonly avisosPorPagina = 5;
  readonly notificacionesNoLeidas = computed(
    () => this.totalNoLeidas() ?? this.notificaciones().filter((aviso) => !aviso.leida).length,
  );
  readonly tiposNotificaciones = computed(() => [
    ...new Set(this.notificaciones().map((notificacion) => notificacion.tipo)),
  ]);
  readonly notificacionesVisibles = computed(() =>
    this.notificaciones().filter(
      (notificacion) =>
        (!this.estadoNotificaciones() ||
          (this.estadoNotificaciones() === 'NO_LEIDA'
            ? !notificacion.leida
            : notificacion.leida)) &&
        (!this.tipoNotificaciones() || notificacion.tipo === this.tipoNotificaciones()),
    ),
  );
  readonly totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.notificacionesVisibles().length / this.avisosPorPagina)),
  );
  readonly pagina = linkedSignal<{ estado: string; tipo: string; total: number }, number>({
    source: () => ({
      estado: this.estadoNotificaciones(),
      tipo: this.tipoNotificaciones(),
      total: this.totalPaginas(),
    }),
    computation: ({ estado, tipo, total }, anterior) =>
      anterior && estado === anterior.source.estado && tipo === anterior.source.tipo
        ? Math.min(anterior.value, total)
        : 1,
  });
  readonly avisosPagina = computed(() => {
    const inicio = (this.pagina() - 1) * this.avisosPorPagina;
    return this.notificacionesVisibles().slice(inicio, inicio + this.avisosPorPagina);
  });
  cambiarPagina(pagina: number) {
    this.cambiarListado.emit();
    this.pagina.set(pagina);
  }
  limpiarFiltrosNotificaciones() {
    this.estadoNotificaciones.set('');
    this.tipoNotificaciones.set('');
    this.cambiarListado.emit();
  }
  tituloNotificacion(tipo: string) {
    return (
      (
        {
          CONVOCATORIA: 'Nueva convocatoria',
          RESULTADO: 'Calificación disponible',
          VALORACION: 'Nueva valoración docente',
          RECORDATORIO_SESION: 'Recordatorio de sesión próxima',
          FINALIZACION_SESION: 'Finalización de sesión',
          ESTUDIANTES_PENDIENTES: 'Estudiantes que no completaron la actividad',
          EVALUACIONES_DISPONIBLES: 'Evaluaciones disponibles',
          REPORTE_DISPONIBLE: 'Reporte disponible',
        } as Record<string, string>
      )[tipo] ?? tipo
    );
  }

  iconoNotificacion(tipo: string) {
    return (
      (
        {
          CONVOCATORIA: '◷',
          RESULTADO: '▣',
          VALORACION: '✎',
          RECORDATORIO_SESION: '◷',
          FINALIZACION_SESION: '✓',
          ESTUDIANTES_PENDIENTES: '♙',
          EVALUACIONES_DISPONIBLES: '▣',
          REPORTE_DISPONIBLE: '▤',
        } as Record<string, string>
      )[tipo] ?? '◉'
    );
  }
}
