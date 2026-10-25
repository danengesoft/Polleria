import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
@Component({
  selector: 'app-confirmar-entrega',
  imports: [MatDialogModule, MatButtonModule, ReactiveFormsModule],
  template: ` <h2 mat-dialog-title>
      {{ incidencia ? 'Registrar no entregado' : 'Confirmar entrega' }}
    </h2>
    <mat-dialog-content>
      @if (incidencia) {
        <form [formGroup]="form">
          <label
            >Observación obligatoria<textarea
              formControlName="observacion"
              maxlength="500"
              rows="4"
              style="display:block;width:100%;box-sizing:border-box"
            ></textarea>
          </label>
          @if (form.touched && form.invalid) {
            <p role="alert">Escribe una observación de hasta 500 caracteres.</p>
          }
        </form>
      } @else {
        <p>
          Confirma que entregaste el pedido al cliente. En efectivo pendiente se registrará el
          cobro; otros métodos requieren confirmación interna.
        </p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end"
      ><button matButton [mat-dialog-close]="null" cdkFocusInitial>Volver</button
      ><button matButton="filled" (click)="confirmar()">
        {{ incidencia ? 'Confirmar no entregado' : 'Confirmar entregado' }}
      </button></mat-dialog-actions
    >`,
})
export class ConfirmarEntrega {
  readonly incidencia = inject<boolean>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<ConfirmarEntrega>);
  readonly form = inject(FormBuilder).nonNullable.group({
    observacion: ['', [Validators.required, Validators.maxLength(500), Validators.pattern(/\S/)]],
  });
  confirmar() {
    this.form.markAllAsTouched();
    if (this.incidencia && this.form.invalid) return;
    this.ref.close(this.incidencia ? this.form.controls.observacion.value.trim() : true);
  }
}
