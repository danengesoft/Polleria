import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { ClientePublicoService } from './cliente-publico-service';
import { PedidoResponse, etiqueta } from '../pedidos/pedido.model';
import { mensajeHttp } from '../../shared/http-error';
@Component({
  selector: 'app-confirmar-cancelacion',
  imports: [MatDialogModule, MatButtonModule],
  template: `<h2 mat-dialog-title>¿Cancelar tu pedido?</h2>
    <mat-dialog-content>Esta acción cancela el pedido pendiente.</mat-dialog-content
    ><mat-dialog-actions
      ><button matButton [mat-dialog-close]="false" cdkFocusInitial>Conservar pedido</button
      ><button matButton="filled" [mat-dialog-close]="true">
        Sí, cancelar pedido
      </button></mat-dialog-actions
    >`,
})
export class ConfirmarCancelacion {}
@Component({
  selector: 'app-mi-pedido',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  styleUrl: './publico.css',
  template: ` <h1>Detalle de mi pedido</h1>
    @if (error()) {
      <p role="alert">{{ error() }}</p>
    }
    @if (pedido(); as p) {
      <article>
        <h2>Pedido #{{ p.id }}</h2>
        <p>{{ p.fecha | date: 'dd/MM/yyyy HH:mm' }}</p>
        <p role="status">Estado: {{ etiqueta(p.estado) }}</p>
        <p>
          {{ etiqueta(p.tipoPedido) }} · {{ etiqueta(p.metodoPago) }} · Pago
          {{ etiqueta(p.estadoPago) }}
        </p>
        @if (p.tipoPedido === 'DELIVERY') {
          <p>Dirección: {{ p.direccionEntrega }}</p>
          <p>Referencia: {{ p.referenciaEntrega }}</p>
          <p>El seguimiento muestra el estado del pedido. Todavía no hay seguimiento de reparto.</p>
        }
        @for (l of p.detalles; track l.productoId) {
          <div class="linea">
            <h3>{{ l.nombreProducto }}</h3>
            <p>{{ l.cantidad }} × {{ l.precioUnitario | currency: 'PEN' : 'S/ ' : '1.2-2' }}</p>
            <strong>{{ l.subtotal | currency: 'PEN' : 'S/ ' : '1.2-2' }}</strong>
          </div>
        }
        <p class="total">Total confirmado {{ p.total | currency: 'PEN' : 'S/ ' : '1.2-2' }}</p>
        @if (p.estado === 'PENDIENTE' && p.estadoPago !== 'PAGADO') {
          <button (click)="cancelar()" [disabled]="ocupado()">Cancelar pedido</button>
        }
        <button (click)="cargar()" [disabled]="ocupado()">Actualizar estado</button>
      </article>
    }
    <a routerLink="/mis-pedidos">Volver a mis pedidos</a>`,
})
export class MiPedido {
  private readonly api = inject(ClientePublicoService);
  private readonly dialog = inject(MatDialog);
  private readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  readonly pedido = signal<PedidoResponse | null>(null);
  readonly error = signal('');
  readonly ocupado = signal(false);
  readonly etiqueta = etiqueta;
  constructor() {
    this.cargar();
  }
  cargar() {
    if (!Number.isSafeInteger(this.id) || this.id < 1) {
      this.error.set('Pedido no encontrado.');
      return;
    }
    this.ocupado.set(true);
    this.error.set('');
    this.api.pedido(this.id).subscribe({
      next: (p) => {
        this.pedido.set(p);
        this.ocupado.set(false);
      },
      error: (e) => {
        this.error.set(mensajeHttp(e));
        this.ocupado.set(false);
      },
    });
  }
  cancelar() {
    if (this.ocupado() || this.pedido()?.estado !== 'PENDIENTE') return;
    this.dialog
      .open(ConfirmarCancelacion, { width: '420px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.ocupado.set(true);
          this.api.cancelar(this.id).subscribe({
            next: (p) => {
              this.pedido.set(p);
              this.ocupado.set(false);
            },
            error: (e) => {
              this.error.set(mensajeHttp(e));
              this.ocupado.set(false);
            },
          });
        }
      });
  }
}
