import { CarritoService } from '../../features/publico/carrito-service';
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/auth/auth-service';
@Component({
  selector: 'app-navigation',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav aria-label="Navegación principal">
      <a
        routerLink="/"
        routerLinkActive="active"
        [routerLinkActiveOptions]="{ exact: true }"
        ariaCurrentWhenActive="page"
        >Inicio</a
      >
      <a routerLink="/catalogo" routerLinkActive="active">Catálogo</a>
      <a routerLink="/carrito" routerLinkActive="active">Carrito ({{ carrito.unidades() }})</a>
      @if (auth.tieneRol('CLIENTE')) {
        <a routerLink="/mis-pedidos" routerLinkActive="active">Mis pedidos</a
        ><a routerLink="/perfil" routerLinkActive="active">Perfil</a>
      }
      @if (auth.usuario()) {
        @if (auth.tieneRol('ADMINISTRADOR', 'CAJERO', 'REPARTIDOR', 'ABASTECIMIENTO')) {
          <a routerLink="/interno" routerLinkActive="active">Portal interno</a>
        }
        <span class="identity"
          >{{ auth.usuario()?.nombre }}<small>{{ auth.roles().join(' · ') }}</small></span
        >
        <button type="button" (click)="auth.cerrarSesion()">Cerrar sesión</button>
      } @else {
        <button type="button" (click)="auth.iniciarLogin('/')">Iniciar sesión</button>
      }
    </nav>
    @if (auth.error()) {
      <p role="alert">{{ auth.error() }}</p>
    }
  `,
  styles: `
    nav {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 0.5rem;
    }
    a,
    button {
      padding: 0.6rem 0.85rem;
      border-radius: 0.35rem;
      text-decoration: none;
      color: var(--muted);
    }
    button {
      margin: 0;
      background: transparent;
      border: 1px solid var(--line);
      cursor: pointer;
      font: inherit;
    }
    a:hover,
    a.active {
      color: var(--ink);
      background: var(--surface-soft);
    }
    .identity {
      font-size: 0.875rem;
      max-width: 16rem;
      overflow-wrap: anywhere;
    }
    small {
      display: block;
      font-size: 0.7rem;
      color: var(--muted);
    }
  `,
})
export class AppNavigation {
  readonly auth = inject(AuthService);
  readonly carrito = inject(CarritoService);
}
