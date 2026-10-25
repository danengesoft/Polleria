package pe.edu.upeu.polleria.delivery.entrega.service;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.polleria.delivery.entrega.dto.*;
import pe.edu.upeu.polleria.delivery.entrega.entity.EstadoEntrega;
import pe.edu.upeu.polleria.delivery.entrega.repository.EntregaRepository;
import pe.edu.upeu.polleria.delivery.repartidor.repository.RepartidorRepository;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class EntregaReporteServiceImpl implements EntregaReporteService {
 private final EntregaRepository entregas; private final RepartidorRepository repartidores;
 public List<EntregaEstadoReporte> porEstado(ReporteFiltro filtro) {
  var cantidades=new EnumMap<EstadoEntrega,Long>(EstadoEntrega.class);
  entregas.porEstado(filtro.desde(),filtro.hasta()).forEach(f->cantidades.put(f.estado(),f.cantidadEntregas()));
  return Arrays.stream(EstadoEntrega.values()).map(e->new EntregaEstadoReporte(e,cantidades.getOrDefault(e,0L))).toList();
 }
 public DeliveryResumenReporte resumen() {
  return new DeliveryResumenReporte(porEstado(new ReporteFiltro()).stream().filter(f->f.estado()!=EstadoEntrega.ENTREGADO).toList(),repartidores.countByActivoTrue());
 }
}
