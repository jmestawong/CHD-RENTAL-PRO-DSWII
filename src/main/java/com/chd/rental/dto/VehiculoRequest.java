package com.chd.rental.dto;

import com.chd.rental.entity.enums.EstadoVehiculo;
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
public class VehiculoRequest {

    @NotBlank(message = "La placa es obligatoria")
    @Pattern(regexp = "^[A-Z0-9]{3}-[A-Z0-9]{3}$", message = "La placa debe tener el formato ABC-123")
    private String placa;

    @NotBlank(message = "La marca es obligatoria")
    @Size(max = 50)
    private String marca;

    @NotBlank(message = "El modelo es obligatorio")
    @Size(max = 50)
    private String modelo;

    @NotNull(message = "El anio es obligatorio")
    @Min(value = 1990, message = "El anio debe ser mayor o igual a 1990")
    @Max(value = 2100, message = "Anio invalido")
    private Integer anio;

    @Size(max = 30)
    private String tipo;

    @Size(max = 30)
    private String color;

    @Size(max = 50)
    private String numMotor;

    @Size(max = 50)
    private String numChasis;

    @PositiveOrZero
    private Integer capacidadPasajeros;

    @PositiveOrZero
    private BigDecimal capacidadCarga;

    @PositiveOrZero(message = "El kilometraje no puede ser negativo")
    private Integer kilometraje;

    @NotNull(message = "La tarifa diaria es obligatoria")
    @Positive(message = "La tarifa diaria debe ser mayor a cero")
    private BigDecimal tarifaDiaria;

    private EstadoVehiculo estado;

    private LocalDate fechaSoat;

    private LocalDate fechaRevisionTecnica;

    @Size(max = 500)
    private String observaciones;
}
