package com.chd.rental.service.impl;

import com.chd.rental.dto.LoginRequest;
import com.chd.rental.dto.LoginResponse;
import com.chd.rental.dto.UsuarioResponse;
import com.chd.rental.entity.Usuario;
import com.chd.rental.exception.ReglaNegocioException;
import com.chd.rental.repository.UsuarioRepository;
import com.chd.rental.security.JwtService;
import com.chd.rental.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    // Login: valida email y contrasena contra la BD, registra la ultima conexion
    // y devuelve el token con los datos basicos del usuario (sin el password)
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        // si la clave no coincide con el hash BCrypt lanza BadCredentialsException (401)
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));
        usuario.setUltimaConexion(LocalDateTime.now());

        String token = jwtService.generarToken(usuario.getEmail(), Map.of(
                "id", usuario.getId(),
                "nombre", usuario.getNombreCompleto(),
                "rol", usuario.getRol().name()));

        return LoginResponse.builder()
                .token(token)
                .expiraEn(jwtService.getExpiracionSegundos())
                .usuario(UsuarioResponse.from(usuario))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse perfilActual() {
        return UsuarioResponse.from(usuarioActual());
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ReglaNegocioException("No hay un usuario autenticado", HttpStatus.UNAUTHORIZED);
        }
        return usuarioRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ReglaNegocioException("Usuario autenticado no encontrado",
                        HttpStatus.UNAUTHORIZED));
    }
}
