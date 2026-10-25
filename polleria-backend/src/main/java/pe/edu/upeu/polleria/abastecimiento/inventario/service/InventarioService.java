package pe.edu.upeu.polleria.abastecimiento.inventario.service;
import java.util.List;
import pe.edu.upeu.polleria.abastecimiento.inventario.dto.*;
public interface InventarioService { List<MovimientoResponse> buscar(MovimientoFiltro filtro);MovimientoResponse salida(SalidaRequest r);MovimientoResponse ajuste(AjusteRequest r); }
