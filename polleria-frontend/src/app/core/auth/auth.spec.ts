import { TestBed } from '@angular/core/testing';
import {
  HttpClient,
  HttpErrorResponse,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import {
  Router,
  provideRouter,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
} from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { AuthService, AUTH_KEYS, OAUTH_REDIRECT } from './auth-service';
import { generarCodeChallenge, generarCodeVerifier } from './pkce';
import { authGuard } from '../guards/auth-guard';
import { rolGuard } from '../guards/rol-guard';
import { authInterceptor } from '../interceptors/auth-interceptor';
import { environment } from '../../../environments/environment';
import { routes } from '../../app.routes';
import { authFixture } from '../../testing/auth-fixture';
import { CategoriaList } from '../../features/catalogo/categoria/categoria-list';
import { ClienteList } from '../../features/clientes/cliente/cliente-list';
import { By } from '@angular/platform-browser';

// Synthetic unsigned payloads are used only to test UI claims; never accepted as backend tokens.
function response(role = 'ADMINISTRADOR', seconds = 300) {
  const claims = {
    sub: 'test-sub',
    preferred_username: 'prueba',
    exp: Math.floor(Date.now() / 1000) + seconds,
    iss: environment.keycloak.issuer,
    realm_access: { roles: [role, 'offline_access'] },
  };
  return {
    access_token: 'test.' + btoa(JSON.stringify(claims)) + '.test',
    refresh_token: 'test-refresh',
    id_token: 'test-id',
    expires_in: seconds,
    refresh_expires_in: 1800,
  };
}
function restore(role = 'ADMINISTRADOR', expired = false) {
  const data = response(role, expired ? -5 : 300);
  sessionStorage.setItem(
    AUTH_KEYS.sesion,
    JSON.stringify({
      accessToken: data.access_token,
      refreshToken: data.refresh_token,
      idToken: data.id_token,
      expiraEn: Date.now() + (expired ? -10000 : 200000),
      refreshExpiraEn: Date.now() + 1000000,
    }),
  );
}

describe('OAuth AuthService', () => {
  const redirect = vi.fn();
  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [{ provide: OAUTH_REDIRECT, useValue: redirect }],
    });
  });
  afterEach(() => {
    TestBed.resetTestingModule();
    sessionStorage.clear();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    redirect.mockReset();
  });
  it('inicia anónimo y descarta copias inválidas sin afectar almacenamiento ajeno', () => {
    sessionStorage.setItem(AUTH_KEYS.sesion, 'bad json');
    sessionStorage.setItem('otra', 'conservar');
    const auth = TestBed.inject(AuthService);
    expect(auth.autenticado()).toBe(false);
    expect(auth.roles()).toEqual([]);
    expect(auth.accessToken()).toBeNull();
    expect(sessionStorage.getItem('otra')).toBe('conservar');
  });
  it('restaura identidad y solo los roles aprobados desde el JWT', () => {
    restore('CAJERO');
    const auth = TestBed.inject(AuthService);
    expect(auth.autenticado()).toBe(true);
    expect(auth.roles()).toEqual(['CAJERO']);
    expect(auth.usuario()?.sub).toBe('test-sub');
    expect(auth.tieneRol('ADMINISTRADOR', 'CAJERO')).toBe(true);
    expect(localStorage.length).toBe(0);
  });
  it('PKCE usa entropía y el vector S256 de RFC 7636', async () => {
    expect(generarCodeVerifier()).toMatch(/^[A-Za-z0-9_-]{43}$/);
    expect(generarCodeVerifier()).not.toBe(generarCodeVerifier());
    expect(await generarCodeChallenge('dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk')).toBe(
      'E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM',
    );
  });
  it('login genera state/verifier por flujo y destino interno seguro', async () => {
    const auth = TestBed.inject(AuthService);
    await auth.iniciarLogin('//evil.invalid');
    const url = new URL(redirect.mock.calls[0][0]);
    expect(url.searchParams.get('code_challenge_method')).toBe('S256');
    expect(url.searchParams.get('state')).toBe(sessionStorage.getItem(AUTH_KEYS.state));
    expect(url.searchParams.get('redirect_uri')).toBe(environment.keycloak.redirectUri);
    expect(auth.destinoPendiente()).toBe('/');
    expect(url.searchParams.has('client_secret')).toBe(false);
  });
  it('state inválido se rechaza antes del intercambio y consume el verifier', async () => {
    const fetch = vi.fn();
    vi.stubGlobal('fetch', fetch);
    sessionStorage.setItem(AUTH_KEYS.state, 'correcto');
    sessionStorage.setItem(AUTH_KEYS.verifier, 'verifier');
    await expect(TestBed.inject(AuthService).completarLogin('code', 'otro')).rejects.toThrow();
    expect(fetch).not.toHaveBeenCalled();
    expect(sessionStorage.getItem(AUTH_KEYS.verifier)).toBeNull();
  });
  it('callback intercambia code con verifier sin Bearer y almacena sesión solo en sessionStorage', async () => {
    const fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => response() });
    vi.stubGlobal('fetch', fetch);
    sessionStorage.setItem(AUTH_KEYS.state, 'state');
    sessionStorage.setItem(AUTH_KEYS.verifier, 'verifier');
    const auth = TestBed.inject(AuthService);
    await auth.completarLogin('code', 'state');
    const options = fetch.mock.calls[0][1];
    expect(options.body.get('grant_type')).toBe('authorization_code');
    expect(options.body.get('code_verifier')).toBe('verifier');
    expect(options.headers.Authorization).toBeUndefined();
    expect(auth.autenticado()).toBe(true);
    expect(sessionStorage.getItem(AUTH_KEYS.sesion)).not.toBeNull();
    expect(localStorage.length).toBe(0);
  });
  it('refresh concurrente comparte un intercambio y reemplaza la sesión', async () => {
    restore();
    let complete!: (data: object) => void;
    const fetch = vi.fn().mockReturnValue(new Promise((resolve) => (complete = resolve)));
    vi.stubGlobal('fetch', fetch);
    const auth = TestBed.inject(AuthService);
    const a = auth.refrescar();
    const b = auth.refrescar();
    expect(a).toBe(b);
    expect(fetch).toHaveBeenCalledTimes(1);
    complete({ ok: true, json: async () => response() });
    expect(await a).toBe(true);
    expect(await b).toBe(true);
    expect(fetch.mock.calls[0][1].body.get('grant_type')).toBe('refresh_token');
  });
  it('access token vencido se recupera mediante refresh al proteger una ruta', async () => {
    restore('CAJERO', true);
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: true, json: async () => response('CAJERO') }),
    );
    const auth = TestBed.inject(AuthService);
    expect(auth.autenticado()).toBe(false);
    expect(await auth.asegurarSesion()).toBe(true);
  });
  it('refresh fallido limpia memoria/copia y no devuelve una sesión vencida', async () => {
    restore();
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false }));
    const auth = TestBed.inject(AuthService);
    expect(await auth.refrescar()).toBe(false);
    expect(auth.accessToken()).toBeNull();
    expect(sessionStorage.getItem(AUTH_KEYS.sesion)).toBeNull();
  });
  it('logout invoca endpoint Keycloak con id_token_hint y limpia solo sus claves', () => {
    restore();
    sessionStorage.setItem('otra', 'conservar');
    const auth = TestBed.inject(AuthService);
    auth.cerrarSesion();
    const url = new URL(redirect.mock.calls[0][0]);
    expect(url.pathname).toContain('/protocol/openid-connect/logout');
    expect(url.searchParams.has('id_token_hint')).toBe(true);
    expect(auth.accessToken()).toBeNull();
    expect(sessionStorage.getItem('otra')).toBe('conservar');
  });
});

describe('guards e interceptor OAuth', () => {
  let auth: ReturnType<typeof authFixture>;
  let http: HttpTestingController;
  beforeEach(() => {
    auth = authFixture();
    vi.spyOn(auth, 'accessToken').mockReturnValue('token-a');
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
      ],
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
  it('guard solicita login para sesión ausente', async () => {
    vi.spyOn(auth, 'asegurarSesion').mockResolvedValue(false);
    const login = vi.spyOn(auth, 'iniciarLogin');
    const result = await TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/interno' } as RouterStateSnapshot),
    );
    expect(result).toBe(false);
    expect(login).toHaveBeenCalledWith('/interno');
  });
  it('portal admite administrador y rechaza CLIENTE con sus guards reales', async () => {
    const harness = await RouterTestingHarness.create('/interno');
    expect(harness.routeNativeElement?.textContent).toContain('Portal interno');
    auth.roles.set(['CLIENTE']);
    await harness.navigateByUrl('/');
    await harness.navigateByUrl('/interno');
    expect(TestBed.inject(Router).url).toBe('/sin-permiso');
  });
  it('guard de rol impide edición de catálogo a CAJERO', () => {
    auth.roles.set(['CAJERO']);
    const result = TestBed.runInInjectionContext(() =>
      rolGuard('ADMINISTRADOR')({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
    expect(String(result)).toBe('/sin-permiso');
  });
  it('Bearer solo se añade a la API, nunca terceros ni Keycloak', () => {
    const client = TestBed.inject(HttpClient);
    for (const url of [
      environment.apiBaseUrl + '/api/v1/clientes',
      'https://example.com/api/data',
      environment.apiBaseUrl + '.evil.invalid/api/v1/pedidos',
      environment.keycloak.issuer + '/protocol/openid-connect/token',
    ]) {
      client.get(url).subscribe();
      const req = http.expectOne(url);
      expect(req.request.headers.has('Authorization')).toBe(
        url === environment.apiBaseUrl + '/api/v1/clientes',
      );
      req.flush([]);
    }
  });
  it('403 no renueva ni cierra sesión; navega a acceso denegado', () => {
    const refresh = vi.spyOn(auth, 'refrescar');
    const clear = vi.spyOn(auth, 'descartarSesion');
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    TestBed.inject(HttpClient)
      .get(environment.apiBaseUrl + '/api/v1/clientes')
      .subscribe({ error: () => {} });
    http
      .expectOne(environment.apiBaseUrl + '/api/v1/clientes')
      .flush({}, { status: 403, statusText: 'Forbidden' });
    expect(refresh).not.toHaveBeenCalled();
    expect(clear).not.toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith('/sin-permiso');
  });
  it('401 renueva y reintenta solo una vez; segundo 401 solicita login', async () => {
    const refresh = vi.spyOn(auth, 'refrescar').mockImplementation(async () => {
      vi.mocked(auth.accessToken).mockReturnValue('token-b');
      return true;
    });
    const login = vi.spyOn(auth, 'iniciarLogin');
    const url = environment.apiBaseUrl + '/api/v1/clientes';
    TestBed.inject(HttpClient)
      .get(url)
      .subscribe({ error: () => {} });
    http.expectOne(url).flush({}, { status: 401, statusText: 'Unauthorized' });
    await Promise.resolve();
    await Promise.resolve();
    const retry = http.expectOne(url);
    expect(retry.request.headers.get('Authorization')).toBe('Bearer token-b');
    retry.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(refresh).toHaveBeenCalledTimes(1);
    expect(login).toHaveBeenCalledTimes(1);
  });
  it('CAJERO tiene lectura de catálogo sin acciones administrativas y Clientes sin eliminar', async () => {
    auth.roles.set(['CAJERO']);
    const harness = await RouterTestingHarness.create('/interno/categorias');
    http
      .expectOne(environment.apiBaseUrl + '/api/v1/categorias')
      .flush([{ id: 24, nombre: 'Pollos', descripcion: null, activo: true }]);
    await harness.fixture.whenStable();
    expect(harness.routeNativeElement?.querySelector('app-categoria-list a')).toBeNull();
    expect(harness.fixture.debugElement.query(By.directive(CategoriaList))).toBeTruthy();
    await harness.navigateByUrl('/interno/clientes');
    http
      .expectOne(environment.apiBaseUrl + '/api/v1/clientes')
      .flush([
        {
          id: 172,
          nombres: 'Ana',
          apellidos: 'Prueba',
          telefono: '999000009',
          correo: null,
          activo: true,
        },
      ]);
    await harness.fixture.whenStable();
    const list = harness.fixture.debugElement.query(By.directive(ClienteList))
      .nativeElement as HTMLElement;
    expect(list.textContent).toContain('Nuevo cliente');
    expect(list.textContent).toContain('Editar');
    expect(list.textContent).not.toContain('Eliminar');
  });
});
