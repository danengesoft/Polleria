import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { CarritoService } from './carrito-service';
import { Checkout } from './checkout';
import { Perfil } from './perfil';
import { MisPedidos } from './mis-pedidos';
import { MiPedido } from './mi-pedido';
import { Producto } from '../catalogo/producto/producto.model';
import { AuthService } from '../../core/auth/auth-service';
import { authFixture } from '../../testing/auth-fixture';
import { authGuard } from '../../core/guards/auth-guard';
import { rolGuard } from '../../core/guards/rol-guard';
import { routes } from '../../app.routes';
const producto: Producto = {
  id: 10,
  nombre: '1/4 Pollo',
  precio: 20,
  disponible: true,
  descripcion: null,
  imagenUrl: null,
  categoria: { id: 24, nombre: 'Pollos' },
};
const perfil = {
  id: 1,
  nombres: 'Ana',
  apellidos: 'Prueba',
  telefono: '912345678',
  correo: null,
  direccionPrincipal: 'Principal',
  referencia: null,
  activo: true,
};
const pedido = {
  id: 99,
  cliente: { id: 1, nombres: 'Ana', apellidos: 'Prueba' },
  fecha: '2026-10-24T12:00:00',
  tipoPedido: 'RECOJO',
  estado: 'PENDIENTE',
  estadoPago: 'PENDIENTE',
  metodoPago: 'EFECTIVO',
  total: 20,
  detalles: [
    {
      productoId: 10,
      nombreProducto: 'Pollo histórico',
      cantidad: 1,
      precioUnitario: 20,
      subtotal: 20,
    },
  ],
};
describe('Experiencia pública', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: new Map([['id', '99']]), queryParamMap: new Map() } },
        },
        { provide: MatDialog, useValue: { open: vi.fn(() => ({ afterClosed: () => of(true) })) } },
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authFixture(['CLIENTE']) },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('agrega sin login, acumula por ID y modifica cantidades enteras', () => {
    const c = TestBed.inject(CarritoService);
    c.agregar(producto);
    c.agregar(producto);
    expect(c.lineas().length).toBe(1);
    expect(c.unidades()).toBe(2);
    expect(c.total()).toBe(40);
    c.cantidad(10, 1);
    c.cantidad(10, 0);
    c.cantidad(10, -1);
    c.cantidad(10, 1.5);
    expect(c.unidades()).toBe(1);
    c.eliminar(10);
    expect(c.lineas()).toEqual([]);
  });
  it('restaura carrito tras recarga sin mezclar claves auth', () => {
    const c = TestBed.inject(CarritoService);
    sessionStorage.setItem('polleria_sesion', 'separada');
    c.agregar(producto);
    const restaurado = new CarritoService();
    expect(restaurado.total()).toBe(20);
    restaurado.vaciar();
    expect(sessionStorage.getItem('polleria_sesion')).toBe('separada');
  });
  it('no agrega producto no disponible ni restaura cantidades corruptas', () => {
    sessionStorage.setItem(
      'polleria_carrito',
      JSON.stringify([{ productoId: 1, nombre: 'x', precio: 20, cantidad: 0 }]),
    );
    const c = new CarritoService();
    c.agregar({ ...producto, disponible: false });
    expect(c.lineas()).toEqual([]);
  });
  it('checkout usa guards existentes y exige CLIENTE', () => {
    const publics = routes.find((r) => r.path === '')!.children!;
    for (const path of ['checkout', 'perfil', 'mis-pedidos', 'mis-pedidos/:id'])
      expect(publics.find((r) => r.path === path)!.canActivate?.[0]).toBe(authGuard);
    expect(TestBed.runInInjectionContext(() => rolGuard('CLIENTE')({} as never, {} as never))).toBe(
      true,
    );
    const fixture = authFixture(['CAJERO']);
    TestBed.inject(AuthService).tieneRol = fixture.tieneRol;
    expect(
      TestBed.runInInjectionContext(() => rolGuard('CLIENTE')({} as never, {} as never)),
    ).not.toBe(true);
  });
  function checkout() {
    const f = TestBed.createComponent(Checkout);
    http.expectOne('http://localhost:8080/api/v1/clientes/me').flush(perfil);
    TestBed.inject(CarritoService).agregar(producto);
    return f.componentInstance;
  }
  it('checkout sin perfil guía a completarlo', () => {
    const nav = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const f = TestBed.createComponent(Checkout);
    http
      .expectOne('http://localhost:8080/api/v1/clientes/me')
      .flush({ message: 'Completa tu perfil' }, { status: 404, statusText: 'Not found' });
    expect(nav).toHaveBeenCalledWith(['/perfil'], { queryParams: { continuar: 'checkout' } });
    expect(f.componentInstance.perfilListo()).toBe(false);
  });
  it('delivery exige dirección y conserva carrito ante error comercial', () => {
    const c = checkout();
    c.form.patchValue({ tipoPedido: 'DELIVERY', direccionEntrega: '' });
    c.confirmar();
    expect(c.error()).toContain('dirección');
    http.expectNone('http://localhost:8080/api/v1/pedidos/mios');
    c.form.patchValue({ direccionEntrega: 'Efectiva' });
    c.confirmar();
    http
      .expectOne('http://localhost:8080/api/v1/pedidos/mios')
      .flush({ message: 'Producto no disponible' }, { status: 409, statusText: 'Conflict' });
    expect(c.error()).toBe('Producto no disponible');
    expect(c.carrito.unidades()).toBe(1);
  });
  it('envía solo contrato público y limpia carrito después del éxito', () => {
    const nav = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const c = checkout();
    c.confirmar();
    const req = http.expectOne('http://localhost:8080/api/v1/pedidos/mios');
    expect(req.request.body).toEqual({
      tipoPedido: 'RECOJO',
      metodoPago: 'EFECTIVO',
      direccionEntrega: null,
      referenciaEntrega: null,
      detalles: [{ productoId: 10, cantidad: 1 }],
    });
    expect(c.carrito.unidades()).toBe(1);
    req.flush(pedido);
    expect(c.carrito.unidades()).toBe(0);
    expect(nav).toHaveBeenCalledWith(['/mis-pedidos', 99], { state: { pedidoCreado: true } });
  });
  it('perfil crea campos comerciales sin sub rol o id', () => {
    const f = TestBed.createComponent(Perfil);
    http
      .expectOne('http://localhost:8080/api/v1/clientes/me')
      .flush({}, { status: 404, statusText: 'Not found' });
    f.componentInstance.form.patchValue({
      nombres: 'Ana',
      apellidos: 'Prueba',
      telefono: '912345678',
    });
    f.componentInstance.guardar();
    const r = http.expectOne('http://localhost:8080/api/v1/clientes/me');
    expect(r.request.method).toBe('POST');
    expect(Object.keys(r.request.body).sort()).toEqual(
      ['nombres', 'apellidos', 'telefono', 'correo', 'direccionPrincipal', 'referencia'].sort(),
    );
    r.flush(perfil);
    expect(f.componentInstance.existe()).toBe(true);
  });
  it('perfil actualiza mediante PUT y comunica correo duplicado', () => {
    const f = TestBed.createComponent(Perfil);
    http.expectOne('http://localhost:8080/api/v1/clientes/me').flush(perfil);
    f.componentInstance.guardar();
    const r = http.expectOne('http://localhost:8080/api/v1/clientes/me');
    expect(r.request.method).toBe('PUT');
    r.flush(
      { message: 'Ya existe un cliente con ese correo' },
      { status: 409, statusText: 'Conflict' },
    );
    expect(f.componentInstance.error()).toContain('correo');
  });
  it('Mis pedidos consulta solo endpoint propio', () => {
    const f = TestBed.createComponent(MisPedidos);
    http.expectOne('http://localhost:8080/api/v1/pedidos/mios').flush([pedido]);
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Pedido #99');
  });
  it('detalle usa precio histórico y confirma cancelación', () => {
    const f = TestBed.createComponent(MiPedido);
    http.expectOne('http://localhost:8080/api/v1/pedidos/mios/99').flush(pedido);
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Pollo histórico');
    expect(f.nativeElement.textContent).toContain('20.00');
    f.componentInstance.cancelar();
    http
      .expectOne('http://localhost:8080/api/v1/pedidos/mios/99/cancelar')
      .flush({ ...pedido, estado: 'CANCELADO' });
    f.detectChanges();
    expect(f.nativeElement.textContent).not.toContain('Cancelar pedido');
  });
  it('detalle confirmado no inicia cancelación', () => {
    const f = TestBed.createComponent(MiPedido);
    http
      .expectOne('http://localhost:8080/api/v1/pedidos/mios/99')
      .flush({ ...pedido, estado: 'CONFIRMADO' });
    f.componentInstance.cancelar();
    http.expectNone('http://localhost:8080/api/v1/pedidos/mios/99/cancelar');
  });
});
