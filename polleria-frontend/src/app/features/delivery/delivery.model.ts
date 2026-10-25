import { EstadoPago, MetodoPago } from '../pedidos/pedido.model';
export interface Repartidor {
  id: number;
  nombreVisible: string;
  activo: boolean;
}
export type EstadoEntrega = 'ASIGNADO' | 'EN_CAMINO' | 'ENTREGADO' | 'NO_ENTREGADO';
export interface Entrega {
  id: number;
  pedidoId: number;
  repartidor: Repartidor;
  fechaAsignacion: string;
  fechaSalida: string | null;
  fechaEntrega: string | null;
  estado: EstadoEntrega;
  observacion: string | null;
  pedido: {
    id: number;
    estadoPedido: string;
    nombreCliente: string;
    telefono: string;
    direccionEntrega: string;
    referenciaEntrega: string | null;
    total: number;
    metodoPago: MetodoPago;
    estadoPago: EstadoPago;
  };
}
