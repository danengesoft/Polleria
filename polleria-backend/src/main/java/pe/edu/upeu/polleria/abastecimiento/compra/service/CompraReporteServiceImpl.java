package pe.edu.upeu.polleria.abastecimiento.compra.service;
import java.util.*;
import pe.edu.upeu.polleria.abastecimiento.compra.dto.*;
import pe.edu.upeu.polleria.abastecimiento.inventario.dto.MovimientoTipoReporte;
import pe.edu.upeu.polleria.abastecimiento.insumo.dto.StockActualReporte;
import java.math.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.polleria.abastecimiento.compra.repository.CompraRepository;
import pe.edu.upeu.polleria.abastecimiento.inventario.repository.MovimientoInventarioRepository;
import pe.edu.upeu.polleria.abastecimiento.inventario.entity.TipoMovimiento;
import pe.edu.upeu.polleria.abastecimiento.insumo.repository.InsumoRepository;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class CompraReporteServiceImpl implements CompraReporteService {
 private final CompraRepository compras; private final MovimientoInventarioRepository inventario; private final InsumoRepository insumos;
 public CompraResumenReporte resumen(ReporteFiltro f) {
  var a=compras.agregado(f.desde(),f.hasta()); var total=a.totalComprado().setScale(2,RoundingMode.HALF_UP);
  return new CompraResumenReporte(a.cantidadCompras(),total,a.cantidadCompras()==0?new BigDecimal("0.00"):total.divide(BigDecimal.valueOf(a.cantidadCompras()),2,RoundingMode.HALF_UP));
 }
 public List<ProveedorCompraReporte> porProveedor(ReporteFiltro f) { return compras.porProveedor(f.desde(),f.hasta()); }
 public List<InsumoCompradoReporte> insumosComprados(ReporteFiltro f) { return compras.insumosComprados(f.desde(),f.hasta()); }
 public List<StockActualReporte> stockActual() { return insumos.stockActual(); }
 public List<MovimientoTipoReporte> movimientos(ReporteFiltro f) {
  var cantidades=new EnumMap<TipoMovimiento,Long>(TipoMovimiento.class);
  inventario.porTipo(f.desde(),f.hasta()).forEach(r->cantidades.put(r.tipoMovimiento(),r.cantidadMovimientos()));
  return Arrays.stream(TipoMovimiento.values()).map(t->new MovimientoTipoReporte(t,cantidades.getOrDefault(t,0L))).toList();
 }
}
