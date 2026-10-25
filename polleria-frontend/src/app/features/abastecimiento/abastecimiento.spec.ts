import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { InsumoService } from './insumo-service';
import { ProveedorService } from './proveedor-service';
import { CompraService } from './compra-service';
import { InventarioService } from './inventario-service';
import { Insumos } from './insumos';
import { Proveedores } from './proveedores';
import { Compras } from './compras';
import { Inventario } from './inventario';
import { Insumo, Proveedor, Compra, Movimiento } from './abastecimiento.model';
import { AuthService } from '../../core/auth/auth-service';
import { authFixture } from '../../testing/auth-fixture';
import { Interno } from '../../core/layout/interno/interno';
import { rolGuard } from '../../core/guards/rol-guard';
import { routes } from '../../app.routes';
const base = 'http://localhost:8080/api/v1/abastecimiento/';
const insumo: Insumo = {
  id: 1,
  nombre: 'Aceite',
  descripcion: null,
  unidadMedida: 'L',
  stockActual: 0,
  activo: true,
};
const proveedor: Proveedor = {
  id: 2,
  razonSocial: 'Proveedor prueba',
  ruc: null,
  telefono: null,
  correo: null,
  direccion: null,
  activo: true,
};
const compra: Compra = {
  id: 3,
  proveedor: { id: 2, razonSocial: 'Proveedor prueba' },
  fecha: '2026-10-25T12:00:00',
  total: 30.86,
  detalles: [
    {
      id: 4,
      insumoId: 1,
      nombreInsumo: 'Aceite histórico',
      unidadMedida: 'L',
      cantidad: 2.5,
      costoUnitario: 12.3456,
      subtotal: 30.86,
    },
  ],
};
const movimiento: Movimiento = {
  id: 5,
  insumoId: 1,
  nombreInsumo: 'Aceite',
  unidadMedida: 'L',
  tipo: 'ENTRADA',
  cantidad: 2.5,
  stockAnterior: 0,
  stockPosterior: 2.5,
  fecha: '2026-10-25T12:00:00',
  motivo: 'Compra #3',
  compraId: 3,
};
describe('Abastecimiento', () => {
  let http: HttpTestingController;
  let auth = authFixture(['ABASTECIMIENTO']);
  beforeEach(() => {
    sessionStorage.clear();
    auth = authFixture(['ABASTECIMIENTO']);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('InsumoService crea y edita campos sin stock', () => {
    const api = TestBed.inject(InsumoService);
    const r = { nombre: 'Aceite', descripcion: null, unidadMedida: 'L' as const, activo: true };
    api.crear(r).subscribe();
    const post = http.expectOne(base + 'insumos');
    expect(post.request.body).toEqual(r);
    expect(post.request.body.stockActual).toBeUndefined();
    post.flush(insumo);
    api.actualizar(1, r).subscribe();
    const put = http.expectOne(base + 'insumos/1');
    expect(put.request.method).toBe('PUT');
    put.flush(insumo);
  });
  it('ProveedorService conserva RUC string y campos opcionales', () => {
    const r = { ...proveedor, ruc: '20123456789' };
    TestBed.inject(ProveedorService).crear(r).subscribe();
    const req = http.expectOne(base + 'proveedores');
    expect(req.request.body.ruc).toBe('20123456789');
    expect(req.request.method).toBe('POST');
    req.flush(proveedor);
  });
  it('CompraService filtra sin mutaciones de historial', () => {
    TestBed.inject(CompraService).listar({ proveedorId: 2, fechaDesde: '2026-10-01' }).subscribe();
    const req = http.expectOne((r) => r.url === base + 'compras');
    expect(req.request.params.get('proveedorId')).toBe('2');
    expect(req.request.params.get('fechaDesde')).toBe('2026-10-01');
    req.flush([compra]);
    TestBed.inject(CompraService).obtener(3).subscribe();
    http.expectOne(base + 'compras/3').flush(compra);
  });
  it('InventarioService envía intenciones sin stock anterior/posterior', () => {
    const api = TestBed.inject(InventarioService);
    api.salida(1, '2.125', 'Uso').subscribe();
    const s = http.expectOne(base + 'movimientos/salidas');
    expect(s.request.body).toEqual({ insumoId: 1, cantidad: '2.125', motivo: 'Uso' });
    s.flush(movimiento);
    api.ajuste(1, '8.500', 'Conteo').subscribe();
    const a = http.expectOne(base + 'movimientos/ajustes');
    expect(a.request.body).toEqual({ insumoId: 1, nuevoStock: '8.500', motivo: 'Conteo' });
    a.flush(movimiento);
  });
  it('Insumo formulario muestra stock inicial cero sin input editable', () => {
    const f = TestBed.createComponent(Insumos);
    http.expectOne(base + 'insumos').flush([]);
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Stock inicial: 0.000');
    expect(f.nativeElement.querySelector('[formControlName="stockActual"]')).toBeNull();
    f.componentInstance.form.patchValue({ nombre: 'Aceite', unidadMedida: 'L' });
    f.componentInstance.guardar();
    const r = http.expectOne(base + 'insumos');
    expect(r.request.body).toEqual({
      nombre: 'Aceite',
      descripcion: null,
      unidadMedida: 'L',
      activo: true,
    });
    r.flush(insumo);
    http.expectOne(base + 'insumos').flush([insumo]);
  });
  it('edición de Insumo preserva stock y permite desactivar', () => {
    const f = TestBed.createComponent(Insumos);
    http.expectOne(base + 'insumos').flush([insumo]);
    f.componentInstance.editar({ ...insumo, stockActual: 5 });
    f.componentInstance.form.controls.activo.setValue(false);
    f.componentInstance.guardar();
    const r = http.expectOne(base + 'insumos/1');
    expect(r.request.body.stockActual).toBeUndefined();
    expect(r.request.body.activo).toBe(false);
    r.flush({ ...insumo, stockActual: 5, activo: false });
    http.expectOne(base + 'insumos').flush([]);
  });
  it('Proveedor valida RUC y comunica conflicto sin perder formulario', () => {
    const f = TestBed.createComponent(Proveedores);
    http.expectOne(base + 'proveedores').flush([]);
    f.componentInstance.form.patchValue({ razonSocial: 'Proveedor', ruc: '123' });
    expect(f.componentInstance.form.invalid).toBe(true);
    f.componentInstance.form.controls.ruc.setValue('20123456789');
    f.componentInstance.guardar();
    http
      .expectOne(base + 'proveedores')
      .flush(
        { message: 'Ya existe un proveedor con ese RUC' },
        { status: 409, statusText: 'Conflict' },
      );
    expect(f.componentInstance.error()).toContain('RUC');
    expect(f.componentInstance.form.controls.razonSocial.value).toBe('Proveedor');
  });
  function prepararCompra() {
    const f = TestBed.createComponent(Compras);
    http.expectOne(base + 'compras').flush([]);
    http
      .expectOne(base + 'insumos')
      .flush([insumo, { ...insumo, id: 6, nombre: 'Papas' }, { ...insumo, id: 8, activo: false }]);
    http.expectOne(base + 'proveedores').flush([proveedor, { ...proveedor, id: 7, activo: false }]);
    return f;
  }
  it('Compra FormArray calcula estimación y selecciona solo activos', () => {
    const f = prepararCompra();
    const c = f.componentInstance;
    c.form.controls.proveedorId.setValue(2);
    c.form.controls.detalles
      .at(0)
      .setValue({ insumoId: 1, cantidad: '2.500', costoUnitario: '12.3456' });
    c.agregar();
    c.form.controls.detalles
      .at(1)
      .setValue({ insumoId: 6, cantidad: '3.125', costoUnitario: '4.2000' });
    expect(c.total()).toBe(43.99);
    expect(c.activos().map((i) => i.id)).toEqual([1, 6]);
    expect(c.proveedores().length).toBe(1);
    c.quitar(1);
    expect(c.form.controls.detalles.length).toBe(1);
  });
  it('Compra bloquea duplicados y colección vacía', () => {
    const f = prepararCompra();
    const c = f.componentInstance;
    c.form.controls.proveedorId.setValue(2);
    c.form.controls.detalles.at(0).setValue({ insumoId: 1, cantidad: '1', costoUnitario: '2' });
    c.agregar();
    c.form.controls.detalles.at(1).setValue({ insumoId: 1, cantidad: '2', costoUnitario: '3' });
    expect(c.form.hasError('duplicados')).toBe(true);
    c.quitar(1);
    c.quitar(0);
    expect(c.form.hasError('sinDetalles')).toBe(true);
  });
  it('Compra envía solo IDs/cantidad/costo y muestra total real, stock y entradas', () => {
    const f = prepararCompra();
    const c = f.componentInstance;
    c.form.controls.proveedorId.setValue(2);
    c.form.controls.detalles
      .at(0)
      .setValue({ insumoId: 1, cantidad: '2.500', costoUnitario: '12.3456' });
    c.guardar();
    const r = http.expectOne(base + 'compras');
    expect(r.request.body).toEqual({
      proveedorId: 2,
      detalles: [{ insumoId: 1, cantidad: '2.500', costoUnitario: '12.3456' }],
    });
    r.flush(compra);
    http.expectOne(base + 'movimientos').flush([movimiento]);
    http.expectOne(base + 'compras').flush([compra]);
    http.expectOne(base + 'insumos').flush([{ ...insumo, stockActual: 2.5 }]);
    http.expectOne(base + 'proveedores').flush([proveedor]);
    expect(c.seleccionada()?.total).toBe(30.86);
    expect(c.insumos()[0].stockActual).toBe(2.5);
    expect(c.entradas().length).toBe(1);
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Aceite histórico');
  });
  it('Compra fallida conserva cabecera y detalle para corregir', () => {
    const f = prepararCompra();
    const c = f.componentInstance;
    c.form.controls.proveedorId.setValue(2);
    c.form.controls.detalles.at(0).setValue({ insumoId: 1, cantidad: '1', costoUnitario: '2' });
    c.guardar();
    http
      .expectOne(base + 'compras')
      .flush({ message: 'Insumo inactivo' }, { status: 409, statusText: 'Conflict' });
    expect(c.form.controls.detalles.length).toBe(1);
    expect(c.error()).toBe('Insumo inactivo');
  });
  function prepararInventario() {
    const f = TestBed.createComponent(Inventario);
    http.expectOne(base + 'insumos').flush([{ ...insumo, stockActual: 5 }]);
    http.expectOne(base + 'movimientos').flush([]);
    return f;
  }
  it('Salida requiere motivo/cantidad y muestra rechazo por stock insuficiente', () => {
    const f = prepararInventario();
    const c = f.componentInstance;
    c.form.patchValue({ insumoId: 1, cantidad: '8', motivo: 'Merma' });
    c.guardar();
    http
      .expectOne(base + 'movimientos/salidas')
      .flush(
        { message: 'Stock insuficiente para registrar la salida' },
        { status: 409, statusText: 'Conflict' },
      );
    expect(c.error()).toContain('Stock insuficiente');
    expect(c.stock()?.stockActual).toBe(5);
    expect(c.form.controls.cantidad.value).toBe('8');
  });
  it('Ajuste envía nuevo conteo y refresca stock/historial tras éxito', () => {
    const f = prepararInventario();
    const c = f.componentInstance;
    c.form.setValue({
      operacion: 'AJUSTE',
      insumoId: 1,
      cantidad: '0.000',
      motivo: 'Conteo físico',
    });
    c.guardar();
    const r = http.expectOne(base + 'movimientos/ajustes');
    expect(r.request.body).toEqual({ insumoId: 1, nuevoStock: '0.000', motivo: 'Conteo físico' });
    r.flush({ ...movimiento, tipo: 'AJUSTE', cantidad: -5, stockAnterior: 5, stockPosterior: 0 });
    http.expectOne(base + 'insumos').flush([insumo]);
    http.expectOne(base + 'movimientos').flush([]);
    expect(c.aviso()).toContain('0.000');
    expect(c.stock()?.stockActual).toBe(0);
  });
  it('Movimientos envía filtros de insumo/tipo', () => {
    const f = prepararInventario();
    f.componentInstance.filtros.setValue({ insumoId: 1, tipo: 'SALIDA' });
    f.componentInstance.cargarMovimientos();
    const r = http.expectOne((req) => req.url === base + 'movimientos');
    expect(r.request.params.get('insumoId')).toBe('1');
    expect(r.request.params.get('tipo')).toBe('SALIDA');
    r.flush([]);
  });
  it('ABASTECIMIENTO tiene navegación y guards propios sin módulos comerciales ajenos', () => {
    const f = TestBed.createComponent(Interno);
    f.detectChanges();
    const nav = f.nativeElement.querySelector('nav[aria-label="Navegación del portal"]');
    expect(nav.textContent).toContain('Abastecimiento');
    for (const t of ['Clientes', 'Pedidos', 'Delivery', 'Mis entregas'])
      expect(nav.textContent).not.toContain(t);
    expect(
      TestBed.runInInjectionContext(() =>
        rolGuard('ADMINISTRADOR', 'ABASTECIMIENTO')({} as never, {} as never),
      ),
    ).toBe(true);
    const children = routes.find((r) => r.path === 'interno')!.children!;
    expect(children.filter((r) => r.path?.startsWith('abastecimiento/')).length).toBe(4);
  });
  it('CAJERO, REPARTIDOR y CLIENTE no ven ni pueden abrir Abastecimiento', () => {
    for (const rol of ['CAJERO', 'REPARTIDOR', 'CLIENTE'] as const) {
      auth.roles.set([rol]);
      const f = TestBed.createComponent(Interno);
      f.detectChanges();
      expect(
        f.nativeElement.querySelector('nav[aria-label="Navegación del portal"]').textContent,
      ).not.toContain('Abastecimiento');
      expect(
        TestBed.runInInjectionContext(() =>
          rolGuard('ADMINISTRADOR', 'ABASTECIMIENTO')({} as never, {} as never),
        ),
      ).not.toBe(true);
      f.destroy();
    }
  });
});
