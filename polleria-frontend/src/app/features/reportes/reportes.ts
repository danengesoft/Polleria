import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable, Subscription } from 'rxjs';
import { PedidoService } from '../pedidos/pedido-service';
import { DeliveryService } from '../delivery/delivery-service';
import { CompraService } from '../abastecimiento/compra-service';
import { InventarioService } from '../abastecimiento/inventario-service';
import { InsumoService } from '../abastecimiento/insumo-service';
import { ReporteTabla, TablaReporte, Celda } from './reporte-tabla';
import { mensajeHttp } from '../../shared/http-error';
@Component({
  selector: 'app-reportes',
  imports: [FormsModule, ReporteTabla],
  templateUrl: './reportes.html',
  styleUrl: './reportes.css',
})
export class Reportes {
  private readonly pedidos = inject(PedidoService);
  private readonly delivery = inject(DeliveryService);
  private readonly compras = inject(CompraService);
  private readonly inventario = inject(InventarioService);
  private readonly insumos = inject(InsumoService);
  private pendientes = new Subscription();
  fechaDesde = '';
  fechaHasta = '';
  top = 20;
  readonly errorFiltro = signal('');
  readonly rangoAplicado = signal('Todo el historial');
  private tabla() {
    return signal<TablaReporte>({ cargando: true, error: '', filas: [] });
  }
  readonly ventas = this.tabla();
  readonly dias = this.tabla();
  readonly pagos = this.tabla();
  readonly productos = this.tabla();
  readonly clientes = this.tabla();
  readonly estados = this.tabla();
  readonly tipos = this.tabla();
  readonly entregas = this.tabla();
  readonly operacion = this.tabla();
  readonly compraResumen = this.tabla();
  readonly proveedores = this.tabla();
  readonly comprados = this.tabla();
  readonly movimientos = this.tabla();
  readonly stock = this.tabla();
  constructor() {
    inject(DestroyRef).onDestroy(() => this.pendientes.unsubscribe());
    this.aplicar();
  }
  private cargar<T>(
    destino: ReturnType<Reportes['tabla']>,
    fuente: Observable<T>,
    filas: (datos: T) => Celda[][],
  ) {
    destino.set({ cargando: true, error: '', filas: [] });
    this.pendientes.add(
      fuente.subscribe({
        next: (datos) => destino.set({ cargando: false, error: '', filas: filas(datos) }),
        error: (e) => destino.set({ cargando: false, error: mensajeHttp(e), filas: [] }),
      }),
    );
  }
  aplicar() {
    if (this.fechaDesde && this.fechaHasta && this.fechaDesde > this.fechaHasta) {
      this.errorFiltro.set('Desde no puede ser posterior a Hasta.');
      return;
    }
    if (!Number.isInteger(this.top) || this.top < 1 || this.top > 100) {
      this.errorFiltro.set('Top debe estar entre 1 y 100.');
      return;
    }
    this.errorFiltro.set('');
    this.rangoAplicado.set(
      `${this.fechaDesde || 'Inicio del historial'} — ${this.fechaHasta || 'Sin fecha final'} · calendario Perú`,
    );
    this.pendientes.unsubscribe();
    this.pendientes = new Subscription();
    const f = { fechaDesde: this.fechaDesde, fechaHasta: this.fechaHasta };
    this.cargar(this.ventas, this.pedidos.resumen(f), (r) => [
      ['Resumen', r.cantidadPedidos, r.totalMonetario, r.ticketPromedio],
    ]);
    this.cargar(this.dias, this.pedidos.ventasPorDia(f), (r) =>
      r.map((x) => [x.fecha, x.cantidadVentas, x.totalVentas]),
    );
    this.cargar(this.pagos, this.pedidos.metodosPago(f), (r) =>
      r.map((x) => [x.metodoPago, x.cantidadVentas, x.totalVentas]),
    );
    this.cargar(this.productos, this.pedidos.productosVendidos({ ...f, top: this.top }), (r) =>
      r.map((x, i) => [
        i + 1,
        x.nombreProducto + ' (#' + x.productoId + ')',
        x.cantidadVendida,
        x.importeGenerado,
      ]),
    );
    this.cargar(this.clientes, this.pedidos.clientesCompradores({ ...f, top: this.top }), (r) =>
      r.map((x) => ['Cliente #' + x.clienteId, x.cantidadPedidos, x.totalComprado]),
    );
    this.cargar(this.estados, this.pedidos.porEstado(f), (r) =>
      r.map((x) => [x.estado, x.cantidadPedidos]),
    );
    this.cargar(this.tipos, this.pedidos.porTipo(f), (r) =>
      r.map((x) => [x.tipoPedido, x.cantidadPedidos]),
    );
    this.cargar(this.entregas, this.delivery.porEstado(f), (r) =>
      r.map((x) => [x.estado, x.cantidadEntregas]),
    );
    this.cargar(this.operacion, this.delivery.resumen(), (r) => [
      ...r.entregasAbiertas.map((x) => [x.estado, x.cantidadEntregas] as Celda[]),
      ['Repartidores activos', r.repartidoresActivos],
    ]);
    this.cargar(this.compraResumen, this.compras.resumen(f), (r) => [
      ['Resumen', r.cantidadCompras, r.totalComprado, r.compraPromedio],
    ]);
    this.cargar(this.proveedores, this.compras.porProveedor(f), (r) =>
      r.map((x) => [
        x.razonSocial + ' (#' + x.proveedorId + ')',
        x.cantidadCompras,
        x.totalComprado,
      ]),
    );
    this.cargar(this.comprados, this.compras.insumosComprados(f), (r) =>
      r.map((x) => [
        x.nombreInsumo + ' (#' + x.insumoId + ')',
        x.unidadMedida,
        x.cantidadComprada,
        x.importeComprado,
      ]),
    );
    this.cargar(this.movimientos, this.inventario.porTipo(f), (r) =>
      r.map((x) => [x.tipoMovimiento, x.cantidadMovimientos]),
    );
    this.cargar(this.stock, this.insumos.stockActual(), (r) =>
      r.map((x) => [
        x.nombre + ' (#' + x.id + ')',
        x.unidadMedida,
        x.stockActual,
        x.activo ? 'Sí' : 'No',
      ]),
    );
  }
}
