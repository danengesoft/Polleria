package pe.edu.upeu.polleria.abastecimiento.compra.service;

import java.util.*;
import pe.edu.upeu.polleria.abastecimiento.compra.dto.*;
import pe.edu.upeu.polleria.abastecimiento.inventario.dto.MovimientoTipoReporte;
import pe.edu.upeu.polleria.abastecimiento.insumo.dto.StockActualReporte;

public interface CompraReporteService {
    List<ProveedorCompraReporte> porProveedor(ReporteFiltro filtro);

    List<InsumoCompradoReporte> insumosComprados(ReporteFiltro filtro);

    List<MovimientoTipoReporte> movimientos(ReporteFiltro filtro);

    List<StockActualReporte> stockActual();

    CompraResumenReporte resumen(ReporteFiltro filtro);
}
