import { CompraResumen, ProveedorCompras, InsumoComprado } from './reporte.model';
import { ReporteFiltro, parametrosReporte } from '../../shared/reporte-filtro';
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
  resumen(f: ReporteFiltro = {}) {
    return this.http.get<CompraResumen>(
      this.api.buildUrl('/api/v1/abastecimiento/reportes/resumen-compras'),
      { params: parametrosReporte(f) },
    );
  }
  porProveedor(f: ReporteFiltro = {}) {
    return this.http.get<ProveedorCompras[]>(
      this.api.buildUrl('/api/v1/abastecimiento/reportes/compras-por-proveedor'),
      { params: parametrosReporte(f) },
    );
  }
  insumosComprados(f: ReporteFiltro = {}) {
    return this.http.get<InsumoComprado[]>(
      this.api.buildUrl('/api/v1/abastecimiento/reportes/insumos-comprados'),
      { params: parametrosReporte(f) },
    );
  }
}
