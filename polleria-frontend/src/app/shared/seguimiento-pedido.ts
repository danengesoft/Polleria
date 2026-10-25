import { Component, computed, input } from '@angular/core';
import { EstadoPedido, TipoPedido, etiqueta } from '../features/pedidos/pedido.model';
import { EstadoBadge } from './estado-badge';
@Component({
  selector: 'app-seguimiento-pedido',
  imports: [EstadoBadge],
  template: `<section aria-label="Seguimiento del pedido">
    <h3>Estado de tu pedido</h3>
    @if (estado() === 'CANCELADO') {
      <app-estado valor="CANCELADO" />
    } @else {
      <ol>
        @for (paso of pasos(); track paso) {
          <li
            [class.actual]="paso === estado()"
            [class.completo]="pasos().indexOf(paso) < pasos().indexOf(estado())"
            [attr.aria-current]="paso === estado() ? 'step' : null"
          >
            {{ etiqueta(paso) }}
            @if (paso === estado()) {
              <span>Estado actual</span>
            }
          </li>
        }
      </ol>
    }
  </section>`,
  styles: `
    section {
      margin-block: 1.5rem;
      padding-block: 1rem;
      border-block: 1px solid var(--line);
    }
    ol {
      display: flex;
      flex-wrap: wrap;
      gap: 0.6rem;
      padding: 0;
      list-style: none;
      counter-reset: paso;
    }
    li {
      counter-increment: paso;
      padding: 0.65rem 0.8rem;
      border: 1px solid var(--line);
      border-radius: var(--radius);
      font-size: 0.8rem;
      color: var(--muted);
      background: var(--surface);
    }
    li::before {
      content: counter(paso) '. ';
      font-weight: 700;
    }
    li.actual {
      color: var(--accent);
      border-color: var(--accent);
      background: var(--accent-soft);
      font-weight: 700;
    }
    li.completo {
      color: var(--success);
    }
    li span {
      display: block;
      font-size: 0.7rem;
    }
    @media (max-width: 600px) {
      ol {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
    }
  `,
})
export class SeguimientoPedido {
  readonly estado = input.required<EstadoPedido>();
  readonly tipo = input.required<TipoPedido>();
  readonly etiqueta = etiqueta;
  readonly pasos = computed<EstadoPedido[]>(() => [
    'PENDIENTE',
    'CONFIRMADO',
    'EN_PREPARACION',
    'LISTO',
    ...(this.tipo() === 'DELIVERY' ? ['DESPACHADO' as const] : []),
    'ENTREGADO',
  ]);
}
