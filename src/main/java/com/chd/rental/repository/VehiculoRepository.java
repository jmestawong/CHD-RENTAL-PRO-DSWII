package com.chd.rental.repository;

import com.chd.rental.entity.Vehiculo;
import com.chd.rental.entity.enums.EstadoVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    Optional<Vehiculo> findByPlacaIgnoreCase(String placa);

    boolean existsByPlacaIgnoreCase(String placa);

    List<Vehiculo> findByEstadoOrderByMarcaAscModeloAsc(EstadoVehiculo estado);
}
