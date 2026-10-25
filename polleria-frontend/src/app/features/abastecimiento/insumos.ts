import { etiquetaUnidad } from './abastecimiento.model';
import { Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { InsumoService } from './insumo-service';
import { Insumo, UnidadMedida, UNIDADES } from './abastecimiento.model';
import { AbastecimientoNav } from './abastecimiento-nav';
import { mensajeHttp } from '../../shared/http-error';
@Component({
  selector: 'app-insumos',
  imports: [DecimalPipe, ReactiveFormsModule, AbastecimientoNav],
  templateUrl: './insumos.html',
  styleUrl: './abastecimiento.css',
})
export class Insumos {
  readonly unidad = etiquetaUnidad;
  private readonly api = inject(InsumoService);
  private readonly fb = inject(FormBuilder);
  readonly items = signal<Insumo[]>([]);
  readonly editando = signal<number | null>(null);
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly aviso = signal('');
  readonly unidades = UNIDADES;
  readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    descripcion: ['', Validators.maxLength(500)],
    unidadMedida: ['UNIDAD' as UnidadMedida, Validators.required],
    activo: [true],
  });
  constructor() {
    this.cargar();
  }
  cargar() {
    this.ocupado.set(true);
    this.api.listar().subscribe({
      next: (r) => {
        this.items.set(r);
        this.ocupado.set(false);
      },
      error: (e) => {
        this.error.set(mensajeHttp(e));
        this.ocupado.set(false);
      },
    });
  }
  editar(i: Insumo) {
    this.editando.set(i.id);
    this.form.setValue({
      nombre: i.nombre,
      descripcion: i.descripcion ?? '',
      unidadMedida: i.unidadMedida,
      activo: i.activo,
    });
    this.error.set('');
  }
  nuevo() {
    this.editando.set(null);
    this.form.reset({ nombre: '', descripcion: '', unidadMedida: 'UNIDAD', activo: true });
    this.error.set('');
  }
  guardar() {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.ocupado()) return;
    this.ocupado.set(true);
    this.error.set('');
    this.aviso.set('');
    const v = this.form.getRawValue();
    const request = { ...v, descripcion: v.descripcion.trim() || null };
    const id = this.editando();
    (id ? this.api.actualizar(id, request) : this.api.crear(request)).subscribe({
      next: () => {
        this.nuevo();
        this.aviso.set('Insumo guardado. El stock se gestiona mediante compras y movimientos.');
        this.cargar();
      },
      error: (e) => {
        this.error.set(mensajeHttp(e));
        this.ocupado.set(false);
      },
    });
  }
}
