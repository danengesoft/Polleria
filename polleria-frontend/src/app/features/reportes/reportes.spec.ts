import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
  TestRequest,
} from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { Reportes } from './reportes';
import { routes } from '../../app.routes';
import { AuthService } from '../../core/auth/auth-service';
import { authFixture } from '../../testing/auth-fixture';
import { Interno } from '../../core/layout/interno/interno';

const ventas = { cantidadPedidos: 2, totalMonetario: 40, ticketPromedio: 20 };
const compras = { cantidadCompras: 1, totalComprado: 43.99, compraPromedio: 43.99 };
describe('Reportes administrativos', () => {
  let http: HttpTestingController;
  let auth: ReturnType<typeof authFixture>;
  beforeEach(() => {
    auth = authFixture();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter(routes),
        { provide: AuthService, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  function preparar() {
    const f = TestBed.createComponent(Reportes);
    f.detectChanges();
    return f;
  }
  function responder(req: TestRequest, empty = false) {
    const url = req.request.url;
    if (url.endsWith('pedidos/reportes/resumen'))
      req.flush(empty ? { cantidadPedidos: 0, totalMonetario: 0, ticketPromedio: 0 } : ventas);
    else if (url.endsWith('resumen-compras'))
      req.flush(empty ? { cantidadCompras: 0, totalComprado: 0, compraPromedio: 0 } : compras);
    else if (url.endsWith('delivery/reportes/resumen'))
      req.flush({
        entregasAbiertas: [{ estado: 'ASIGNADO', cantidadEntregas: 0 }],
        repartidoresActivos: 2,
      });
    else req.flush([]);
  }
  function completar(empty = false) {
    const reqs = http.match(() => true);
    reqs.forEach((r) => responder(r, empty));
    return reqs;
  }
  it('compone los tres módulos con importes monetarios y carga independiente', () => {
    const f = preparar();
    expect(f.componentInstance.ventas().cargando).toBe(true);
    const reqs = completar();
    expect(reqs.length).toBe(14);
    f.detectChanges();
    expect(f.nativeElement.textContent.replace(/\s+/g, ' ')).toContain('S/ 43.99');
    expect(f.nativeElement.textContent.replace(/\s+/g, ' ')).toContain('S/ 40.00');
    expect(f.componentInstance.operacion().filas).toContainEqual(['Repartidores activos', 2]);
  });
  it('envía días ISO y top solo a rankings; stock y operación no reciben fechas', () => {
    const f = preparar();
    completar();
    const c = f.componentInstance;
    c.fechaDesde = '2026-10-25';
    c.fechaHasta = '2026-10-25';
    c.top = 5;
    c.aplicar();
    const reqs = http.match(() => true);
    for (const r of reqs) {
      if (
        r.request.url.endsWith('stock-actual') ||
        r.request.url.endsWith('delivery/reportes/resumen')
      )
        expect(r.request.params.keys()).toEqual([]);
      else {
        expect(r.request.params.get('fechaDesde')).toBe('2026-10-25');
        expect(r.request.params.get('fechaHasta')).toBe('2026-10-25');
      }
      if (r.request.url.endsWith('/clientes') || r.request.url.endsWith('productos-mas-vendidos'))
        expect(r.request.params.get('top')).toBe('5');
      responder(r);
    }
    expect(c.rangoAplicado()).toContain('2026-10-25');
  });
  it('rango invertido y top inválido no disparan solicitudes', () => {
    const f = preparar();
    completar();
    const c = f.componentInstance;
    c.fechaDesde = '2026-10-25';
    c.fechaHasta = '2026-10-24';
    c.aplicar();
    expect(c.errorFiltro()).toContain('posterior');
    http.expectNone(() => true);
    c.fechaHasta = '2026-10-25';
    c.top = 101;
    c.aplicar();
    expect(c.errorFiltro()).toContain('100');
    http.expectNone(() => true);
  });
  it('maneja ceros y listas vacías sin NaN ni valores indefinidos', () => {
    const f = preparar();
    completar(true);
    f.detectChanges();
    const text = f.nativeElement.textContent.replace(/\s+/g, ' ');
    expect(text).toContain('Sin datos para el rango seleccionado');
    expect(text).toContain('S/ 0.00');
    expect(text).not.toMatch(/NaN|undefined|Infinity/);
  });
  it('falla solo la subsección afectada y conserva ventas y abastecimiento', () => {
    const f = preparar();
    const reqs = http.match(() => true);
    for (const r of reqs) {
      if (r.request.url.endsWith('delivery/reportes/por-estado'))
        r.flush({}, { status: 500, statusText: 'Error' });
      else responder(r);
    }
    f.detectChanges();
    expect(f.componentInstance.entregas().error).not.toBe('');
    expect(f.componentInstance.ventas().filas).toContainEqual(['Resumen', 2, 40, 20]);
    expect(f.nativeElement.textContent.replace(/\s+/g, ' ')).toContain('S/ 43.99');
  });
  it('renderiza líneas históricas, cantidades físicas y estados sin cálculos monetarios', () => {
    const f = preparar();
    const reqs = http.match(() => true);
    for (const r of reqs) {
      if (r.request.url.endsWith('insumos-comprados'))
        r.flush([
          {
            insumoId: 84,
            nombreInsumo: 'Aceite histórico',
            unidadMedida: 'L',
            cantidadComprada: 2.5,
            importeComprado: 30.86,
          },
        ]);
      else if (r.request.url.endsWith('ventas-por-dia'))
        r.flush([{ fecha: '2026-10-25', cantidadVentas: 2, totalVentas: 40 }]);
      else if (r.request.url.endsWith('delivery/reportes/por-estado'))
        r.flush([{ estado: 'ENTREGADO', cantidadEntregas: 2 }]);
      else responder(r);
    }
    f.detectChanges();
    const text = f.nativeElement.textContent.replace(/\s+/g, ' ');
    expect(text).toContain('Aceite histórico (#84)');
    expect(text).toContain('S/ 30.86');
    expect(text).toContain('2026-10-25');
    expect(text).toContain('ENTREGADO');
  });
  it('cancela respuestas anteriores al reaplicar filtros', () => {
    const f = preparar();
    const old = http.match(() => true);
    f.componentInstance.fechaDesde = '2026-10-25';
    f.componentInstance.aplicar();
    expect(old.every((r) => r.cancelled)).toBe(true);
    completar();
  });
  it('ruta y enlace Reportes exclusivos para ADMINISTRADOR', () => {
    const route = routes
      .find((r) => r.path === 'interno')!
      .children!.find((r) => r.path === 'reportes')!;
    expect(route).toBeDefined();
    expect(route.loadComponent).toBeDefined();
    expect(TestBed.runInInjectionContext(() => (route.canActivate![0] as Function)({}, {}))).toBe(
      true,
    );
    const f = TestBed.createComponent(Interno);
    f.detectChanges();
    expect(f.nativeElement.querySelector('a[href="/interno/reportes"]')).not.toBeNull();
  });
  it('todos los roles no autorizados carecen de navegación y guard permite solo admin', () => {
    const route = routes
      .find((r) => r.path === 'interno')!
      .children!.find((r) => r.path === 'reportes')!;
    for (const rol of ['CAJERO', 'ABASTECIMIENTO', 'REPARTIDOR', 'CLIENTE'] as const) {
      auth.roles.set([rol]);
      const f = TestBed.createComponent(Interno);
      f.detectChanges();
      expect(f.nativeElement.querySelector('a[href="/interno/reportes"]')).toBeNull();
      const result = TestBed.runInInjectionContext(() =>
        (route.canActivate![0] as Function)({}, {}),
      );
      expect(TestBed.inject(Router).serializeUrl(result)).toBe('/sin-permiso');
      f.destroy();
    }
  });
});
