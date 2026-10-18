package com.chd.rental.entity;

import com.chd.rental.entity.enums.EstadoMantenimiento;
import com.chd.rental.entity.enums.TipoMantenimiento;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Mantenimiento preventivo o correctivo de un vehiculo.
 */
@Entity
@Table(name = "mantenimientos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "vehiculo")
public class Mantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mantenimiento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vehiculo", nullable = false)
    private Vehiculo vehiculo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMantenimiento tipo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    private Integer kilometraje;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(length = 100)
    private String taller;

    @Column(precision = 12, scale = 2)
    private BigDecimal costo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoMantenimiento estado = EstadoMantenimiento.PROGRAMADO;

    @Column(name = "proximo_mantenimiento")
    private LocalDate proximoMantenimiento;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }
}
