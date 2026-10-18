package com.chd.rental.service;

import com.chd.rental.dto.MantenimientoRequest;
import com.chd.rental.dto.MantenimientoResponse;

import java.util.List;

public interface MantenimientoService {

    List<MantenimientoResponse> listar(Long idVehiculo);

    MantenimientoResponse obtener(Long id);

    MantenimientoResponse crear(MantenimientoRequest request);

    MantenimientoResponse actualizar(Long id, MantenimientoRequest request);

    void eliminar(Long id);
}
