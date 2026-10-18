package com.chd.rental.dto;

import com.chd.rental.entity.Contrato;
import com.chd.rental.entity.enums.EstadoContrato;
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
public class ContratoResponse {

    private Long id;
    private String numeroContrato;
    private Long idCliente;
    private String clienteRazonSocial;
    private Long idVehiculo;
    private String vehiculoPlaca;
    private String vehiculoDescripcion;
    private Long idUsuario;
    private String usuarioNombre;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Integer diasAlquiler;
    private BigDecimal tarifaDiaria;
    private BigDecimal subtotal;
    private BigDecimal igv;
    private BigDecimal total;
    private BigDecimal garantia;
    private Integer kilometrajeInicial;
    private Integer kilometrajeFinal;
    private String combustibleInicial;
    private String combustibleFinal;
    private EstadoContrato estado;
    private String lugarEntrega;
    private String lugarDevolucion;
    private String observaciones;
    private LocalDateTime fechaCreacion;

    public static ContratoResponse from(Contrato c) {
        return ContratoResponse.builder()
                .id(c.getId())
                .numeroContrato(c.getNumeroContrato())
                .idCliente(c.getCliente().getId())
                .clienteRazonSocial(c.getCliente().getRazonSocial())
                .idVehiculo(c.getVehiculo().getId())
                .vehiculoPlaca(c.getVehiculo().getPlaca())
                .vehiculoDescripcion(c.getVehiculo().getMarca() + " " + c.getVehiculo().getModelo())
                .idUsuario(c.getUsuario().getId())
                .usuarioNombre(c.getUsuario().getNombreCompleto())
                .fechaInicio(c.getFechaInicio())
                .fechaFin(c.getFechaFin())
                .diasAlquiler(c.getDiasAlquiler())
                .tarifaDiaria(c.getTarifaDiaria())
                .subtotal(c.getSubtotal())
                .igv(c.getIgv())
                .total(c.getTotal())
                .garantia(c.getGarantia())
                .kilometrajeInicial(c.getKilometrajeInicial())
                .kilometrajeFinal(c.getKilometrajeFinal())
                .combustibleInicial(c.getCombustibleInicial())
                .combustibleFinal(c.getCombustibleFinal())
                .estado(c.getEstado())
                .lugarEntrega(c.getLugarEntrega())
                .lugarDevolucion(c.getLugarDevolucion())
                .observaciones(c.getObservaciones())
                .fechaCreacion(c.getFechaCreacion())
                .build();
    }
}
