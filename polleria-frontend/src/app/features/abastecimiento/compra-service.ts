import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ApiService } from '../../core/services/api-service';
import { Compra, CompraRequest, CompraFiltro } from './abastecimiento.model';
@Injectable({ providedIn: 'root' })
export class CompraService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);
  private readonly url = this.api.buildUrl('/api/v1/abastecimiento/compras');
  listar(f: CompraFiltro = {}) {
    let params = new HttpParams();
    for (const [k, v] of Object.entries(f))
      if (v !== undefined && v !== '') params = params.set(k, String(v));
    return this.http.get<Compra[]>(this.url, { params });
  }
  obtener(id: number) {
    return this.http.get<Compra>(this.url + '/' + id);
  }
  crear(r: CompraRequest) {
    return this.http.post<Compra>(this.url, r);
  }
}
