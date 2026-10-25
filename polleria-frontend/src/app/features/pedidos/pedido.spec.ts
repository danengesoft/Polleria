import { AuthService } from '../../core/auth/auth-service';
import { authFixture } from '../../testing/auth-fixture';
import { Type } from '@angular/core';
import { By } from '@angular/platform-browser';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { routes } from '../../app.routes';
import { environment } from '../../../environments/environment';
import { PedidoService } from './pedido-service';
import { PedidoForm } from './pedido-form';
import { PedidoDetalle } from './pedido-detalle';
import { PedidoList } from './pedido-list';
import { EstadoPedido, PedidoResponse, PedidoRequest, transiciones } from './pedido.model';

const base = environment.apiBaseUrl + '/api/v1';
const cliente = {
  id: 1,
  nombres: 'Ana',
  apellidos: 'Prueba',
  telefono: '987654321',
  correo: null,
  direccionPrincipal: 'Dirección principal',
  referencia: 'Referencia',
  activo: true,
};
const producto = {
  id: 10,
  nombre: 'Pollo',
  precio: 20,
  descripcion: null,
  disponible: true,
  imagenUrl: null,
  categoria: { id: 24, nombre: 'Pollos' },
};
const otro = { ...producto, id: 11, nombre: 'Bebida', precio: 5.5 };
const pedido: PedidoResponse = {
  id: 1,
  cliente: { id: 1, nombres: 'Ana', apellidos: 'Prueba' },
  fecha: '2026-10-24T20:00:00',
  tipoPedido: 'RECOJO',
  estado: 'PENDIENTE',
  estadoPago: 'PENDIENTE',
  metodoPago: 'EFECTIVO',
  direccionEntrega: null,
  referenciaEntrega: null,
  total: 40,
  detalles: [
    { productoId: 10, nombreProducto: 'Pollo', cantidad: 2, precioUnitario: 20, subtotal: 40 },
  ],
};
async function navegar<T>(
  harness: RouterTestingHarness,
  url: string,
  component: Type<T>,
): Promise<T> {
  await harness.navigateByUrl(url);
  return harness.fixture.debugElement.query(By.directive(component)).componentInstance as T;
}

describe('Pedidos transaccionales', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useFactory: () => authFixture() },provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    try {
      http.verify();
    } finally {
      vi.restoreAllMocks();
      TestBed.resetTestingModule();
    }
  });
  async function formulario() {
    const harness = await RouterTestingHarness.create();
    const form = await navegar(harness, '/interno/pedidos/nuevo', PedidoForm);
    http.expectOne(`${base}/clientes`).flush([cliente]);
    http
      .expectOne(`${base}/productos`)
      .flush([producto, otro, { ...producto, id: 12, disponible: false }]);
    return { harness, form };
  }
  function confirmar(value = true) {
    return vi
      .spyOn(TestBed.inject(MatDialog), 'open')
      .mockReturnValue({ afterClosed: () => of(value) } as ReturnType<MatDialog['open']>);
  }
  function completar(form: PedidoForm) {
    form.form.controls.clienteId.setValue(1);
    form.form.controls.tipoPedido.setValue('RECOJO');
    form.seleccionarProducto(0, 10);
    form.lineasForm.at(0).controls.cantidad.setValue(2);
  }

  it('PedidoService consume solo GET, POST y PATCH existentes y filtra en servidor', () => {
    const service = TestBed.inject(PedidoService);
    service.listar().subscribe();
    http.expectOne(`${base}/pedidos`).flush([]);
    service.listar({ estado: 'PENDIENTE', tipoPedido: 'RECOJO' }).subscribe();
    http.expectOne(`${base}/pedidos?estado=PENDIENTE&tipoPedido=RECOJO`).flush([pedido]);
    service.obtener(1).subscribe();
    http.expectOne(`${base}/pedidos/1`).flush(pedido);
    const request: PedidoRequest = {
      clienteId: 1,
      tipoPedido: 'RECOJO',
      metodoPago: 'EFECTIVO',
      direccionEntrega: null,
      referenciaEntrega: null,
      detalles: [{ productoId: 10, cantidad: 2 }],
    };
    service.crear(request).subscribe();
    const post = http.expectOne(`${base}/pedidos`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual(request);
    post.flush(pedido);
    service.cambiarEstado(1, 'CONFIRMADO').subscribe();
    const patch = http.expectOne(`${base}/pedidos/1/estado`);
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({ estado: 'CONFIRMADO' });
    patch.flush({ ...pedido, estado: 'CONFIRMADO' });
    expect('eliminar' in service).toBe(false);
    expect('actualizar' in service).toBe(false);
  });
  it('carga clientes y solo permite productos disponibles elegidos por ID', async () => {
    const { form } = await formulario();
    expect(form.clientes()[0].id).toBe(1);
    expect(form.productos().map((p) => p.id)).toEqual([10, 11]);
    form.lineasForm.at(0).controls.busqueda.setValue('Pollo');
    expect(form.lineasForm.at(0).invalid).toBe(true);
    form.seleccionarProducto(0, 10);
    expect(form.lineasForm.at(0).controls.productoId.value).toBe(10);
    form.buscarProducto(0);
    expect(form.lineasForm.at(0).controls.productoId.value).toBe(0);
    form.seleccionarProducto(0, 12);
    expect(form.lineasForm.at(0).controls.productoId.value).toBe(0);
  });
  it('FormArray conserva identidad al quitar la primera línea y actualiza cálculos', async () => {
    const { form } = await formulario();
    completar(form);
    form.agregarLinea();
    form.seleccionarProducto(1, 11);
    form.lineasForm.at(1).controls.cantidad.setValue(3);
    expect(form.subtotales()).toEqual([40, 16.5]);
    expect(form.total()).toBe(56.5);
    const segunda = form.lineasForm.at(1);
    form.quitarLinea(0);
    expect(form.lineasForm.at(0)).toBe(segunda);
    expect(form.total()).toBe(16.5);
    form.quitarLinea(0);
    expect(form.lineasForm.length).toBe(1);
  });
  it('rechaza colección vacía y cantidades no enteras positivas', async () => {
    const { form } = await formulario();
    completar(form);
    for (const cantidad of [0, -1, 1.5, 2147483648]) {
      form.lineasForm.at(0).controls.cantidad.setValue(cantidad);
      expect(form.lineasForm.invalid).toBe(true);
    }
    form.lineasForm.at(0).controls.cantidad.setValue(1);
    expect(form.lineasForm.valid).toBe(true);
    form.lineasForm.clear();
    expect(form.lineasForm.hasError('sinDetalles')).toBe(true);
    form.confirmarYGuardar();
    http.expectNone(`${base}/pedidos`);
  });
  it('evita productos duplicados y valida también el conjunto de líneas', async () => {
    const { form } = await formulario();
    completar(form);
    form.agregarLinea();
    expect(form.opcionesPorLinea()[1].some((p) => p.id === 10)).toBe(false);
    form.seleccionarProducto(1, 10);
    expect(form.lineasForm.at(1).controls.productoId.value).toBe(0);
    form.lineasForm.at(1).controls.productoId.setValue(10);
    expect(form.lineasForm.hasError('duplicados')).toBe(true);
  });
  it('Delivery requiere dirección y cambiar cliente no sobrescribe la dirección manual', async () => {
    const { form } = await formulario();
    completar(form);
    form.form.controls.tipoPedido.setValue('DELIVERY');
    expect(form.form.controls.direccionEntrega.invalid).toBe(true);
    form.usarDireccionCliente();
    expect(form.form.controls.direccionEntrega.value).toBe('Dirección principal');
    form.form.controls.direccionEntrega.setValue('Dirección efectiva manual');
    form.form.controls.clienteId.setValue(999);
    expect(form.form.controls.direccionEntrega.value).toBe('Dirección efectiva manual');
    form.form.controls.direccionEntrega.setValue('');
    form.form.controls.tipoPedido.setValue('PRESENCIAL');
    expect(form.form.controls.direccionEntrega.valid).toBe(true);
  });
  it('rechaza cliente o producto que no están entre las opciones cargadas', async () => {
    const { form } = await formulario();
    completar(form);
    form.form.controls.clienteId.setValue(999);
    form.confirmarYGuardar();
    expect(form.error()).toContain('cliente existente');
    http.expectNone(`${base}/pedidos`);
    form.form.controls.clienteId.setValue(1);
    form.lineasForm.at(0).controls.productoId.setValue(999);
    form.confirmarYGuardar();
    expect(form.error()).toContain('productos disponibles');
    http.expectNone(`${base}/pedidos`);
  });
  it('cancelar confirmación no crea pedido ni pierde datos', async () => {
    const { form } = await formulario();
    completar(form);
    confirmar(false);
    form.confirmarYGuardar();
    http.expectNone(`${base}/pedidos`);
    expect(form.form.enabled).toBe(true);
    expect(form.lineasForm.at(0).controls.cantidad.value).toBe(2);
  });
  it('request exacto excluye precios, total, fecha, estados y texto de búsqueda; usa respuesta real', async () => {
    const { harness, form } = await formulario();
    completar(form);
    confirmar();
    form.confirmarYGuardar();
    form.confirmarYGuardar();
    const post = http.expectOne(`${base}/pedidos`);
    expect(post.request.body).toEqual({
      clienteId: 1,
      tipoPedido: 'RECOJO',
      metodoPago: 'EFECTIVO',
      direccionEntrega: null,
      referenciaEntrega: null,
      detalles: [{ productoId: 10, cantidad: 2 }],
    });
    expect(form.guardando()).toBe(true);
    expect(form.form.disabled).toBe(true);
    post.flush({
      ...pedido,
      total: 50,
      detalles: [{ ...pedido.detalles[0], precioUnitario: 25, subtotal: 50 }],
    });
    await harness.fixture.whenStable();
    http
      .expectOne(`${base}/pedidos/1`)
      .flush({
        ...pedido,
        total: 50,
        detalles: [{ ...pedido.detalles[0], precioUnitario: 25, subtotal: 50 }],
      });
    await harness.fixture.whenStable();
    expect(harness.routeNativeElement?.textContent).toContain('S/ 50.00');
    // La vista histórica no consulta Catálogo para sustituir precios.
    http.expectNone(`${base}/productos`);
  });
  it('conflicto por disponibilidad conserva formulario y recarga opciones', async () => {
    const { form } = await formulario();
    completar(form);
    confirmar();
    form.confirmarYGuardar();
    http
      .expectOne(`${base}/pedidos`)
      .flush({ message: 'Producto no disponible: 10' }, { status: 409, statusText: 'Conflict' });
    expect(form.error()).toContain('no disponible');
    expect(form.form.enabled).toBe(true);
    http.expectOne(`${base}/clientes`).flush([cliente]);
    http.expectOne(`${base}/productos`).flush([{ ...producto, disponible: false }]);
    expect(form.productos()).toEqual([]);
    expect(form.lineasForm.at(0).controls.cantidad.value).toBe(2);
  });
  it('ofrece únicamente transiciones reales y nunca despacho o entrega Delivery', () => {
    expect(transiciones(pedido)).toEqual(['CONFIRMADO', 'CANCELADO']);
    expect(transiciones({ ...pedido, estado: 'CONFIRMADO' })).toEqual([
      'EN_PREPARACION',
      'CANCELADO',
    ]);
    expect(transiciones({ ...pedido, estado: 'EN_PREPARACION' })).toEqual(['LISTO']);
    expect(transiciones({ ...pedido, estado: 'LISTO' })).toEqual(['ENTREGADO']);
    expect(transiciones({ ...pedido, estado: 'LISTO', tipoPedido: 'DELIVERY' })).toEqual([]);
    expect(transiciones({ ...pedido, estadoPago: 'PAGADO' })).toEqual(['CONFIRMADO']);
    for (const estado of ['ENTREGADO', 'CANCELADO', 'DESPACHADO'] as EstadoPedido[])
      expect(transiciones({ ...pedido, estado })).toEqual([]);
  });
  it('detalle cambia estado solo con confirmación y conserva error 409 del servidor', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await navegar(harness, '/interno/pedidos/1', PedidoDetalle);
    http.expectOne(`${base}/pedidos/1`).flush(pedido);
    const dialog = confirmar(false);
    component.cambiarEstado('CANCELADO');
    http.expectNone(`${base}/pedidos/1/estado`);
    dialog.mockReturnValue({ afterClosed: () => of(true) } as ReturnType<MatDialog['open']>);
    component.cambiarEstado('ENTREGADO');
    http.expectNone(`${base}/pedidos/1/estado`);
    component.cambiarEstado('CONFIRMADO');
    http.expectOne(`${base}/pedidos/1/estado`).flush({ ...pedido, estado: 'CONFIRMADO' });
    expect(component.pedido()?.estado).toBe('CONFIRMADO');
    component.cambiarEstado('CANCELADO');
    http
      .expectOne(`${base}/pedidos/1/estado`)
      .flush(
        { message: 'Transición de pedido no permitida' },
        { status: 409, statusText: 'Conflict' },
      );
    expect(component.error()).toContain('no permitida');
    expect(component.pedido()?.estado).toBe('CONFIRMADO');
  });
  it('listado presenta totales del servidor y aplica filtros HTTP', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await navegar(harness, '/interno/pedidos', PedidoList);
    http.expectOne(`${base}/pedidos`).flush([pedido]);
    await harness.fixture.whenStable();
    expect(harness.routeNativeElement?.textContent).toContain('Ana Prueba');
    expect(harness.routeNativeElement?.textContent).toContain('S/ 40.00');
    component.filtrarEstado('CONFIRMADO');
    http.expectOne(`${base}/pedidos?estado=CONFIRMADO`).flush([]);
    component.filtrarTipo('DELIVERY');
    http.expectOne(`${base}/pedidos?estado=CONFIRMADO&tipoPedido=DELIVERY`).flush([]);
  });
});
