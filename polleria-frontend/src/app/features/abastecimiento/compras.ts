import { etiquetaUnidad } from './abastecimiento.model';
import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { CompraService } from './compra-service';
import { InsumoService } from './insumo-service';
import { ProveedorService } from './proveedor-service';
import { InventarioService } from './inventario-service';
import {
  Compra,
  Insumo,
  Proveedor,
  Movimiento,
  CompraRequest,
  CANTIDAD,
  COSTO,
} from './abastecimiento.model';
import { AbastecimientoNav } from './abastecimiento-nav';
import { mensajeHttp } from '../../shared/http-error';
@Component({
  selector: 'app-compras',
  imports: [CurrencyPipe, DatePipe, DecimalPipe, ReactiveFormsModule, AbastecimientoNav],
  templateUrl: './compras.html',
  styleUrl: './abastecimiento.css',
})
export class Compras {
  readonly unidad = etiquetaUnidad;
  private readonly api = inject(CompraService);
  private readonly insumosApi = inject(InsumoService);
  private readonly proveedoresApi = inject(ProveedorService);
  private readonly inventarioApi = inject(InventarioService);
  private readonly fb = inject(FormBuilder);
  readonly compras = signal<Compra[]>([]);
  readonly insumos = signal<Insumo[]>([]);
  readonly proveedores = signal<Proveedor[]>([]);
  readonly seleccionada = signal<Compra | null>(null);
  readonly entradas = signal<Movimiento[]>([]);
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly aviso = signal('');
  readonly form = this.fb.nonNullable.group(
    { proveedorId: [0, Validators.min(1)], detalles: this.fb.array([this.linea()]) },
    {
      validators: (c: AbstractControl) => {
        const values = (c.get('detalles')?.value as { insumoId: number }[]) ?? [];
        const ids = values.map((l) => Number(l.insumoId)).filter((id) => id > 0);
        return !values.length
          ? { sinDetalles: true }
          : new Set(ids).size !== ids.length
            ? { duplicados: true }
            : null;
      },
    },
  );
  constructor() {
    this.cargar();
  }
  linea() {
    return this.fb.nonNullable.group({
      insumoId: [0, Validators.min(1)],
      cantidad: [
        '1.000',
        [Validators.required, Validators.pattern(CANTIDAD), Validators.min(0.001)],
      ],
      costoUnitario: ['', [Validators.required, Validators.pattern(COSTO), Validators.min(0.0001)]],
    });
  }
  agregar() {
    this.form.controls.detalles.push(this.linea());
  }
  quitar(i: number) {
    this.form.controls.detalles.removeAt(i);
  }
  cargar() {
    this.ocupado.set(true);
    this.error.set('');
    forkJoin({
      compras: this.api.listar(),
      insumos: this.insumosApi.listar(),
      proveedores: this.proveedoresApi.listar(),
    }).subscribe({
      next: (r) => {
        this.compras.set(r.compras);
        this.insumos.set(r.insumos);
        this.proveedores.set(r.proveedores.filter((p) => p.activo));
        this.ocupado.set(false);
      },
      error: (e) => {
        this.error.set(mensajeHttp(e));
        this.ocupado.set(false);
      },
    });
  }
  activos() {
    return this.insumos().filter((i) => i.activo);
  }
  stock(id: number) {
    return this.insumos().find((i) => i.id === Number(id));
  }
  subtotal(i: number) {
    const l = this.form.controls.detalles.at(i).getRawValue();
    return Math.round(Number(l.cantidad) * Number(l.costoUnitario) * 100) / 100;
  }
  total() {
    return this.form.controls.detalles.controls.reduce((n, _, i) => n + this.subtotal(i), 0);
  }
  guardar() {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.ocupado()) return;
    this.ocupado.set(true);
    this.error.set('');
    this.aviso.set('');
    const v = this.form.getRawValue();
    const r: CompraRequest = {
      proveedorId: Number(v.proveedorId),
      detalles: v.detalles.map((l) => ({
        insumoId: Number(l.insumoId),
        cantidad: l.cantidad,
        costoUnitario: l.costoUnitario,
      })),
    };
    this.api.crear(r).subscribe({
      next: (c) => {
        this.seleccionada.set(c);
        this.form.controls.detalles.clear();
        this.agregar();
        this.form.controls.proveedorId.setValue(0);
        this.form.markAsUntouched();
        this.aviso.set('Compra registrada. Total, stock y entradas confirmados por el servidor.');
        this.inventarioApi.listar().subscribe({
          next: (m) => this.entradas.set(m.filter((x) => x.compraId === c.id)),
          error: (e) => this.error.set(mensajeHttp(e)),
        });
        this.cargar();
      },
      error: (e) => {
        this.error.set(mensajeHttp(e));
        this.ocupado.set(false);
      },
    });
  }
  abrir(id: number) {
    this.ocupado.set(true);
    this.error.set('');
    this.api.obtener(id).subscribe({
      next: (c) => {
        this.seleccionada.set(c);
        this.entradas.set([]);
        this.ocupado.set(false);
      },
      error: (e) => {
        this.error.set(mensajeHttp(e));
        this.ocupado.set(false);
      },
    });
  }
}
