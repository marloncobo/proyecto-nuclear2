import { Routes } from '@angular/router';

export const rutas: Routes = [
  { path: 'ingreso', children: [] },
  { path: '', pathMatch: 'full', children: [] },
  { path: '**', redirectTo: 'ingreso' },
];
