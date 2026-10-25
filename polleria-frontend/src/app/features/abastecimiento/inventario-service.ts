import { MovimientoReporte } from './reporte.model';
import { ReporteFiltro, parametrosReporte } from '../../shared/reporte-filtro';
import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ApiService } from '../../core/services/api-service';
import { Movimiento, MovimientoFiltro } from './abastecimiento.model';
@Injectable({ providedIn: 'root' })
export class InventarioService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);
  private readonly url = this.api.buildUrl('/api/v1/abastecimiento/movimientos');
  listar(f: MovimientoFiltro = {}) {
    let params = new HttpParams();
    for (const [k, v] of Object.entries(f))
      if (v !== undefined && v !== '') params = params.set(k, String(v));
    return this.http.get<Movimiento[]>(this.url, { params });
  }
  salida(insumoId: number, cantidad: string, motivo: string) {
    return this.http.post<Movimiento>(this.url + '/salidas', { insumoId, cantidad, motivo });
  }
  ajuste(insumoId: number, nuevoStock: string, motivo: string) {
    return this.http.post<Movimiento>(this.url + '/ajustes', { insumoId, nuevoStock, motivo });
  }
  porTipo(f: ReporteFiltro = {}) {
    return this.http.get<MovimientoReporte[]>(
      this.api.buildUrl('/api/v1/abastecimiento/reportes/movimientos'),
      { params: parametrosReporte(f) },
    );
  }
}
