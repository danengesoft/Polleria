import { etiquetaUnidad } from './abastecimiento.model';
import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { InventarioService } from './inventario-service';
import { InsumoService } from './insumo-service';
import { Insumo, Movimiento, TipoMovimiento, CANTIDAD } from './abastecimiento.model';
import { AbastecimientoNav } from './abastecimiento-nav';
import { mensajeHttp } from '../../shared/http-error';
@Component({
  selector: 'app-inventario',
  imports: [DatePipe, DecimalPipe, ReactiveFormsModule, AbastecimientoNav],
  templateUrl: './inventario.html',
  styleUrl: './abastecimiento.css',
})
export class Inventario {
  readonly unidad = etiquetaUnidad;
  private readonly api = inject(InventarioService);
  private readonly insumosApi = inject(InsumoService);
  private readonly fb = inject(FormBuilder);
  readonly movimientos = signal<Movimiento[]>([]);
  readonly insumos = signal<Insumo[]>([]);
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly aviso = signal('');
  readonly filtros = this.fb.nonNullable.group({
    insumoId: [0],
    tipo: ['' as '' | TipoMovimiento],
  });
  readonly form = this.fb.nonNullable.group({
    operacion: ['SALIDA' as 'SALIDA' | 'AJUSTE'],
    insumoId: [0, Validators.min(1)],
    cantidad: ['', [Validators.required, Validators.pattern(CANTIDAD), Validators.min(0)]],
    motivo: ['', [Validators.required, Validators.maxLength(500), Validators.pattern(/\S/)]],
  });
  constructor() {
    this.cargar();
  }
  cargar() {
    this.ocupado.set(true);
    this.error.set('');
    this.insumosApi.listar().subscribe({
      next: (i) => {
        this.insumos.set(i);
        this.cargarMovimientos();
      },
      error: (e) => {
        this.error.set(mensajeHttp(e));
        this.ocupado.set(false);
      },
    });
  }
  cargarMovimientos() {
    const f = this.filtros.getRawValue();
    this.api
      .listar({
        ...(f.insumoId ? { insumoId: Number(f.insumoId) } : {}),
        ...(f.tipo ? { tipo: f.tipo } : {}),
      })
      .subscribe({
        next: (m) => {
          this.movimientos.set(m);
          this.ocupado.set(false);
        },
        error: (e) => {
          this.error.set(mensajeHttp(e));
          this.ocupado.set(false);
        },
      });
  }
  stock() {
    return this.insumos().find((i) => i.id === Number(this.form.controls.insumoId.value));
  }
  guardar() {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.ocupado()) return;
    const v = this.form.getRawValue();
    if (v.operacion === 'SALIDA' && Number(v.cantidad) <= 0) {
      this.error.set('La salida requiere una cantidad mayor que cero.');
      return;
    }
    this.ocupado.set(true);
    this.error.set('');
    this.aviso.set('');
    const op =
      v.operacion === 'SALIDA'
        ? this.api.salida(Number(v.insumoId), v.cantidad, v.motivo.trim())
        : this.api.ajuste(Number(v.insumoId), v.cantidad, v.motivo.trim());
    op.subscribe({
      next: (m) => {
        this.aviso.set('Movimiento registrado. Stock confirmado: ' + m.stockPosterior.toFixed(3));
        this.form.controls.cantidad.setValue('');
        this.form.controls.motivo.setValue('');
        this.form.markAsUntouched();
        this.cargar();
      },
      error: (e) => {
        this.error.set(mensajeHttp(e));
        this.ocupado.set(false);
      },
    });
  }
}
