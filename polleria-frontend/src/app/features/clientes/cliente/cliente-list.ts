import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmarEliminacion } from '../../../shared/confirmar-eliminacion';
import { mensajeHttp } from '../../../shared/http-error';
import { ClienteService } from './cliente-service';
import { Cliente } from './cliente.model';

@Component({
  selector: 'app-cliente-list',
  imports: [RouterLink, MatButtonModule],
  templateUrl: './cliente-list.html',
})
export class ClienteList {
  private readonly service = inject(ClienteService);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  readonly datos = signal<Cliente[]>([]);
  readonly loading = signal(false);
  readonly eliminando = signal<number | null>(null);
  readonly error = signal('');
  readonly aviso = signal((history.state?.['aviso'] as string) || '');

  constructor() {
    this.cargar();
  }
  cargar() {
    this.loading.set(true);
    this.error.set('');
    this.service
      .listar()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.datos.set(data);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(mensajeHttp(err));
          this.loading.set(false);
        },
      });
  }
  eliminar(item: Cliente) {
    if (this.eliminando() !== null) return;
    this.dialog
      .open(ConfirmarEliminacion, {
        data: item.nombres + ' ' + item.apellidos,
        width: '26rem',
        maxWidth: 'calc(100vw - 2rem)',
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((confirmado) => {
        if (confirmado === true) this.eliminarConfirmado(item);
      });
  }
  private eliminarConfirmado(item: Cliente) {
    if (this.eliminando() !== null) return;
    this.error.set('');
    this.aviso.set('');
    this.eliminando.set(item.id);
    this.service
      .eliminar(item.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.eliminando.set(null);
          this.aviso.set('Registro eliminado.');
          this.cargar();
        },
        error: (err) => {
          this.eliminando.set(null);
          if (err.status === 404) {
            this.datos.update((data) => data.filter((row) => row.id !== item.id));
          }
          this.error.set(mensajeHttp(err));
        },
      });
  }
}
