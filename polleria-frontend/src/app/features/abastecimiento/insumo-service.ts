import { StockReporte } from './reporte.model';
import { ReporteFiltro, parametrosReporte } from '../../shared/reporte-filtro';
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api-service';
import { Insumo, InsumoRequest } from './abastecimiento.model';
@Injectable({ providedIn: 'root' })
export class InsumoService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);
  private readonly url = this.api.buildUrl('/api/v1/abastecimiento/insumos');
  listar() {
    return this.http.get<Insumo[]>(this.url);
  }
  obtener(id: number) {
    return this.http.get<Insumo>(this.url + '/' + id);
  }
  crear(r: InsumoRequest) {
    return this.http.post<Insumo>(this.url, r);
  }
  actualizar(id: number, r: InsumoRequest) {
    return this.http.put<Insumo>(this.url + '/' + id, r);
  }
  stockActual() {
    return this.http.get<StockReporte[]>(
      this.api.buildUrl('/api/v1/abastecimiento/reportes/stock-actual'),
    );
  }
}
