package com.chd.rental.dto;

import com.chd.rental.entity.Mantenimiento;
import com.chd.rental.entity.enums.EstadoMantenimiento;
import com.chd.rental.entity.enums.TipoMantenimiento;
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
public class MantenimientoResponse {

    private Long id;
    private Long idVehiculo;
    private String vehiculoPlaca;
    private String vehiculoDescripcion;
    private TipoMantenimiento tipo;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Integer kilometraje;
    private String descripcion;
    private String taller;
    private BigDecimal costo;
    private EstadoMantenimiento estado;
    private LocalDate proximoMantenimiento;
    private String observaciones;
    private LocalDateTime fechaRegistro;

    public static MantenimientoResponse from(Mantenimiento m) {
        return MantenimientoResponse.builder()
                .id(m.getId())
                .idVehiculo(m.getVehiculo().getId())
                .vehiculoPlaca(m.getVehiculo().getPlaca())
                .vehiculoDescripcion(m.getVehiculo().getMarca() + " " + m.getVehiculo().getModelo())
                .tipo(m.getTipo())
                .fechaInicio(m.getFechaInicio())
                .fechaFin(m.getFechaFin())
                .kilometraje(m.getKilometraje())
                .descripcion(m.getDescripcion())
                .taller(m.getTaller())
                .costo(m.getCosto())
                .estado(m.getEstado())
                .proximoMantenimiento(m.getProximoMantenimiento())
                .observaciones(m.getObservaciones())
                .fechaRegistro(m.getFechaRegistro())
                .build();
    }
}
