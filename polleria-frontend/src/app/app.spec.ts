import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { App } from './app';
import { routes } from './app.routes';
import { traceIdInterceptor } from './core/interceptors/trace-id-interceptor';
import { environment } from '../environments/environment';

describe('Arquitectura SPA', () => {
  beforeEach(() => TestBed.configureTestingModule({
    imports: [App],
    providers: [provideRouter(routes), provideHttpClient(withInterceptors([traceIdInterceptor])), provideHttpClientTesting()],
  }));

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('crea el componente raíz con el outlet superior', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
    expect(fixture.nativeElement.querySelector('router-outlet')).not.toBeNull();
  });

  it('navega entre layouts y renderiza las rutas hijas', async () => {
    const harness = await RouterTestingHarness.create('/');
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toContain('Bienvenido a Pollería');
    expect(harness.routeNativeElement?.querySelector('header .brand')?.textContent).toBe('Pollería');
    await harness.navigateByUrl('/interno');
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe('Portal interno');
    expect(harness.routeNativeElement?.querySelector('header .brand')?.textContent).toBe('Sistema Pollería');
    await harness.navigateByUrl('/interno/conexion');
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe('Conexión con el servidor');
    await harness.navigateByUrl('/');
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toContain('Bienvenido');
  });

  it('consulta el backend usando la URL central y conserva el Trace ID', async () => {
    const harness = await RouterTestingHarness.create('/interno/conexion');
    harness.routeNativeElement?.querySelector('button')?.click();
    const req = TestBed.inject(HttpTestingController).expectOne(`${environment.apiBaseUrl}/api/v1/categorias`);
    expect(req.request.method).toBe('GET');
    const traceId = req.request.headers.get('X-Trace-ID');
    expect(traceId).toMatch(/^[a-f0-9-]{36}$/);
    req.flush([], { headers: { 'X-Trace-ID': traceId! } });
    await harness.fixture.whenStable();
    expect(harness.routeNativeElement?.textContent).toContain('Conexión correcta · HTTP 200');
    expect(harness.routeNativeElement?.textContent).toContain(traceId);
  });

  it('muestra un fallo de conexión sin romper el layout', async () => {
    const harness = await RouterTestingHarness.create('/interno/conexion');
    harness.routeNativeElement?.querySelector('button')?.click();
    TestBed.inject(HttpTestingController).expectOne(`${environment.apiBaseUrl}/api/v1/categorias`)
      .flush({}, { status: 503, statusText: 'Service Unavailable' });
    await harness.fixture.whenStable();
    expect(harness.routeNativeElement?.querySelector('[role="alert"]')?.textContent).toContain('No se pudo conectar');
    expect(harness.routeNativeElement?.querySelector('header')).not.toBeNull();
  });
});
