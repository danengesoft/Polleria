package pe.edu.upeu.polleria.abastecimiento.compra.service;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.polleria.abastecimiento.compra.dto.*;
import pe.edu.upeu.polleria.abastecimiento.compra.entity.*;
import pe.edu.upeu.polleria.abastecimiento.compra.mapper.CompraMapper;
import pe.edu.upeu.polleria.abastecimiento.compra.repository.CompraRepository;
import pe.edu.upeu.polleria.abastecimiento.insumo.entity.Insumo;
import pe.edu.upeu.polleria.abastecimiento.insumo.repository.InsumoRepository;
import pe.edu.upeu.polleria.abastecimiento.proveedor.repository.ProveedorRepository;
import pe.edu.upeu.polleria.abastecimiento.inventario.service.InventarioServiceImpl;
import pe.edu.upeu.polleria.exception.*;
@Service @RequiredArgsConstructor
public class CompraServiceImpl implements CompraService {
 private final CompraRepository repository;private final ProveedorRepository proveedores;private final InsumoRepository insumos;private final CompraMapper mapper;private final InventarioServiceImpl inventario;
 @Override @Transactional(readOnly=true) public List<CompraResponse> buscar(CompraFiltro f){InventarioServiceImpl.validarFechas(f.getFechaDesde(),f.getFechaHasta());return repository.buscar(f.getProveedorId(),InventarioServiceImpl.desde(f.getFechaDesde()),InventarioServiceImpl.hasta(f.getFechaHasta())).stream().map(mapper::toResponse).toList();}
 @Override @Transactional(readOnly=true) public CompraResponse obtener(Long id){return mapper.toResponse(repository.findById(id).orElseThrow(()->new ResourceNotFoundException("Compra no encontrada")));}
 @Override @Transactional public CompraResponse crear(CompraRequest r){
  var proveedor=proveedores.findById(r.proveedorId()).orElseThrow(()->new ResourceNotFoundException("Proveedor no encontrado"));
  if(!Boolean.TRUE.equals(proveedor.getActivo()))throw new BusinessConflictException("Proveedor inactivo");
  if(r.detalles()==null||r.detalles().isEmpty())throw new InvalidRequestException("Agrega al menos un insumo");
  Map<Long,Insumo> bloqueados=new LinkedHashMap<>();
  var ids=r.detalles().stream().map(DetalleCompraRequest::insumoId).sorted().toList();
  if(new HashSet<>(ids).size()!=ids.size())throw new InvalidRequestException("Un insumo debe aparecer una sola vez por compra");
  for(Long id:ids){Insumo i=insumos.bloquear(id).orElseThrow(()->new ResourceNotFoundException("Insumo no encontrado"));if(!Boolean.TRUE.equals(i.getActivo()))throw new BusinessConflictException("Insumo inactivo");bloqueados.put(id,i);}
  Compra c=new Compra();c.setProveedor(proveedor);c.setFecha(LocalDateTime.now(ZoneId.of("America/Lima")));BigDecimal total=new BigDecimal("0.00");
  for(var linea:r.detalles()){
   if(linea.cantidad()==null||linea.cantidad().signum()<=0||linea.costoUnitario()==null||linea.costoUnitario().signum()<=0)throw new InvalidRequestException("Cantidad y costo deben ser positivos");
   Insumo i=bloqueados.get(linea.insumoId());DetalleCompra d=new DetalleCompra();d.setCompra(c);d.setInsumoId(i.getId());d.setNombreInsumo(i.getNombre());d.setUnidadMedida(i.getUnidadMedida());d.setCantidad(linea.cantidad());d.setCostoUnitario(linea.costoUnitario());d.setSubtotal(moneda(linea.cantidad().multiply(linea.costoUnitario())));c.getDetalles().add(d);total=moneda(total.add(d.getSubtotal()));
  }
  c.setTotal(total);repository.saveAndFlush(c);
  for(var d:c.getDetalles())inventario.entradaCompra(bloqueados.get(d.getInsumoId()),d.getCantidad(),c.getId());
  return mapper.toResponse(c);
 }
 private BigDecimal moneda(BigDecimal n){BigDecimal v=n.setScale(2,RoundingMode.HALF_UP);if(v.precision()>19)throw new InvalidRequestException("Importe fuera de precisión monetaria");return v;}
}
