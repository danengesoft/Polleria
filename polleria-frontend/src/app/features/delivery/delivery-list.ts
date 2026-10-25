import { EstadoBadge } from '../../shared/estado-badge';
import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { DeliveryService } from './delivery-service';
import { Entrega, Repartidor } from './delivery.model';
import { PedidoService } from '../pedidos/pedido-service';
import { PedidoResponse, etiqueta } from '../pedidos/pedido.model';
import { mensajeHttp } from '../../shared/http-error';
@Component({
  selector: 'app-delivery-list',
  imports: [EstadoBadge, CurrencyPipe, DatePipe, ReactiveFormsModule, RouterLink],
  templateUrl: './delivery-list.html',
  styleUrl: './delivery.css',
})
export class DeliveryList {
  private readonly api = inject(DeliveryService);
  private readonly pedidosApi = inject(PedidoService);
  private readonly fb = inject(FormBuilder);
  readonly entregas = signal<Entrega[]>([]);
  readonly repartidores = signal<Repartidor[]>([]);
  readonly candidatos = signal<PedidoResponse[]>([]);
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly aviso = signal('');
  readonly etiqueta = etiqueta;
  readonly seleccion: Record<number, number> = {};
  readonly form = this.fb.nonNullable.group({
    pedidoId: [0, [Validators.required, Validators.min(1)]],
    repartidorId: [0, [Validators.required, Validators.min(1)]],
  });
  constructor() {
    this.cargar();
  }
  cargar() {
    this.ocupado.set(true);
    this.error.set('');
    forkJoin({
      entregas: this.api.entregas(),
      repartidores: this.api.repartidores(),
      pedidos: this.pedidosApi.listar({ tipoPedido: 'DELIVERY', estado: 'LISTO' }),
    }).subscribe({
      next: (r) => {
        this.entregas.set(r.entregas);
        this.repartidores.set(r.repartidores);
        const asignados = new Set(r.entregas.map((e) => e.pedidoId));
        this.candidatos.set(r.pedidos.filter((p) => !asignados.has(p.id)));
        this.ocupado.set(false);
      },
      error: (e) => {
        this.error.set(mensajeHttp(e));
        this.ocupado.set(false);
      },
    });
  }
  asignar() {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.ocupado()) return;
    this.ocupado.set(true);
    const r = this.form.getRawValue();
    this.api.asignar(Number(r.pedidoId), Number(r.repartidorId)).subscribe({
      next: () => {
        this.aviso.set('Entrega asignada.');
        this.form.reset();
        this.cargar();
      },
      error: (e) => {
        this.error.set(mensajeHttp(e));
        this.ocupado.set(false);
      },
    });
  }
  reasignar(e: Entrega) {
    const id = this.seleccion[e.id];
    if (!id || this.ocupado() || e.estado !== 'NO_ENTREGADO') return;
    this.ocupado.set(true);
    this.api.reasignar(e.id, Number(id)).subscribe({
      next: () => {
        this.aviso.set('Entrega reasignada; se conserva la incidencia anterior.');
        this.cargar();
      },
      error: (e) => {
        this.error.set(mensajeHttp(e));
        this.ocupado.set(false);
      },
    });
  }
}
