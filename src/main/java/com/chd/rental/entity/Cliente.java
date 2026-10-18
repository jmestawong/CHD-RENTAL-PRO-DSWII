package com.chd.rental.entity;

import com.chd.rental.entity.enums.EstadoRegistro;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Empresa cliente que alquila las camionetas (mineria, construccion, etc.).
 */
@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long id;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(nullable = false, unique = true, length = 11)
    private String ruc;

    @Column(length = 200)
    private String direccion;

    @Column(length = 50)
    private String sector;

    @Column(length = 20)
    private String telefono;

    @Column(length = 100)
    private String email;

    @Column(name = "contacto_nombre", length = 100)
    private String contactoNombre;

    @Column(name = "contacto_cargo", length = 60)
    private String contactoCargo;

    @Column(name = "contacto_telefono", length = 20)
    private String contactoTelefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoRegistro estado = EstadoRegistro.ACTIVO;

    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(length = 500)
    private String observaciones;

    @PrePersist
    void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }
}
