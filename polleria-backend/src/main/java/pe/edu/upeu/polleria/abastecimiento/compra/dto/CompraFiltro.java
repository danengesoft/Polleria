package pe.edu.upeu.polleria.abastecimiento.compra.dto;
import java.time.LocalDate;
import jakarta.validation.constraints.Positive;
import lombok.*;
@Getter @Setter public class CompraFiltro { @Positive private Long proveedorId;private LocalDate fechaDesde;private LocalDate fechaHasta; }
