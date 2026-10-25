package pe.edu.upeu.polleria.abastecimiento.inventario.dto;
import java.time.LocalDate;
import jakarta.validation.constraints.Positive;
import lombok.*;
import pe.edu.upeu.polleria.abastecimiento.inventario.entity.TipoMovimiento;
@Getter @Setter public class MovimientoFiltro {
 @Positive private Long insumoId;private TipoMovimiento tipo;private LocalDate fechaDesde;private LocalDate fechaHasta;
}
