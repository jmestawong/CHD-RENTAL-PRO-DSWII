package com.chd.rental.dto;

import com.chd.rental.entity.enums.EstadoPago;
import com.chd.rental.entity.enums.MetodoPago;
import com.chd.rental.entity.enums.TipoComprobante;
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
public class PagoRequest {

    @NotNull(message = "El contrato es obligatorio")
    private Long idContrato;

    @NotBlank(message = "El numero de comprobante es obligatorio")
    @Pattern(regexp = "^[A-Z0-9]{4}-[0-9]{1,8}$", message = "El comprobante debe tener el formato F001-123")
    private String numeroComprobante;

    @NotNull(message = "El tipo de comprobante es obligatorio")
    private TipoComprobante tipoComprobante;

    @NotNull(message = "La fecha de pago es obligatoria")
    @PastOrPresent(message = "La fecha de pago no puede ser futura")
    private LocalDate fechaPago;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a cero")
    @Digits(integer = 10, fraction = 2, message = "El monto admite como maximo 2 decimales")
    private BigDecimal monto;

    @NotNull(message = "El metodo de pago es obligatorio")
    private MetodoPago metodoPago;

    @Size(max = 50)
    private String banco;

    @Size(max = 50)
    private String numeroOperacion;

    private EstadoPago estado;

    @Size(max = 500)
    private String observaciones;
}
