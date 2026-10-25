import { RouterLink } from '@angular/router';
import { Component } from '@angular/core';

@Component({
  selector: 'app-inicio-publico',
  imports: [RouterLink],
  template: `
    <section class="intro">
      <p class="eyebrow">Área pública</p>
      <h1>Bienvenido a Pollería</h1>
      <p class="lead">Un espacio sencillo para acercarte a nuestra atención.</p>
      <p>Elige tus favoritos, prepara el carrito y pide para recoger o recibir a domicilio.</p>
      <a routerLink="/catalogo">Explorar catálogo</a>
    </section>
  `,
})
export class InicioPublico {}
