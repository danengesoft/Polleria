export type UnidadMedida = 'UNIDAD' | 'KG' | 'G' | 'L' | 'ML';
export const UNIDADES: { valor: UnidadMedida; etiqueta: string }[] = [
  { valor: 'UNIDAD', etiqueta: 'Unidad' },
  { valor: 'KG', etiqueta: 'kg' },
  { valor: 'G', etiqueta: 'g' },
  { valor: 'L', etiqueta: 'L' },
  { valor: 'ML', etiqueta: 'ml' },
];
export type TipoMovimiento = 'ENTRADA' | 'SALIDA' | 'AJUSTE';
export interface InsumoRequest {
  nombre: string;
  descripcion: string | null;
  unidadMedida: UnidadMedida;
  activo: boolean;
}
export interface Insumo extends InsumoRequest {
  id: number;
  stockActual: number;
}
export interface ProveedorRequest {
  razonSocial: string;
  ruc: string | null;
  telefono: string | null;
  correo: string | null;
  direccion: string | null;
  activo: boolean;
}
export interface Proveedor extends ProveedorRequest {
  id: number;
}
export interface DetalleCompraRequest {
  insumoId: number;
  cantidad: string;
  costoUnitario: string;
}
export interface CompraRequest {
  proveedorId: number;
  detalles: DetalleCompraRequest[];
}
export interface DetalleCompra {
  id: number;
  insumoId: number;
  nombreInsumo: string;
  unidadMedida: UnidadMedida;
  cantidad: number;
  costoUnitario: number;
  subtotal: number;
}
export interface Compra {
  id: number;
  proveedor: { id: number; razonSocial: string };
  fecha: string;
  total: number;
  detalles: DetalleCompra[];
}
export interface Movimiento {
  id: number;
  insumoId: number;
  nombreInsumo: string;
  unidadMedida: UnidadMedida;
  tipo: TipoMovimiento;
  cantidad: number;
  stockAnterior: number;
  stockPosterior: number;
  fecha: string;
  motivo: string;
  compraId: number | null;
}
export interface CompraFiltro {
  proveedorId?: number;
  fechaDesde?: string;
  fechaHasta?: string;
}
export interface MovimientoFiltro {
  insumoId?: number;
  tipo?: TipoMovimiento;
  fechaDesde?: string;
  fechaHasta?: string;
}
export const CANTIDAD = /^\d{1,16}(\.\d{1,3})?$/;
export const COSTO = /^\d{1,15}(\.\d{1,4})?$/;

export function etiquetaUnidad(unidad: UnidadMedida): string {
  return UNIDADES.find((u) => u.valor === unidad)?.etiqueta ?? unidad;
}
