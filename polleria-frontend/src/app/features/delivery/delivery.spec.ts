import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PedidoDetalle } from '../pedidos/pedido-detalle';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { MatDialog, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { DeliveryService } from './delivery-service';
import { DeliveryList } from './delivery-list';
import { MisEntregas } from './mis-entregas';
import { ConfirmarEntrega } from './confirmar-entrega';
import { Entrega } from './delivery.model';
import { Interno } from '../../core/layout/interno/interno';
import { AuthService } from '../../core/auth/auth-service';
import { authFixture } from '../../testing/auth-fixture';
import { routes } from '../../app.routes';
import { rolGuard } from '../../core/guards/rol-guard';
const base = 'http://localhost:8080/api/v1/delivery/';
const repartidor = { id: 1, nombreVisible: 'Repartidor A', activo: true };
const entrega: Entrega = {
  id: 5,
  pedidoId: 10,
  repartidor,
  fechaAsignacion: '2026-10-25T12:00:00',
  fechaSalida: null,
  fechaEntrega: null,
  estado: 'ASIGNADO',
  observacion: null,
  pedido: {
    id: 10,
    estadoPedido: 'LISTO',
    nombreCliente: 'Cliente Prueba',
    telefono: '999000000',
    direccionEntrega: 'Histórica',
    referenciaEntrega: null,
    total: 20,
    metodoPago: 'EFECTIVO',
    estadoPago: 'PENDIENTE',
  },
};
describe('Delivery y reparto', () => {
  let http: HttpTestingController;
  const dialog = { open: vi.fn(() => ({ afterClosed: () => of(true) })) };
  beforeEach(() => {
    dialog.open.mockReset();
    dialog.open.mockReturnValue({ afterClosed: () => of(true) });
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: new Map([['id', '1738']]) } },
        },
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authFixture(['REPARTIDOR']) },
        { provide: MatDialog, useValue: dialog },
        { provide: MAT_DIALOG_DATA, useValue: true },
        { provide: MatDialogRef, useValue: { close: vi.fn() } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('activar perfil no envía sub ni nombres elegidos por UI', () => {
    TestBed.inject(DeliveryService).activar().subscribe();
    const r = http.expectOne(base + 'repartidores/me');
    expect(r.request.method).toBe('POST');
    expect(r.request.body).toEqual({});
    r.flush(repartidor);
  });
  it('asignar y reasignar usa solo IDs operativos', () => {
    const api = TestBed.inject(DeliveryService);
    api.asignar(10, 1).subscribe();
    let r = http.expectOne(base + 'entregas');
    expect(r.request.body).toEqual({ pedidoId: 10, repartidorId: 1 });
    r.flush(entrega);
    api.reasignar(5, 2).subscribe();
    r = http.expectOne(base + 'entregas/5/reasignar');
    expect(r.request.body).toEqual({ repartidorId: 2 });
    r.flush(entrega);
  });
  function interno() {
    const f = TestBed.createComponent(DeliveryList);
    http.expectOne(base + 'entregas').flush([entrega]);
    http.expectOne(base + 'repartidores').flush([repartidor]);
    const r = http.expectOne((req) => req.url === 'http://localhost:8080/api/v1/pedidos');
    expect(r.request.params.get('tipoPedido')).toBe('DELIVERY');
    expect(r.request.params.get('estado')).toBe('LISTO');
    r.flush([{ id: 10 }, { id: 11, cliente: { nombres: 'Ana', apellidos: 'Prueba' } }]);
    return f;
  }
  it('listado reutiliza filtros y elimina pedidos ya asignados', () => {
    const f = interno();
    expect(f.componentInstance.candidatos().map((p) => p.id)).toEqual([11]);
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Repartidor A');
  });
  it('asigna formulario válido y recarga datos canónicos', () => {
    const f = interno();
    f.componentInstance.form.setValue({ pedidoId: 11, repartidorId: 1 });
    f.componentInstance.asignar();
    http.expectOne(base + 'entregas').flush(entrega);
    http.expectOne(base + 'entregas').flush([entrega]);
    http.expectOne(base + 'repartidores').flush([repartidor]);
    http.expectOne((req) => req.url.endsWith('/pedidos')).flush([]);
    expect(f.componentInstance.aviso()).toContain('asignada');
  });
  it('primer ingreso invita a activar perfil sin identificadores', () => {
    const f = TestBed.createComponent(MisEntregas);
    http.expectOne(base + 'repartidores/me').flush({}, { status: 404, statusText: 'Not found' });
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Activar perfil de reparto');
    f.componentInstance.activar();
    http.expectOne(base + 'repartidores/me').flush(repartidor);
    http.expectOne(base + 'repartidores/me').flush(repartidor);
    http.expectOne(base + 'entregas/mias').flush([]);
    expect(f.componentInstance.perfil()?.id).toBe(1);
  });
  function propias(e: Entrega = entrega) {
    const f = TestBed.createComponent(MisEntregas);
    http.expectOne(base + 'repartidores/me').flush(repartidor);
    http.expectOne(base + 'entregas/mias').flush([e]);
    f.detectChanges();
    return f;
  }
  it('consulta solo mias y muestra dirección histórica sin administración', () => {
    const f = propias();
    expect(f.nativeElement.textContent).toContain('Histórica');
    expect(f.nativeElement.textContent).toContain('Cliente Prueba');
    expect(f.nativeElement.textContent).not.toContain('Asignar entrega');
    expect(f.nativeElement.textContent).toContain('Iniciar entrega');
    expect(f.nativeElement.textContent).not.toContain('No entregado');
  });
  it('iniciar usa intención explícita y recarga el estado', () => {
    const f = propias();
    f.componentInstance.iniciar(entrega);
    const r = http.expectOne(base + 'entregas/mias/5/iniciar');
    expect(r.request.body).toEqual({});
    r.flush({ ...entrega, estado: 'EN_CAMINO' });
    http.expectOne(base + 'entregas/mias').flush([{ ...entrega, estado: 'EN_CAMINO' }]);
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('No entregado');
    expect(f.nativeElement.textContent).not.toContain('Iniciar entrega');
  });
  it('entregar confirma y no envía estadoPago', () => {
    const e = { ...entrega, estado: 'EN_CAMINO' as const };
    const f = propias(e);
    f.componentInstance.finalizar(e, false);
    expect(dialog.open).toHaveBeenCalled();
    const r = http.expectOne(base + 'entregas/mias/5/entregar');
    expect(r.request.body).toEqual({});
    r.flush({ ...entrega, estado: 'ENTREGADO' });
    http.expectOne(base + 'entregas/mias').flush([{ ...entrega, estado: 'ENTREGADO' }]);
    f.detectChanges();
    expect(f.nativeElement.querySelectorAll('.delivery-actions button').length).toBe(0);
  });
  it('diálogo exige observación no vacía para NO_ENTREGADO', () => {
    const f = TestBed.createComponent(ConfirmarEntrega);
    const ref = TestBed.inject(MatDialogRef);
    f.componentInstance.form.controls.observacion.setValue('   ');
    f.componentInstance.confirmar();
    expect(ref.close).not.toHaveBeenCalled();
    f.componentInstance.form.controls.observacion.setValue('Cliente ausente');
    f.componentInstance.confirmar();
    expect(ref.close).toHaveBeenCalledWith('Cliente ausente');
  });
  it('NO_ENTREGADO conserva incidencia sin acciones operativas', () => {
    const f = propias({ ...entrega, estado: 'NO_ENTREGADO', observacion: 'Cliente ausente' });
    expect(f.nativeElement.querySelectorAll('.delivery-actions button').length).toBe(0);
    expect(f.nativeElement.textContent).toContain('Cliente ausente');
  });
  it('REPARTIDOR ve Mis entregas sin navegación comercial global', () => {
    const f = TestBed.createComponent(Interno);
    f.detectChanges();
    const nav = f.nativeElement.querySelector('nav[aria-label="Navegación del portal"]');
    expect(nav.textContent).toContain('Mis entregas');
    for (const texto of ['Categorías', 'Productos', 'Clientes', 'Pedidos', 'Delivery'])
      expect(nav.textContent).not.toContain(texto);
  });
  it('rutas conservan guards y roles diferentes', () => {
    const internos = routes.find((r) => r.path === 'interno')!.children!;
    expect(internos.find((r) => r.path === 'delivery')!.canActivate?.length).toBe(1);
    expect(internos.find((r) => r.path === 'reparto')!.canActivate?.length).toBe(1);
    expect(
      TestBed.runInInjectionContext(() => rolGuard('REPARTIDOR')({} as never, {} as never)),
    ).toBe(true);
    expect(
      TestBed.runInInjectionContext(() =>
        rolGuard('ADMINISTRADOR', 'CAJERO')({} as never, {} as never),
      ),
    ).not.toBe(true);
  });
  it('confirmación de pago digital es explícita y usa el endpoint interno', () => {
    const f = TestBed.createComponent(PedidoDetalle);
    const pedido = {
      id: 1738,
      cliente: { id: 676, nombres: 'Cliente', apellidos: 'Prueba' },
      fecha: '2026-10-25T12:00:00',
      tipoPedido: 'DELIVERY',
      estado: 'ENTREGADO',
      estadoPago: 'PENDIENTE',
      metodoPago: 'YAPE',
      direccionEntrega: 'Histórica',
      referenciaEntrega: null,
      total: 20,
      detalles: [],
    };
    http.expectOne('http://localhost:8080/api/v1/pedidos/1738').flush(pedido);
    f.componentInstance.confirmarPago();
    expect(dialog.open).toHaveBeenCalled();
    const r = http.expectOne('http://localhost:8080/api/v1/pedidos/1738/confirmar-pago');
    expect(r.request.method).toBe('PATCH');
    expect(r.request.body).toEqual({});
    r.flush({ ...pedido, estadoPago: 'PAGADO' });
    expect(f.componentInstance.pedido()?.estadoPago).toBe('PAGADO');
  });
});
