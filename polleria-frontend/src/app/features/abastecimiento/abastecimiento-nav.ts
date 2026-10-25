import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
@Component({
  selector: 'app-abastecimiento-nav',
  imports: [RouterLink, RouterLinkActive],
  template: `<nav aria-label="Abastecimiento">
    <a routerLink="/interno/abastecimiento/insumos" routerLinkActive="active">Insumos</a
    ><a routerLink="/interno/abastecimiento/proveedores" routerLinkActive="active">Proveedores</a
    ><a routerLink="/interno/abastecimiento/compras" routerLinkActive="active">Compras</a
    ><a routerLink="/interno/abastecimiento/inventario" routerLinkActive="active">Inventario</a>
  </nav>`,
  styles: `
    nav {
      display: flex;
      flex-wrap: wrap;
      gap: 0.6rem;
      margin-bottom: 1.5rem;
    }
    a {
      padding: 0.5rem;
      border-radius: 0.3rem;
      color: var(--ink);
    }
    a.active {
      background: var(--surface-soft);
    }
  `,
})
export class AbastecimientoNav {}
