package pe.edu.upeu.polleria.abastecimiento.compra.service;
import java.util.List;
import pe.edu.upeu.polleria.abastecimiento.compra.dto.*;
public interface CompraService { List<CompraResponse> buscar(CompraFiltro f);CompraResponse obtener(Long id);CompraResponse crear(CompraRequest r); }
