import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-inicio-interno',
  imports: [RouterLink],
  template: `
    <section class="intro">
      <p class="eyebrow">Inicio</p>
      <h1>Portal interno</h1>
      <p class="lead">El punto de partida para la gestión de la pollería.</p>
      <p>Las funciones de negocio se incorporarán en las siguientes etapas.</p>
      <a class="action" routerLink="/interno/conexion">Comprobar conexión</a>
      <p class="development-note">Entorno de desarrollo. Este portal aún no tiene autenticación.</p>
    </section>
  `,
})
export class InicioInterno {}
