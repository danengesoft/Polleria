import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProveedorService } from './proveedor-service';
import { Proveedor } from './abastecimiento.model';
import { AbastecimientoNav } from './abastecimiento-nav';
import { mensajeHttp } from '../../shared/http-error';
@Component({
  selector: 'app-proveedores',
  imports: [ReactiveFormsModule, AbastecimientoNav],
  templateUrl: './proveedores.html',
  styleUrl: './abastecimiento.css',
})
export class Proveedores {
  private readonly api = inject(ProveedorService);
  private readonly fb = inject(FormBuilder);
  readonly items = signal<Proveedor[]>([]);
  readonly editando = signal<number | null>(null);
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly aviso = signal('');
  readonly form = this.fb.nonNullable.group({
    razonSocial: ['', [Validators.required, Validators.maxLength(150)]],
    ruc: ['', Validators.pattern(/^\d{11}$/)],
    telefono: ['', Validators.pattern(/^(\+51)?[0-9]{7,9}$/)],
    correo: ['', [Validators.email, Validators.maxLength(254)]],
    direccion: ['', Validators.maxLength(300)],
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
  editar(p: Proveedor) {
    this.editando.set(p.id);
    this.form.setValue({
      razonSocial: p.razonSocial,
      ruc: p.ruc ?? '',
      telefono: p.telefono ?? '',
      correo: p.correo ?? '',
      direccion: p.direccion ?? '',
      activo: p.activo,
    });
    this.error.set('');
  }
  nuevo() {
    this.editando.set(null);
    this.form.reset({
      razonSocial: '',
      ruc: '',
      telefono: '',
      correo: '',
      direccion: '',
      activo: true,
    });
    this.error.set('');
  }
  guardar() {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.ocupado()) return;
    this.ocupado.set(true);
    this.error.set('');
    this.aviso.set('');
    const v = this.form.getRawValue();
    const r = {
      ...v,
      ruc: v.ruc.trim() || null,
      telefono: v.telefono.trim() || null,
      correo: v.correo.trim() || null,
      direccion: v.direccion.trim() || null,
    };
    const id = this.editando();
    (id ? this.api.actualizar(id, r) : this.api.crear(r)).subscribe({
      next: () => {
        this.nuevo();
        this.aviso.set('Proveedor guardado.');
        this.cargar();
      },
      error: (e) => {
        this.error.set(mensajeHttp(e));
        this.ocupado.set(false);
      },
    });
  }
}
