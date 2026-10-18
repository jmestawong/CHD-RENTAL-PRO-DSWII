package com.chd.rental.service;

import com.chd.rental.dto.UsuarioRequest;
import com.chd.rental.dto.UsuarioResponse;

import java.util.List;

public interface UsuarioService {

    List<UsuarioResponse> listar();

    UsuarioResponse obtener(Long id);

    UsuarioResponse crear(UsuarioRequest request);

    UsuarioResponse actualizar(Long id, UsuarioRequest request);

    void eliminar(Long id);
}
