import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'interno',
    loadComponent: () => import('./core/layout/interno/interno').then(m => m.Interno),
    children: [
      { path: '', pathMatch: 'full', title: 'Portal interno | Pollería', loadComponent: () => import('./core/inicio/inicio-interno').then(m => m.InicioInterno) },
      { path: 'conexion', title: 'Conexión | Pollería', loadComponent: () => import('./core/conexion/conexion').then(m => m.Conexion) },
    ],
  },
  {
    path: '',
    loadComponent: () => import('./core/layout/publico/publico').then(m => m.Publico),
    children: [
      { path: '', pathMatch: 'full', title: 'Inicio | Pollería', loadComponent: () => import('./core/inicio/inicio-publico').then(m => m.InicioPublico) },
    ],
  },
  { path: '**', redirectTo: '' },
];
