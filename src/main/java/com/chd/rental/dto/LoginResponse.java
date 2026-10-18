package com.chd.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;

    @Builder.Default
    private String tipo = "Bearer";

    /** Vigencia del token en segundos. */
    private long expiraEn;

    private UsuarioResponse usuario;
}
