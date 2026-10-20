package com.chd.rental.service;

import com.chd.rental.dto.LoginRequest;
import com.chd.rental.dto.LoginResponse;
import com.chd.rental.dto.UsuarioResponse;
import com.chd.rental.entity.Usuario;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    UsuarioResponse perfilActual();

    Usuario usuarioActual();
}
