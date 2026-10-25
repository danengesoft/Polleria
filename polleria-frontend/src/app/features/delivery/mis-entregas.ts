import { EstadoBadge } from '../../shared/estado-badge';
import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { DeliveryService } from './delivery-service';
import { Entrega, Repartidor } from './delivery.model';
import { ConfirmarEntrega } from './confirmar-entrega';
import { etiqueta } from '../pedidos/pedido.model';
import { mensajeHttp } from '../../shared/http-error';
@Component({
  selector: 'app-mis-entregas',
  imports: [EstadoBadge, CurrencyPipe],
  templateUrl: './mis-entregas.html',
  styleUrl: './delivery.css',
})
export class MisEntregas {
  private readonly api = inject(DeliveryService);
  private readonly dialog = inject(MatDialog);
  readonly perfil = signal<Repartidor | null>(null);
  readonly faltaPerfil = signal(false);
  readonly entregas = signal<Entrega[]>([]);
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly etiqueta = etiqueta;
  constructor() {
    this.cargar();
  }
  cargar() {
    this.ocupado.set(true);
    this.error.set('');
    this.api.perfil().subscribe({
      next: (r) => {
        this.perfil.set(r);
        this.faltaPerfil.set(false);
        if (!r.activo) {
          this.error.set('Tu perfil de reparto está inactivo.');
          this.ocupado.set(false);
          return;
        }
        this.cargarEntregas();
      },
      error: (e) => {
        this.ocupado.set(false);
        if (e instanceof HttpErrorResponse && e.status === 404) this.faltaPerfil.set(true);
        else this.error.set(mensajeHttp(e));
      },
    });
  }
  activar() {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.api.activar().subscribe({
      next: () => this.cargar(),
      error: (e) => {
        this.error.set(mensajeHttp(e));
        this.ocupado.set(false);
      },
    });
  }
  private cargarEntregas() {
    this.error.set('');
    this.api.mias().subscribe({
      next: (r) => {
        this.entregas.set(r);
        this.ocupado.set(false);
      },
      error: (e) => {
        this.error.set(mensajeHttp(e));
        this.ocupado.set(false);
      },
    });
  }
  iniciar(e: Entrega) {
    if (e.estado !== 'ASIGNADO' || this.ocupado()) return;
    this.ocupado.set(true);
    this.api.iniciar(e.id).subscribe({
      next: () => this.cargarEntregas(),
      error: (err) => {
        this.error.set(mensajeHttp(err));
        this.ocupado.set(false);
      },
    });
  }
  finalizar(e: Entrega, incidencia: boolean) {
    if (e.estado !== 'EN_CAMINO' || this.ocupado()) return;
    this.ocupado.set(true);
    this.dialog
      .open(ConfirmarEntrega, { data: incidencia, width: '28rem', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((result) => {
        if (!result) {
          this.ocupado.set(false);
          return;
        }
        const op = incidencia ? this.api.noEntregado(e.id, result) : this.api.entregar(e.id);
        op.subscribe({
          next: () => this.cargarEntregas(),
          error: (err) => {
            this.error.set(mensajeHttp(err));
            this.ocupado.set(false);
          },
        });
      });
  }
}
