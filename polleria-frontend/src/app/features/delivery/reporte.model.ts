export interface EntregaEstadoReporte {
  estado: string;
  cantidadEntregas: number;
}
export interface DeliveryResumen {
  entregasAbiertas: EntregaEstadoReporte[];
  repartidoresActivos: number;
}
