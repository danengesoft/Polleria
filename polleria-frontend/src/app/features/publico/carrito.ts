import { Component, inject } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { CarritoService } from './carrito-service';
@Component({
  selector: 'app-confirmar-vaciado',
  imports: [MatDialogModule, MatButtonModule],
  template: `<h2 mat-dialog-title>¿Vaciar el carrito?</h2>
    <mat-dialog-content>Se quitarán los productos de tu selección.</mat-dialog-content
    ><mat-dialog-actions align="end"
      ><button matButton [mat-dialog-close]="false" cdkFocusInitial>Conservar carrito</button
      ><button matButton="filled" [mat-dialog-close]="true">
        Vaciar carrito
      </button></mat-dialog-actions
    >`,
})
export class ConfirmarVaciado {}
@Component({
  selector: 'app-carrito',
  imports: [CurrencyPipe, RouterLink],
  styleUrl: './publico.css',
  template: ` <h1>Tu carrito</h1>
    <p>El total es estimado. La disponibilidad y el precio se confirman al crear el pedido.</p>
    @for (l of carrito.lineas(); track l.productoId) {
      <article class="linea cart-line">
        <div>
          <h2>{{ l.nombre }}</h2>
          <p>{{ l.precio | currency: 'PEN' : 'S/ ' : '1.2-2' }} por unidad</p>
        </div>
        <div class="cantidad">
          <button
            [disabled]="l.cantidad === 1"
            (click)="carrito.cantidad(l.productoId, l.cantidad - 1)"
            [attr.aria-label]="'Disminuir ' + l.nombre"
          >
            −
          </button>
          <label
            >Cantidad de {{ l.nombre
            }}<input
              type="number"
              min="1"
              max="999"
              step="1"
              [value]="l.cantidad"
              (change)="carrito.cantidad(l.productoId, +$any($event.target).value)"
          /></label>
          <button
            [disabled]="l.cantidad === 999"
            (click)="carrito.cantidad(l.productoId, l.cantidad + 1)"
            [attr.aria-label]="'Aumentar ' + l.nombre"
          >
            +
          </button>
        </div>
        <strong>{{ l.precio * l.cantidad | currency: 'PEN' : 'S/ ' : '1.2-2' }}</strong
        ><button class="quiet" (click)="carrito.eliminar(l.productoId)">
          Quitar {{ l.nombre }}
        </button>
      </article>
    } @empty {
      <div class="empty-state">
        <p>Tu carrito está vacío.</p>
        <a class="action" routerLink="/catalogo">Ver catálogo</a>
      </div>
    }
    @if (carrito.lineas().length) {
      <p class="total">Total estimado {{ carrito.total() | currency: 'PEN' : 'S/ ' : '1.2-2' }}</p>
      <div class="acciones">
        <button class="secondary" (click)="confirmarVaciado()">Vaciar carrito</button
        ><a class="action" routerLink="/checkout">Continuar al checkout</a>
      </div>
    }
    <a routerLink="/catalogo">Seguir eligiendo</a>`,
})
export class Carrito {
  readonly carrito = inject(CarritoService);
  private readonly dialog = inject(MatDialog);
  confirmarVaciado() {
    this.dialog
      .open(ConfirmarVaciado, { width: '420px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) this.carrito.vaciar();
      });
  }
}
