import { EntregaEstadoReporte, DeliveryResumen } from './reporte.model';
import { ReporteFiltro, parametrosReporte } from '../../shared/reporte-filtro';
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api-service';
import { Entrega, Repartidor } from './delivery.model';
@Injectable({ providedIn: 'root' })
export class DeliveryService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);
  private url(path: string) {
    return this.api.buildUrl('/api/v1/delivery/' + path);
  }
  perfil() {
    return this.http.get<Repartidor>(this.url('repartidores/me'));
  }
  activar() {
    return this.http.post<Repartidor>(this.url('repartidores/me'), {});
  }
  repartidores() {
    return this.http.get<Repartidor[]>(this.url('repartidores'));
  }
  entregas() {
    return this.http.get<Entrega[]>(this.url('entregas'));
  }
  asignar(pedidoId: number, repartidorId: number) {
    return this.http.post<Entrega>(this.url('entregas'), { pedidoId, repartidorId });
  }
  reasignar(id: number, repartidorId: number) {
    return this.http.patch<Entrega>(this.url('entregas/' + id + '/reasignar'), { repartidorId });
  }
  mias() {
    return this.http.get<Entrega[]>(this.url('entregas/mias'));
  }
  mia(id: number) {
    return this.http.get<Entrega>(this.url('entregas/mias/' + id));
  }
  iniciar(id: number) {
    return this.http.patch<Entrega>(this.url('entregas/mias/' + id + '/iniciar'), {});
  }
  entregar(id: number) {
    return this.http.patch<Entrega>(this.url('entregas/mias/' + id + '/entregar'), {});
  }
  noEntregado(id: number, observacion: string) {
    return this.http.patch<Entrega>(this.url('entregas/mias/' + id + '/no-entregado'), {
      observacion,
    });
  }
  porEstado(f: ReporteFiltro = {}) {
    return this.http.get<EntregaEstadoReporte[]>(
      this.api.buildUrl('/api/v1/delivery/reportes/por-estado'),
      { params: parametrosReporte(f) },
    );
  }
  resumen() {
    return this.http.get<DeliveryResumen>(this.api.buildUrl('/api/v1/delivery/reportes/resumen'));
  }
}
