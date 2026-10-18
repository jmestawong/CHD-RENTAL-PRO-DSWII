package com.chd.rental.service;

import com.chd.rental.dto.ClienteRequest;
import com.chd.rental.entity.Cliente;
import com.chd.rental.entity.enums.EstadoRegistro;

import java.util.List;

public interface ClienteService {

    List<Cliente> listar(EstadoRegistro estado, String buscar);

    Cliente obtener(Long id);

    Cliente crear(ClienteRequest request);

    Cliente actualizar(Long id, ClienteRequest request);

    void eliminar(Long id);
}
