package pe.edu.upeu.polleria.delivery.entrega.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.Operation;
import pe.edu.upeu.polleria.delivery.entrega.dto.*;
import pe.edu.upeu.polleria.delivery.entrega.service.EntregaReporteService;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Delivery · Reportes")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/delivery/reportes")
public class EntregaReporteController {
    private final EntregaReporteService service;

    @GetMapping("/por-estado")
    @Operation(summary = "Entregas según fecha de asignación vigente; una reasignación reemplaza esa fecha")
    public List<EntregaEstadoReporte> porEstado(@ModelAttribute @ParameterObject ReporteFiltro filtro) {
        return service.porEstado(filtro);
    }

    @GetMapping("/resumen")
    @Operation(summary = "Operación actual: entregas abiertas y perfiles de repartidores activos; sin filtro histórico")
    public DeliveryResumenReporte resumen() {
        return service.resumen();
    }
}
