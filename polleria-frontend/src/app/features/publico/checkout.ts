import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ClientePublicoService, MiPedidoRequest } from './cliente-publico-service';
import { CarritoService } from './carrito-service';
import { METODOS, MetodoPago } from '../pedidos/pedido.model';
import { mensajeHttp } from '../../shared/http-error';
@Component({
  selector: 'app-checkout',
  imports: [CurrencyPipe, ReactiveFormsModule, RouterLink],
  styleUrl: './publico.css',
  templateUrl: './checkout.html',
})
export class Checkout {
  private readonly api = inject(ClientePublicoService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  readonly carrito = inject(CarritoService);
  readonly perfilListo = signal(false);
  readonly guardando = signal(false);
  readonly error = signal('');
  readonly metodos = METODOS;
  readonly form = this.fb.nonNullable.group({
    tipoPedido: ['RECOJO' as 'RECOJO' | 'DELIVERY', Validators.required],
    metodoPago: ['EFECTIVO' as MetodoPago, Validators.required],
    direccionEntrega: ['', Validators.maxLength(300)],
    referenciaEntrega: ['', Validators.maxLength(300)],
  });
  constructor() {
    this.cargarPerfil();
  }
  cargarPerfil() {
    this.error.set('');
    this.api.perfil().subscribe({
      next: (p) => {
        if (!p.activo) {
          this.error.set('Tu perfil comercial está desactivado.');
          return;
        }
        this.perfilListo.set(true);
        this.form.patchValue({
          direccionEntrega: p.direccionPrincipal ?? '',
          referenciaEntrega: p.referencia ?? '',
        });
      },
      error: (e) => {
        if (e instanceof HttpErrorResponse && e.status === 404)
          void this.router.navigate(['/perfil'], { queryParams: { continuar: 'checkout' } });
        else this.error.set(mensajeHttp(e));
      },
    });
  }
  confirmar() {
    this.form.markAllAsTouched();
    if (this.guardando() || !this.perfilListo() || !this.carrito.lineas().length) return;
    const v = this.form.getRawValue();
    if (v.tipoPedido === 'DELIVERY' && !v.direccionEntrega.trim()) {
      this.error.set('Delivery requiere una dirección de entrega.');
      return;
    }
    if (this.form.invalid) {
      this.error.set('Revisa los datos del pedido.');
      return;
    }
    const r: MiPedidoRequest = {
      tipoPedido: v.tipoPedido,
      metodoPago: v.metodoPago,
      direccionEntrega: v.tipoPedido === 'DELIVERY' ? v.direccionEntrega.trim() : null,
      referenciaEntrega: v.tipoPedido === 'DELIVERY' ? v.referenciaEntrega.trim() || null : null,
      detalles: this.carrito
        .lineas()
        .map((l) => ({ productoId: l.productoId, cantidad: l.cantidad })),
    };
    this.guardando.set(true);
    this.error.set('');
    this.api.crearPedido(r).subscribe({
      next: (p) => {
        this.carrito.vaciar();
        void this.router.navigate(['/mis-pedidos', p.id], { state: { pedidoCreado: true } });
      },
      error: (e) => {
        this.guardando.set(false);
        this.error.set(mensajeHttp(e));
      },
    });
  }
}
