package com.chd.rental.dto;

import com.chd.rental.entity.Pago;
import com.chd.rental.entity.enums.EstadoPago;
import com.chd.rental.entity.enums.MetodoPago;
import com.chd.rental.entity.enums.TipoComprobante;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoResponse {

    private Long id;
    private Long idContrato;
    private String numeroContrato;
    private String clienteRazonSocial;
    private String numeroComprobante;
    private TipoComprobante tipoComprobante;
    private LocalDate fechaPago;
    private BigDecimal monto;
    private MetodoPago metodoPago;
    private String banco;
    private String numeroOperacion;
    private EstadoPago estado;
    private String observaciones;
    private LocalDateTime fechaRegistro;

    public static PagoResponse from(Pago p) {
        return PagoResponse.builder()
                .id(p.getId())
                .idContrato(p.getContrato().getId())
                .numeroContrato(p.getContrato().getNumeroContrato())
                .clienteRazonSocial(p.getContrato().getCliente().getRazonSocial())
                .numeroComprobante(p.getNumeroComprobante())
                .tipoComprobante(p.getTipoComprobante())
                .fechaPago(p.getFechaPago())
                .monto(p.getMonto())
                .metodoPago(p.getMetodoPago())
                .banco(p.getBanco())
                .numeroOperacion(p.getNumeroOperacion())
                .estado(p.getEstado())
                .observaciones(p.getObservaciones())
                .fechaRegistro(p.getFechaRegistro())
                .build();
    }
}
