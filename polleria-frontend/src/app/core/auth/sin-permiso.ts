import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppNavigation } from '../../shared/navigation/app-navigation';
@Component({
  selector: 'app-sin-permiso',
  imports: [RouterLink, AppNavigation],
  template: `<main class="container auth-page">
    <app-navigation />
    <h1>Acceso denegado</h1>
    <p>No tienes permiso para acceder a esta pantalla.</p>
    <a routerLink="/">Volver al inicio</a>
  </main>`,
  styles: '.auth-page { padding-block:2rem; } h1 { margin-top:2rem; }',
})
export class SinPermiso {}
