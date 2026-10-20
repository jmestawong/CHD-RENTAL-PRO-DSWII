package com.chd.rental.dto;

import com.chd.rental.entity.Usuario;
import com.chd.rental.entity.enums.EstadoRegistro;
import com.chd.rental.entity.enums.RolUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Lo que se devuelve del usuario; no incluye el password
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {

    private Long id;
    private String nombreCompleto;
    private String email;
    private RolUsuario rol;
    private String telefono;
    private EstadoRegistro estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimaConexion;

    public static UsuarioResponse from(Usuario u) {
        return UsuarioResponse.builder()
                .id(u.getId())
                .nombreCompleto(u.getNombreCompleto())
                .email(u.getEmail())
                .rol(u.getRol())
                .telefono(u.getTelefono())
                .estado(u.getEstado())
                .fechaCreacion(u.getFechaCreacion())
                .ultimaConexion(u.getUltimaConexion())
                .build();
    }
}
