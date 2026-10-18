package com.chd.rental.entity;

import com.chd.rental.entity.enums.EstadoContrato;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Contrato de alquiler: vincula cliente, vehiculo y usuario que lo registra.
 */
@Entity
@Table(name = "contratos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cliente", "vehiculo", "usuario"})
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contrato")
    private Long id;

    @Column(name = "numero_contrato", unique = true, length = 20)
    private String numeroContrato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vehiculo", nullable = false)
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "dias_alquiler", nullable = false)
    private Integer diasAlquiler;

    @Column(name = "tarifa_diaria", nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifaDiaria;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal igv;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(precision = 12, scale = 2)
    private BigDecimal garantia;

    @Column(name = "kilometraje_inicial")
    private Integer kilometrajeInicial;

    @Column(name = "kilometraje_final")
    private Integer kilometrajeFinal;

    @Column(name = "combustible_inicial", length = 10)
    private String combustibleInicial;

    @Column(name = "combustible_final", length = 10)
    private String combustibleFinal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoContrato estado = EstadoContrato.ACTIVO;

    @Column(name = "lugar_entrega", length = 200)
    private String lugarEntrega;

    @Column(name = "lugar_devolucion", length = 200)
    private String lugarDevolucion;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
