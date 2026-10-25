import { RouterLink } from '@angular/router';
import { Component } from '@angular/core';
@Component({
  selector: 'app-inicio-publico',
  imports: [RouterLink],
  template: ` <section class="welcome">
    <div>
      <p class="eyebrow">Pollería · Catálogo en línea</p>
      <h1>Bienvenido a Pollería</h1>
      <p class="lead">Elige tus favoritos y prepara tu pedido.</p>
      <p>
        Consulta el catálogo, revisa tu carrito y elige recoger tu pedido o recibirlo por delivery.
      </p>
      <a class="action" routerLink="/catalogo">Ver catálogo <span aria-hidden="true">→</span></a>
    </div>
    <aside aria-label="Cómo hacer tu pedido">
      <p class="eyebrow">A tu ritmo</p>
      <ol>
        <li>
          <strong>Explora el catálogo</strong><span>Productos, precios y disponibilidad.</span>
        </li>
        <li>
          <strong>Prepara tu carrito</strong><span>Elige cantidades y revisa tu selección.</span>
        </li>
        <li><strong>Confirma tu pedido</strong><span>Inicia sesión y consulta su estado.</span></li>
      </ol>
    </aside>
  </section>`,
  styles: `
    .welcome {
      display: grid;
      grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr);
      gap: 4rem;
      align-items: center;
      padding-block: 2rem 4rem;
    }
    .welcome h1 {
      font-size: clamp(2.2rem, 4vw, 3.2rem);
      max-width: 12ch;
    }
    .welcome p {
      max-width: 40rem;
    }
    .action {
      margin-top: 1rem;
    }
    aside {
      border-left: 1px solid var(--line);
      padding: 1rem 0 1rem 2.5rem;
    }
    ol {
      padding: 0;
      list-style: none;
      counter-reset: pasos;
      margin: 0;
    }
    li {
      counter-increment: pasos;
      padding: 1.15rem 0;
      border-bottom: 1px solid var(--line);
      display: grid;
      grid-template-columns: 2rem 1fr;
      gap: 0.25rem 0.75rem;
    }
    li::before {
      content: '0' counter(pasos);
      grid-row: span 2;
      color: var(--accent);
      font-size: 0.8rem;
      font-weight: 700;
    }
    li strong {
      font-size: 1rem;
    }
    li span {
      font-size: 0.85rem;
      color: var(--muted);
    }
    @media (max-width: 768px) {
      .welcome {
        grid-template-columns: 1fr;
        gap: 2rem;
        padding-block: 0 1rem;
      }
      aside {
        padding: 1.25rem 0 0;
        border-left: 0;
        border-top: 1px solid var(--line);
      }
    }
  `,
})
export class InicioPublico {}
