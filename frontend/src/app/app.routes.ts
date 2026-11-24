import { Routes } from '@angular/router';

export const rutas: Routes = [
  { path: 'ingreso', children: [] },
  { path: 'estudiante', children: [] },
  { path: 'estudiante/actividades', children: [] },
  { path: 'estudiante/actividades/:idSesion', children: [] },
  { path: 'estudiante/resultados', children: [] },
  { path: 'estudiante/notificaciones', children: [] },
  { path: '', pathMatch: 'full', children: [] },
  { path: '**', redirectTo: 'ingreso' },
];
