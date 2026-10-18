package com.chd.rental.dto;

import com.chd.rental.entity.enums.EstadoContrato;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos de entrada del contrato. La tarifa, dias, subtotal, IGV y total
 * se calculan en el servidor; no se aceptan desde el cliente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContratoRequest {

    @NotNull(message = "El cliente es obligatorio")
    private Long idCliente;

    @NotNull(message = "El vehiculo es obligatorio")
    private Long idVehiculo;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate fechaFin;

    @PositiveOrZero(message = "La garantia no puede ser negativa")
    private BigDecimal garantia;

    @PositiveOrZero
    private Integer kilometrajeInicial;

    @PositiveOrZero
    private Integer kilometrajeFinal;

    @Pattern(regexp = "^$|LLENO|3/4|1/2|1/4|VACIO", message = "Combustible: LLENO, 3/4, 1/2, 1/4 o VACIO")
    private String combustibleInicial;

    @Pattern(regexp = "^$|LLENO|3/4|1/2|1/4|VACIO", message = "Combustible: LLENO, 3/4, 1/2, 1/4 o VACIO")
    private String combustibleFinal;

    private EstadoContrato estado;

    @Size(max = 200)
    private String lugarEntrega;

    @Size(max = 200)
    private String lugarDevolucion;

    @Size(max = 500)
    private String observaciones;
}
