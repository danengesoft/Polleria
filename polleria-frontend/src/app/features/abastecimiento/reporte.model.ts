export interface CompraResumen {
  cantidadCompras: number;
  totalComprado: number;
  compraPromedio: number;
}
export interface ProveedorCompras {
  proveedorId: number;
  razonSocial: string;
  cantidadCompras: number;
  totalComprado: number;
}
export interface InsumoComprado {
  insumoId: number;
  nombreInsumo: string;
  unidadMedida: string;
  cantidadComprada: number;
  importeComprado: number;
}
export interface MovimientoReporte {
  tipoMovimiento: string;
  cantidadMovimientos: number;
}
export interface StockReporte {
  id: number;
  nombre: string;
  unidadMedida: string;
  stockActual: number;
  activo: boolean;
}
