package com.chd.rental.service;

import com.chd.rental.dto.PagoRequest;
import com.chd.rental.dto.PagoResponse;

import java.util.List;

public interface PagoService {

    List<PagoResponse> listar(Long idContrato);

    PagoResponse obtener(Long id);

    PagoResponse crear(PagoRequest request);

    PagoResponse actualizar(Long id, PagoRequest request);

    void eliminar(Long id);
}
