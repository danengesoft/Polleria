import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { AppNavigation } from './navigation/app-navigation';
import { Interno } from '../core/layout/interno/interno';
import { SeguimientoPedido } from './seguimiento-pedido';
import { AuthService } from '../core/auth/auth-service';
import { authFixture } from '../testing/auth-fixture';
import { Carrito } from '../features/publico/carrito';
import { CarritoService } from '../features/publico/carrito-service';
import { CatalogoPublico } from '../features/publico/catalogo-publico';
const producto = {
  id: 10,
  nombre: 'Pollo',
  descripcion: null,
  precio: 20,
  disponible: true,
  imagenUrl: 'https://example.com/producto.jpg',
  categoria: { id: 1, nombre: 'Pollos' },
};
describe('Interacciones de presentación', () => {
  let auth: ReturnType<typeof authFixture>;
  beforeEach(() => {
    auth = authFixture();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
      ],
    });
  });
  it('menú principal publica expansión y cierra con Escape', () => {
    const f = TestBed.createComponent(AppNavigation);
    f.detectChanges();
    const button = f.nativeElement.querySelector('.menu-toggle');
    Object.defineProperty(button, 'offsetWidth', { value: 44 });
    expect(button.getAttribute('aria-expanded')).toBe('false');
    button.click();
    f.detectChanges();
    expect(button.getAttribute('aria-expanded')).toBe('true');
    expect(f.nativeElement.querySelector('.nav-extra').classList.contains('abierto')).toBe(true);
    f.nativeElement
      .querySelector('nav')
      .dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    f.detectChanges();
    expect(button.getAttribute('aria-expanded')).toBe('false');
    expect(document.activeElement).toBe(button);
  });
  it('menú del portal cierra al seleccionar enlace sin cambiar permisos', async () => {
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const f = TestBed.createComponent(Interno);
    f.detectChanges();
    const b = f.nativeElement.querySelector('.portal-toggle');
    b.click();
    f.detectChanges();
    expect(b.getAttribute('aria-expanded')).toBe('true');
    f.nativeElement.querySelector('nav[aria-label="Navegación del portal"] a').click();
    await f.whenStable();
    f.detectChanges();
    expect(b.getAttribute('aria-expanded')).toBe('false');
    expect(f.nativeElement.querySelector('a[href="/interno/reportes"]')).not.toBeNull();
  });
  it('menú colapsado conserva navegación exclusiva de reparto', () => {
    auth.roles.set(['REPARTIDOR']);
    const f = TestBed.createComponent(Interno);
    f.detectChanges();
    f.nativeElement.querySelector('.portal-toggle').click();
    f.detectChanges();
    const nav = f.nativeElement.querySelector('nav[aria-label="Navegación del portal"]');
    expect(nav.textContent).toContain('Mis entregas');
    expect(nav.textContent).not.toMatch(/Reportes|Clientes|Abastecimiento/);
  });
  it('seguimiento distingue Delivery de Recojo con un solo estado actual', () => {
    const f = TestBed.createComponent(SeguimientoPedido);
    f.componentRef.setInput('estado', 'LISTO');
    f.componentRef.setInput('tipo', 'RECOJO');
    f.detectChanges();
    expect(f.nativeElement.textContent).not.toContain('Despachado');
    expect(f.nativeElement.querySelectorAll('[aria-current="step"]').length).toBe(1);
    f.componentRef.setInput('tipo', 'DELIVERY');
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Despachado');
  });
  it('cancelación no muestra una progresión falsa', () => {
    const f = TestBed.createComponent(SeguimientoPedido);
    f.componentRef.setInput('estado', 'CANCELADO');
    f.componentRef.setInput('tipo', 'DELIVERY');
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Cancelado');
    expect(f.nativeElement.querySelector('ol')).toBeNull();
  });
  it('vaciar carrito requiere confirmación y conserva selección al rechazar', () => {
    let confirmar = false;
    TestBed.overrideProvider(MatDialog, {
      useValue: { open: () => ({ afterClosed: () => of(confirmar) }) },
    });
    const f = TestBed.createComponent(Carrito);
    const carrito = TestBed.inject(CarritoService);
    carrito.vaciar();
    carrito.agregar(producto);
    f.componentInstance.confirmarVaciado();
    expect(carrito.unidades()).toBe(1);
    confirmar = true;
    f.componentInstance.confirmarVaciado();
    expect(carrito.unidades()).toBe(0);
  });
  it('imagen fallida se sustituye por placeholder sin manipulación directa del DOM', () => {
    const f = TestBed.createComponent(CatalogoPublico);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('http://localhost:8080/api/v1/categorias').flush([]);
    http.expectOne('http://localhost:8080/api/v1/productos').flush([producto]);
    f.detectChanges();
    f.nativeElement.querySelector('img').dispatchEvent(new Event('error'));
    f.detectChanges();
    expect(f.nativeElement.querySelector('img')).toBeNull();
    expect(f.nativeElement.querySelector('.placeholder')).not.toBeNull();
    http.verify();
  });
});
