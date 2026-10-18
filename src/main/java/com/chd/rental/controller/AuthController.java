package com.chd.rental.controller;

import com.chd.rental.dto.LoginRequest;
import com.chd.rental.dto.LoginResponse;
import com.chd.rental.dto.UsuarioResponse;
import com.chd.rental.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "1. Autenticacion", description = "Servicio REST de login (usuario y password en BD, cifrado BCrypt)")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion",
            description = "Valida email y contrasena contra la tabla usuarios (hash BCrypt) y devuelve un token JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Usuario autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    public UsuarioResponse me() {
        return authService.perfilActual();
    }
}
