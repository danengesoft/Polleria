package pe.edu.upeu.polleria.delivery.entrega.service;
import java.util.List;
import pe.edu.upeu.polleria.delivery.entrega.dto.*;
public interface EntregaReporteService {
 List<EntregaEstadoReporte> porEstado(ReporteFiltro filtro);
 DeliveryResumenReporte resumen();
}
