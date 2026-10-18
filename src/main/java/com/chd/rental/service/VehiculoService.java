package com.chd.rental.service;

import com.chd.rental.dto.VehiculoRequest;
import com.chd.rental.entity.Vehiculo;
import com.chd.rental.entity.enums.EstadoVehiculo;

import java.util.List;

public interface VehiculoService {

    List<Vehiculo> listar(EstadoVehiculo estado);

    Vehiculo obtener(Long id);

    Vehiculo crear(VehiculoRequest request);

    Vehiculo actualizar(Long id, VehiculoRequest request);

    void eliminar(Long id);
}
