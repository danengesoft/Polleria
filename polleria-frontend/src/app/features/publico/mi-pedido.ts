import { SeguimientoPedido } from '../../shared/seguimiento-pedido';
import { EstadoBadge } from '../../shared/estado-badge';
import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
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
  imports: [SeguimientoPedido, EstadoBadge, CurrencyPipe, DatePipe, RouterLink],
  styleUrl: './publico.css',
  template: ` <h1>Detalle de mi pedido</h1>
    @if (error()) {
      <p role="alert">{{ error() }}</p>
    }
    @if (pedido(); as p) {
      @if (recienCreado) {
        <section class="confirmation" role="status">
          <h2>Pedido creado</h2>
          <p>Tu pedido se registró correctamente. Aquí puedes consultar su estado.</p>
        </section>
      }
      <article class="public-detail">
        <h2>Pedido #{{ p.id }}</h2>
        <p>{{ p.fecha | date: 'dd/MM/yyyy HH:mm' }}</p>
        <p class="order-badges">Estado: <app-estado [valor]="p.estado" /></p>
        <app-seguimiento-pedido [estado]="p.estado" [tipo]="p.tipoPedido" />
        <p>
          {{ etiqueta(p.tipoPedido) }} · {{ etiqueta(p.metodoPago) }} · Pago
          <app-estado [valor]="p.estadoPago" />
        </p>
        @if (p.tipoPedido === 'DELIVERY') {
          <p>Dirección: {{ p.direccionEntrega }}</p>
          <p>Referencia: {{ p.referenciaEntrega }}</p>
          <p>
            El seguimiento muestra el estado del pedido. Actualiza esta pantalla para consultar
            cambios; no hay ubicación en vivo.
          </p>
        }
        @for (l of p.detalles; track l.productoId) {
          <div class="linea">
            <h3>{{ l.nombreProducto }}</h3>
            <p>{{ l.cantidad }} × {{ l.precioUnitario | currency: 'PEN' : 'S/ ' : '1.2-2' }}</p>
            <strong>{{ l.subtotal | currency: 'PEN' : 'S/ ' : '1.2-2' }}</strong>
          </div>
        }
        <p class="total">Total confirmado {{ p.total | currency: 'PEN' : 'S/ ' : '1.2-2' }}</p>
        <div class="acciones">
          @if (p.estado === 'PENDIENTE' && p.estadoPago !== 'PAGADO') {
            <button class="destructive" (click)="cancelar()" [disabled]="ocupado()">
              Cancelar pedido
            </button>
          }
          <button class="secondary" (click)="cargar()" [disabled]="ocupado()">
            Actualizar estado
          </button>
        </div>
      </article>
    }
    <a routerLink="/mis-pedidos">Volver a mis pedidos</a>`,
})
export class MiPedido {
  readonly recienCreado = Boolean(
    inject(Router).currentNavigation()?.extras.state?.['pedidoCreado'],
  );
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
