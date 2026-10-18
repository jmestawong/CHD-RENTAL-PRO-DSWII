package com.chd.rental.dto;

import com.chd.rental.entity.enums.EstadoRegistro;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteRequest {

    @NotBlank(message = "La razon social es obligatoria")
    @Size(max = 150)
    private String razonSocial;

    @NotBlank(message = "El RUC es obligatorio")
    @Pattern(regexp = "^(10|15|16|17|20)\\d{9}$",
            message = "El RUC debe tener 11 digitos y empezar con 10, 15, 16, 17 o 20")
    private String ruc;

    @Size(max = 200)
    private String direccion;

    @Size(max = 50)
    private String sector;

    @Pattern(regexp = "^$|^[0-9+ ]{6,20}$", message = "Telefono invalido")
    private String telefono;

    @Email(message = "El email no tiene un formato valido")
    @Size(max = 100)
    private String email;

    @Size(max = 100)
    private String contactoNombre;

    @Size(max = 60)
    private String contactoCargo;

    @Pattern(regexp = "^$|^[0-9+ ]{6,20}$", message = "Telefono de contacto invalido")
    private String contactoTelefono;

    private EstadoRegistro estado;

    @Size(max = 500)
    private String observaciones;
}
