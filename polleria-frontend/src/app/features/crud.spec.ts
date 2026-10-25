import { By } from '@angular/platform-browser';
import { Type } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideHttpClient, HttpErrorResponse } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { routes } from '../app.routes';
import { environment } from '../../environments/environment';
import { CategoriaService } from './catalogo/categoria/categoria-service';
import { ProductoService } from './catalogo/producto/producto-service';
import { ClienteService } from './clientes/cliente/cliente-service';
import { CategoriaForm } from './catalogo/categoria/categoria-form';
import { ProductoForm } from './catalogo/producto/producto-form';
import { ClienteForm } from './clientes/cliente/cliente-form';
import { ProductoList } from './catalogo/producto/producto-list';
import { mensajeHttp } from '../shared/http-error';

async function navegar<T>(
  harness: RouterTestingHarness,
  url: string,
  component: Type<T>,
): Promise<T> {
  await harness.navigateByUrl(url);
  return harness.fixture.debugElement.query(By.directive(component)).componentInstance as T;
}

const base = environment.apiBaseUrl + '/api/v1';
const categoria = { id: 24, nombre: 'Pollos', descripcion: null, activo: true };
const producto = {
  id: 10,
  nombre: '1/4 Pollo',
  descripcion: null,
  precio: 20,
  disponible: true,
  categoria: { id: 24, nombre: 'Pollos' },
  imagenUrl: null,
};
const cliente = {
  id: 1,
  nombres: 'Ana',
  apellidos: 'Prueba',
  telefono: '+51987654321',
  correo: null,
  direccionPrincipal: 'Dirección',
  referencia: 'Referencia',
  activo: true,
};

describe('CRUD conectados al contrato HTTP', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
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

  it('CategoriaService ejecuta los cinco métodos con URL central y contratos correctos', () => {
    const service = TestBed.inject(CategoriaService);
    service.listar().subscribe();
    const list = http.expectOne(`${base}/categorias`);
    expect(list.request.method).toBe('GET');
    list.flush([]);
    service.obtener(24).subscribe();
    http.expectOne(`${base}/categorias/24`).flush(categoria);
    const body = { nombre: 'Pollos', descripcion: null, activo: true };
    service.crear(body).subscribe();
    const post = http.expectOne(`${base}/categorias`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual(body);
    post.flush(categoria);
    service.actualizar(24, body).subscribe();
    const put = http.expectOne(`${base}/categorias/24`);
    expect(put.request.method).toBe('PUT');
    put.flush(categoria);
    service.eliminar(24).subscribe();
    const del = http.expectOne(`${base}/categorias/24`);
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
  });

  it('ProductoService usa categoriaId únicamente para filtro válido y nunca envía entidad Categoria', () => {
    const service = TestBed.inject(ProductoService);
    service.listar().subscribe();
    const all = http.expectOne(`${base}/productos`);
    expect(all.request.params.has('categoriaId')).toBe(false);
    all.flush([]);
    service.listar(24).subscribe();
    http.expectOne(`${base}/productos?categoriaId=24`).flush([producto]);
    const body = {
      nombre: 'Pollo',
      descripcion: null,
      precio: 20,
      disponible: true,
      categoriaId: 24,
      imagenUrl: null,
    };
    service.crear(body).subscribe();
    const post = http.expectOne(`${base}/productos`);
    expect(post.request.body).toEqual(body);
    expect(post.request.body.categoria).toBeUndefined();
    post.flush(producto);
    service.obtener(10).subscribe();
    http.expectOne(`${base}/productos/10`).flush(producto);
    service.actualizar(10, body).subscribe();
    const put = http.expectOne(`${base}/productos/10`);
    expect(put.request.method).toBe('PUT');
    put.flush(producto);
    service.eliminar(10).subscribe();
    const del = http.expectOne(`${base}/productos/10`);
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
  });

  it('ClienteService conserva teléfono como cadena y campos opcionales nulos', () => {
    const service = TestBed.inject(ClienteService);
    service.listar().subscribe();
    http.expectOne(`${base}/clientes`).flush([]);
    service.obtener(1).subscribe();
    http.expectOne(`${base}/clientes/1`).flush(cliente);
    const { id, ...body } = cliente;
    service.crear(body).subscribe();
    const post = http.expectOne(`${base}/clientes`);
    expect(post.request.body.telefono).toBe('+51987654321');
    expect(post.request.body.correo).toBeNull();
    post.flush(cliente);
    service.actualizar(id, body).subscribe();
    const put = http.expectOne(`${base}/clientes/1`);
    expect(put.request.method).toBe('PUT');
    put.flush(cliente);
    service.eliminar(id).subscribe();
    const del = http.expectOne(`${base}/clientes/1`);
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
  });

  it('categoría rechaza espacios y muestra el conflicto devuelto al guardar', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await navegar(harness, '/interno/categorias/nuevo', CategoriaForm);
    component.form.controls.nombre.setValue('   ');
    component.guardar();
    http.expectNone(`${base}/categorias`);
    expect(component.form.controls.nombre.hasError('required')).toBe(true);
    component.form.controls.nombre.setValue('Pollos');
    component.guardar();
    expect(component.guardando()).toBe(true);
    expect(component.form.disabled).toBe(true);
    http
      .expectOne(`${base}/categorias`)
      .flush(
        { message: 'Ya existe una categoría con ese nombre' },
        { status: 409, statusText: 'Conflict' },
      );
    expect(component.error()).toContain('Ya existe');
    expect(component.guardando()).toBe(false);
    expect(component.form.enabled).toBe(true);
  });

  it('lista muestra los productos relacionados y filtra en el servidor', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await navegar(harness, '/interno/productos', ProductoList);
    http.expectOne(`${base}/categorias`).flush([categoria]);
    http.expectOne(`${base}/productos`).flush([producto]);
    await harness.fixture.whenStable();
    expect(harness.routeNativeElement?.textContent).toContain('Pollos');
    expect(harness.routeNativeElement?.textContent).toContain('S/ 20.00');
    component.filtrar(24);
    http.expectOne(`${base}/productos?categoriaId=24`).flush([producto]);
    component.filtrar(null);
    http.expectOne(`${base}/productos`).flush([producto]);
  });

  it('edición de producto preselecciona categoría numérica y preserva opcionales', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await navegar(harness, '/interno/productos/10/editar', ProductoForm);
    http
      .expectOne(`${base}/productos/10`)
      .flush({
        ...producto,
        descripcion: 'Descripción',
        imagenUrl: 'https://example.com/pollo.jpg',
      });
    http.expectOne(`${base}/categorias`).flush([categoria]);
    expect(component.form.controls.categoriaId.value).toBe(24);
    component.form.controls.disponible.setValue(false);
    component.guardar();
    const put = http.expectOne(`${base}/productos/10`);
    expect(put.request.body).toEqual({
      nombre: '1/4 Pollo',
      descripcion: 'Descripción',
      precio: 20,
      disponible: false,
      categoriaId: 24,
      imagenUrl: 'https://example.com/pollo.jpg',
    });
    put.flush({}, { status: 409, statusText: 'Conflict' });
  });

  it('producto impide precio inválido y categoría sin elegir', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await navegar(harness, '/interno/productos/nuevo', ProductoForm);
    http.expectOne(`${base}/categorias`).flush([categoria]);
    component.form.controls.nombre.setValue('Prueba');
    for (const value of ['0', '-1', '1.001', '100000000', 'NaN']) {
      component.form.controls.precio.setValue(value);
      expect(component.form.controls.precio.invalid).toBe(true);
    }
    component.form.controls.precio.setValue('20.10');
    expect(component.form.controls.precio.valid).toBe(true);
    component.guardar();
    http.expectNone(`${base}/productos`);
    expect(component.form.controls.categoriaId.invalid).toBe(true);
  });

  it('producto sin categorías impide guardar y ofrece crear una', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await navegar(harness, '/interno/productos/nuevo', ProductoForm);
    http.expectOne(`${base}/categorias`).flush([]);
    await harness.fixture.whenStable();
    expect(harness.routeNativeElement?.textContent).toContain('No hay categorías disponibles');
    component.guardar();
    http.expectNone(`${base}/productos`);
  });

  it('categoría desaparecida muestra error 404 y recarga las opciones', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await navegar(harness, '/interno/productos/nuevo', ProductoForm);
    http.expectOne(`${base}/categorias`).flush([categoria]);
    component.form.patchValue({ nombre: 'Pollo', precio: '20', categoriaId: 24 });
    component.guardar();
    http
      .expectOne(`${base}/productos`)
      .flush({ message: 'Categoría no encontrada' }, { status: 404, statusText: 'Not Found' });
    expect(component.error()).toBe('Categoría no encontrada');
    http.expectOne(`${base}/categorias`).flush([]);
    expect(component.form.controls.categoriaId.value).toBe(0);
  });

  it('cliente acepta exactamente 7–9 dígitos con +51 opcional y correo opcional', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await navegar(harness, '/interno/clientes/nuevo', ClienteForm);
    component.form.patchValue({ nombres: 'Ana', apellidos: 'Prueba' });
    for (const phone of ['1234567', '12345678', '987654321', '+511234567', '+51987654321']) {
      component.form.controls.telefono.setValue(phone);
      expect(component.form.controls.telefono.valid).toBe(true);
    }
    for (const phone of ['123456', '1234567890', '+52' + '987654321', '987 654321', 'abcdefgh']) {
      component.form.controls.telefono.setValue(phone);
      expect(component.form.controls.telefono.invalid).toBe(true);
    }
    component.form.controls.telefono.setValue('987654321');
    expect(component.form.valid).toBe(true);
    component.form.controls.correo.setValue('correo-invalido');
    expect(component.form.controls.correo.invalid).toBe(true);
    component.form.controls.correo.setValue('');
    component.guardar();
    const post = http.expectOne(`${base}/clientes`);
    expect(post.request.body.correo).toBeNull();
    post.flush({ message: 'Conflicto de prueba' }, { status: 409, statusText: 'Conflict' });
  });

  it('cliente editado mantiene dirección, referencia y correo opcional', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await navegar(harness, '/interno/clientes/1/editar', ClienteForm);
    http.expectOne(`${base}/clientes/1`).flush(cliente);
    expect(component.form.controls.correo.value).toBe('');
    expect(component.form.controls.direccionPrincipal.value).toBe('Dirección');
    component.guardar();
    const put = http.expectOne(`${base}/clientes/1`);
    expect(put.request.body.referencia).toBe('Referencia');
    expect(put.request.body.direccionPrincipal).toBe('Dirección');
    put.flush({}, { status: 409, statusText: 'Conflict' });
  });

  it('edición de recurso inexistente no habilita crear por accidente', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await navegar(harness, '/interno/categorias/999/editar', CategoriaForm);
    http
      .expectOne(`${base}/categorias/999`)
      .flush({ message: 'Categoría no encontrada' }, { status: 404, statusText: 'Not Found' });
    expect(component.errorCarga()).toBe(true);
    component.guardar();
    http.expectNone(`${base}/categorias`);
  });

  it('eliminar requiere confirmación y conserva el registro si backend responde 409', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await navegar(harness, '/interno/productos', ProductoList);
    http.expectOne(`${base}/categorias`).flush([categoria]);
    http.expectOne(`${base}/productos`).flush([producto]);
    const confirm = vi
      .spyOn(TestBed.inject(MatDialog), 'open')
      .mockReturnValue({ afterClosed: () => of(false) } as ReturnType<MatDialog['open']>);
    component.eliminar(producto);
    http.expectNone(`${base}/productos/10`);
    confirm.mockReturnValue({ afterClosed: () => of(true) } as ReturnType<MatDialog['open']>);
    component.eliminar(producto);
    http
      .expectOne(`${base}/productos/10`)
      .flush(
        { message: 'El producto tiene pedidos asociados' },
        { status: 409, statusText: 'Conflict' },
      );
    expect(component.datos()).toEqual([producto]);
    expect(component.error()).toContain('pedidos asociados');
  });

  it('errores 400/404/409 conservan mensaje seguro y 500 oculta detalles internos', () => {
    for (const status of [400, 404, 409])
      expect(
        mensajeHttp(new HttpErrorResponse({ status, error: { message: 'Mensaje empresarial' } })),
      ).toBe('Mensaje empresarial');
    expect(
      mensajeHttp(
        new HttpErrorResponse({ status: 500, error: { message: 'ORA-123 SQL secreto' } }),
      ),
    ).not.toContain('ORA');
    expect(mensajeHttp(new HttpErrorResponse({ status: 0 }))).toContain('conectar');
  });
});
