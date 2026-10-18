package com.chd.rental.service;

import com.chd.rental.dto.ContratoRequest;
import com.chd.rental.dto.ContratoResponse;
import com.chd.rental.entity.enums.EstadoContrato;

import java.util.List;

public interface ContratoService {

    List<ContratoResponse> listar(EstadoContrato estado);

    ContratoResponse obtener(Long id);

    ContratoResponse crear(ContratoRequest request);

    ContratoResponse actualizar(Long id, ContratoRequest request);

    void eliminar(Long id);
}
