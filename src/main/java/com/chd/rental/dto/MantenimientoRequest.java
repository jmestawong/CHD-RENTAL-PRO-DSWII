package com.chd.rental.dto;

import com.chd.rental.entity.enums.EstadoMantenimiento;
import com.chd.rental.entity.enums.TipoMantenimiento;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MantenimientoRequest {

    @NotNull(message = "El vehiculo es obligatorio")
    private Long idVehiculo;

    @NotNull(message = "El tipo de mantenimiento es obligatorio")
    private TipoMantenimiento tipo;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    @PositiveOrZero(message = "El kilometraje no puede ser negativo")
    private Integer kilometraje;

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(max = 500)
    private String descripcion;

    @Size(max = 100)
    private String taller;

    @PositiveOrZero(message = "El costo no puede ser negativo")
    private BigDecimal costo;

    private EstadoMantenimiento estado;

    private LocalDate proximoMantenimiento;

    @Size(max = 500)
    private String observaciones;
}
