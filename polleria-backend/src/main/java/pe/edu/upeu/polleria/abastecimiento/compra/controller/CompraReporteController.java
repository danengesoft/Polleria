package pe.edu.upeu.polleria.abastecimiento.compra.controller;
import java.util.*;
import pe.edu.upeu.polleria.abastecimiento.compra.dto.*;
import pe.edu.upeu.polleria.abastecimiento.inventario.dto.MovimientoTipoReporte;
import pe.edu.upeu.polleria.abastecimiento.insumo.dto.StockActualReporte;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.Operation;
import pe.edu.upeu.polleria.abastecimiento.compra.service.CompraReporteService;
@io.swagger.v3.oas.annotations.tags.Tag(name="Abastecimiento · Reportes")
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/abastecimiento/reportes")
public class CompraReporteController {
 private final CompraReporteService service;
 @GetMapping("/resumen-compras") @Operation(summary="Compras por fecha de registro en Perú; promedio monetario exacto")
 public CompraResumenReporte resumen(@ModelAttribute @ParameterObject ReporteFiltro filtro) { return service.resumen(filtro); }
 @GetMapping("/compras-por-proveedor") @Operation(summary="Compras por proveedor; razón social vigente")
 public List<ProveedorCompraReporte> porProveedor(@ModelAttribute @ParameterObject ReporteFiltro filtro) { return service.porProveedor(filtro); }
 @GetMapping("/insumos-comprados") @Operation(summary="Cantidades e importes históricos por insumo y unidad; nombre mínimo alfabético histórico")
 public List<InsumoCompradoReporte> insumosComprados(@ModelAttribute @ParameterObject ReporteFiltro filtro) { return service.insumosComprados(filtro); }
 @GetMapping("/movimientos") @Operation(summary="Conteo de movimientos por tipo y fecha en Perú; no suma unidades físicas")
 public List<MovimientoTipoReporte> movimientos(@ModelAttribute @ParameterObject ReporteFiltro filtro) { return service.movimientos(filtro); }
 @GetMapping("/stock-actual") @Operation(summary="Stock físico actual por insumo y unidad; sin rango histórico ni valorización")
 public List<StockActualReporte> stockActual() { return service.stockActual(); }
}
