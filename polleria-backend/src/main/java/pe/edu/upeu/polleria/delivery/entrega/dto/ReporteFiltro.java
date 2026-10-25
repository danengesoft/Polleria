package pe.edu.upeu.polleria.delivery.entrega.dto;
import java.time.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upeu.polleria.exception.InvalidRequestException;
@Getter @Setter
public class ReporteFiltro {
 @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) @Schema(description="Primer día incluido, calendario Perú") private LocalDate fechaDesde;
 @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) @Schema(description="Último día incluido, calendario Perú") private LocalDate fechaHasta;
 public void validar() {
  if (fechaDesde!=null && (fechaDesde.getYear()<1 || fechaDesde.getYear()>9999) || fechaHasta!=null && (fechaHasta.getYear()<1 || fechaHasta.getYear()>=9999)) throw new InvalidRequestException("Rango de fechas fuera del intervalo admitido");
  if (fechaDesde!=null && fechaHasta!=null && fechaDesde.isAfter(fechaHasta)) throw new InvalidRequestException("fechaDesde no puede ser posterior a fechaHasta");
 }
 public LocalDateTime desde() { validar(); return fechaDesde==null?null:fechaDesde.atStartOfDay(); }
 public LocalDateTime hasta() { validar(); return fechaHasta==null?null:fechaHasta.plusDays(1).atStartOfDay(); }
}
