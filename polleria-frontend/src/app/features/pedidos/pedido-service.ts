import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ApiService } from '../../core/services/api-service';
import { EstadoPedido, PedidoFiltro, PedidoRequest, PedidoResponse } from './pedido.model';
@Injectable({ providedIn: 'root' })
export class PedidoService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);
  private readonly resource = '/api/v1/pedidos';
  listar(filtro: PedidoFiltro = {}) {
    let params = new HttpParams();
    if (filtro.estado) params = params.set('estado', filtro.estado);
    if (filtro.tipoPedido) params = params.set('tipoPedido', filtro.tipoPedido);
    return this.http.get<PedidoResponse[]>(this.api.buildUrl(this.resource), { params });
  }
  obtener(id: number) {
    return this.http.get<PedidoResponse>(this.api.buildUrl(`${this.resource}/${id}`));
  }
  crear(request: PedidoRequest) {
    return this.http.post<PedidoResponse>(this.api.buildUrl(this.resource), request);
  }
  confirmarPago(id: number) {
    return this.http.patch<PedidoResponse>(
      this.api.buildUrl(`${this.resource}/${id}/confirmar-pago`),
      {},
    );
  }
  cambiarEstado(id: number, estado: EstadoPedido) {
    return this.http.patch<PedidoResponse>(this.api.buildUrl(`${this.resource}/${id}/estado`), {
      estado,
    });
  }
}
