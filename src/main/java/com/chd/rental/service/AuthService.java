package com.chd.rental.service;

import com.chd.rental.dto.LoginRequest;
import com.chd.rental.dto.LoginResponse;
import com.chd.rental.dto.UsuarioResponse;
import com.chd.rental.entity.Usuario;

public interface AuthService {

    /** Valida credenciales contra la BD (hash BCrypt) y emite un JWT. */
    LoginResponse login(LoginRequest request);

    /** Datos del usuario autenticado en la peticion actual. */
    UsuarioResponse perfilActual();

    /** Entidad del usuario autenticado en la peticion actual. */
    Usuario usuarioActual();
}
