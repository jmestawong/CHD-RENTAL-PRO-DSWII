package com.chd.rental.dto;

import com.chd.rental.entity.enums.EstadoRegistro;
import com.chd.rental.entity.enums.RolUsuario;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRequest {

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato valido")
    @Size(max = 100)
    private String email;

    @Size(min = 8, max = 60, message = "La contrasena debe tener entre 8 y 60 caracteres")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private RolUsuario rol;

    @Pattern(regexp = "^$|^[0-9+ ]{6,20}$", message = "Telefono invalido")
    private String telefono;

    private EstadoRegistro estado;
}
