import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { mensajeHttp } from '../../../shared/http-error';
import {
  mensajeValidacion,
  textoObligatorio,
  precioMonetario,
} from '../../../shared/form-validation';
import { ProductoService } from './producto-service';
import { ProductoRequest } from './producto.model';
import { CategoriaService } from '../categoria/categoria-service';
import { Categoria } from '../categoria/categoria.model';
import { MatSelectModule } from '@angular/material/select';
@Component({
  selector: 'app-producto-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatSelectModule,
  ],
  templateUrl: './producto-form.html',
})
export class ProductoForm {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ProductoService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly id = signal<number | null>(null);
  readonly loading = signal(false);
  readonly guardando = signal(false);
  readonly errorCarga = signal(false);
  readonly error = signal('');
  readonly validacion = mensajeValidacion;
  readonly form = this.fb.nonNullable.group({
    nombre: ['', [textoObligatorio, Validators.maxLength(120)]],
    descripcion: ['', [Validators.maxLength(500)]],
    precio: ['', [Validators.required, precioMonetario]],
    categoriaId: [0, [Validators.required, Validators.min(1)]],
    imagenUrl: ['', [Validators.maxLength(1000), Validators.pattern(/^https?:\/\/[^\s]+$/i)]],
    disponible: [true, []],
  });
  private readonly categoriaService = inject(CategoriaService);
  readonly categorias = signal<Categoria[]>([]);
  readonly categoriasCargadas = signal(false);
  readonly errorCategorias = signal('');
  cargarCategorias() {
    this.categoriasCargadas.set(false);
    this.errorCategorias.set('');
    this.categoriaService
      .listar()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.categorias.set(data);
          this.categoriasCargadas.set(true);
          if (!data.some((c) => c.id === this.form.controls.categoriaId.value))
            this.form.controls.categoriaId.setValue(0);
        },
        error: (err) => this.errorCategorias.set(mensajeHttp(err)),
      });
  }

  constructor() {
    const param = this.route.snapshot.paramMap.get('id');
    if (param !== null) {
      const id = Number(param);
      if (!Number.isSafeInteger(id) || id <= 0) {
        this.errorCarga.set(true);
        this.error.set('Identificador inválido.');
        return;
      }
      this.id.set(id);
      this.loading.set(true);
      this.service
        .obtener(id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (data) => {
            this.form.patchValue({
              ...data,
              descripcion: data.descripcion ?? '',
              imagenUrl: data.imagenUrl ?? '',
              precio: String(data.precio),
              categoriaId: data.categoria.id,
            });
            this.cargarCategorias();
            this.loading.set(false);
          },
          error: (err) => {
            this.loading.set(false);
            this.errorCarga.set(true);
            this.error.set(mensajeHttp(err));
          },
        });
    } else {
      this.cargarCategorias();
    }
  }
  guardar() {
    if (
      this.loading() ||
      this.guardando() ||
      this.errorCarga() ||
      !this.categoriasCargadas() ||
      !this.categorias().length
    )
      return;
    this.form.controls.nombre.setValue(this.form.controls.nombre.value.trim());
    this.error.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const data: ProductoRequest = {
      nombre: value.nombre.trim(),
      descripcion: value.descripcion.trim() || null,
      precio: Number(value.precio),
      categoriaId: value.categoriaId,
      imagenUrl: value.imagenUrl.trim() || null,
      disponible: value.disponible,
    };
    this.guardando.set(true);
    this.form.disable();
    const id = this.id();
    const request = id === null ? this.service.crear(data) : this.service.actualizar(id, data);
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () =>
        this.router.navigate(['/interno/productos'], {
          state: { aviso: id === null ? 'Registro creado.' : 'Cambios guardados.' },
        }),
      error: (err) => {
        this.guardando.set(false);
        this.form.enable();
        this.error.set(mensajeHttp(err));
        if (err.status === 404 && /categor[ií]a/i.test(mensajeHttp(err))) this.cargarCategorias();
      },
    });
  }
}
