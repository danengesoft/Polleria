import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api-service';
import { Proveedor, ProveedorRequest } from './abastecimiento.model';
@Injectable({ providedIn: 'root' })
export class ProveedorService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);
  private readonly url = this.api.buildUrl('/api/v1/abastecimiento/proveedores');
  listar() {
    return this.http.get<Proveedor[]>(this.url);
  }
  obtener(id: number) {
    return this.http.get<Proveedor>(this.url + '/' + id);
  }
  crear(r: ProveedorRequest) {
    return this.http.post<Proveedor>(this.url, r);
  }
  actualizar(id: number, r: ProveedorRequest) {
    return this.http.put<Proveedor>(this.url + '/' + id, r);
  }
}
