import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-navigation',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav aria-label="Navegación principal">
      <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}" ariaCurrentWhenActive="page">Inicio</a>
      <a routerLink="/interno" routerLinkActive="active" ariaCurrentWhenActive="page">Portal interno</a>
    </nav>
  `,
  styles: `
    nav { display:flex; flex-wrap:wrap; gap:.5rem; }
    a { padding:.6rem .85rem; border-radius:.35rem; text-decoration:none; color:var(--muted); }
    a:hover, a.active { color:var(--ink); background:var(--surface-soft); }
    a.active { font-weight:600; }
  `,
})
export class AppNavigation {}
