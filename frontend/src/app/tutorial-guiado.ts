import { Component, ElementRef, HostListener, effect, input, output, signal, viewChild } from '@angular/core';

export interface PasoTutorial {
  objetivo: string;
  titulo: string;
  descripcion: string;
}

const PREFERENCIA_TUTORIAL = 'psicoapp.tutorial.oculto';

@Component({
  selector: 'psico-tutorial',
  templateUrl: './tutorial-guiado.html',
  styleUrl: './tutorial-guiado.css',
})
export class TutorialGuiado {
  readonly pasos = input.required<readonly PasoTutorial[]>();
  readonly automatico = input(false);
  readonly inicioConsumido = output<void>();
  readonly oculto = signal(this.leerPreferencia());
  readonly pasosActivos = signal<readonly PasoTutorial[]>([]);
  readonly indice = signal(0);
  readonly marco = signal<DOMRect | null>(null);
  readonly ventana = viewChild.required<ElementRef<HTMLDialogElement>>('ventana');
  readonly disparador = viewChild.required<ElementRef<HTMLButtonElement>>('disparador');
  private origen: HTMLElement | null = null;

  constructor() {
    effect((limpiar) => {
      if (!this.automatico()) return;
      const cuadro = requestAnimationFrame(() => {
        this.inicioConsumido.emit();
        if (!this.leerPreferencia()) this.abrir();
      });
      limpiar(() => cancelAnimationFrame(cuadro));
    });
  }

  private leerPreferencia() {
    try {
      return localStorage.getItem(PREFERENCIA_TUTORIAL) === '1';
    } catch {
      return false;
    }
  }

  guardarPreferencia(ocultar: boolean) {
    this.oculto.set(ocultar);
    try {
      if (ocultar) localStorage.setItem(PREFERENCIA_TUTORIAL, '1');
      else localStorage.removeItem(PREFERENCIA_TUTORIAL);
    } catch {
      // La preferencia se mantiene mientras el componente siga abierto.
    }
  }

  abrir() {
    const ventana = this.ventana().nativeElement;
    if (ventana.open || !this.pasos().length) return;
    this.oculto.set(this.leerPreferencia());
    this.origen = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const disponibles = this.pasos().filter((paso) => this.buscarObjetivo(paso));
    this.pasosActivos.set(disponibles.length ? disponibles : this.pasos().slice(0, 1));
    this.indice.set(0);
    ventana.showModal();
    this.enfocarPaso();
    requestAnimationFrame(() => {
      if (!ventana.open) return;
      if (document.activeElement === ventana)
        ventana.querySelector<HTMLElement>('.siguiente')?.focus({ preventScroll: true });
      this.medir();
    });
  }

  avanzar() {
    if (this.indice() + 1 === this.pasosActivos().length) {
      this.cerrar();
      return;
    }
    this.indice.update((indice) => indice + 1);
    this.enfocarPaso();
  }

  cerrar() {
    this.ventana().nativeElement.close();
    const destino = this.origen?.isConnected && this.origen !== document.body
      ? this.origen
      : this.disparador().nativeElement;
    destino.focus({ preventScroll: true });
  }

  cancelar(evento: Event) {
    evento.preventDefault();
    evento.stopPropagation();
    this.cerrar();
  }

  private buscarObjetivo(paso: PasoTutorial) {
    return [...document.querySelectorAll<HTMLElement>(paso.objetivo)].find(
      (elemento) => elemento.getClientRects().length && getComputedStyle(elemento).visibility !== 'hidden',
    );
  }

  private enfocarPaso() {
    const paso = this.pasosActivos()[this.indice()];
    if (!paso) return;
    this.buscarObjetivo(paso)?.scrollIntoView({
      behavior: 'instant',
      block: 'nearest',
      inline: 'nearest',
    });
    this.medir();
  }

  @HostListener('window:resize')
  medir() {
    const paso = this.pasosActivos()[this.indice()];
    const rect = paso && this.buscarObjetivo(paso)?.getBoundingClientRect();
    this.marco.set(rect ?? null);
  }
}
