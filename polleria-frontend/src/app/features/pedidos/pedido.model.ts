export type TipoPedido = 'PRESENCIAL' | 'RECOJO' | 'DELIVERY';
export type MetodoPago = 'EFECTIVO' | 'YAPE' | 'PLIN' | 'TARJETA';
export type EstadoPedido =
  | 'PENDIENTE'
  | 'CONFIRMADO'
  | 'EN_PREPARACION'
  | 'LISTO'
  | 'DESPACHADO'
  | 'ENTREGADO'
  | 'CANCELADO';
export type EstadoPago = 'PENDIENTE' | 'PAGADO' | 'ANULADO';
export interface DetallePedidoRequest {
  productoId: number;
  cantidad: number;
}
export interface PedidoRequest {
  clienteId: number;
  tipoPedido: TipoPedido;
  metodoPago: MetodoPago;
  direccionEntrega: string | null;
  referenciaEntrega: string | null;
  detalles: DetallePedidoRequest[];
}
export interface DetallePedidoResponse {
  productoId: number;
  nombreProducto: string;
  precioUnitario: number;
  cantidad: number;
  subtotal: number;
}
export interface PedidoResponse {
  id: number;
  cliente: { id: number; nombres: string; apellidos: string };
  fecha: string;
  tipoPedido: TipoPedido;
  estado: EstadoPedido;
  metodoPago: MetodoPago;
  estadoPago: EstadoPago;
  direccionEntrega: string | null;
  referenciaEntrega: string | null;
  total: number;
  detalles: DetallePedidoResponse[];
}
export interface PedidoFiltro {
  estado?: EstadoPedido;
  tipoPedido?: TipoPedido;
}
export const TIPOS: TipoPedido[] = ['PRESENCIAL', 'RECOJO', 'DELIVERY'];
export const METODOS: MetodoPago[] = ['EFECTIVO', 'YAPE', 'PLIN', 'TARJETA'];
export const ESTADOS: EstadoPedido[] = [
  'PENDIENTE',
  'CONFIRMADO',
  'EN_PREPARACION',
  'LISTO',
  'DESPACHADO',
  'ENTREGADO',
  'CANCELADO',
];
export function etiqueta(value: string): string {
  if (value === 'EN_PREPARACION') return 'En preparación';
  return value.charAt(0) + value.slice(1).toLowerCase().replaceAll('_', ' ');
}
export function transiciones(pedido: PedidoResponse): EstadoPedido[] {
  switch (pedido.estado) {
    case 'PENDIENTE':
      return ['CONFIRMADO', ...(pedido.estadoPago !== 'PAGADO' ? ['CANCELADO' as const] : [])];
    case 'CONFIRMADO':
      return ['EN_PREPARACION', ...(pedido.estadoPago !== 'PAGADO' ? ['CANCELADO' as const] : [])];
    case 'EN_PREPARACION':
      return ['LISTO'];
    case 'LISTO':
      return pedido.tipoPedido === 'DELIVERY' ? [] : ['ENTREGADO'];
    default:
      return [];
  }
}
