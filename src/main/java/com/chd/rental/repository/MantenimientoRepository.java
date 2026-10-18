package com.chd.rental.repository;

import com.chd.rental.entity.Mantenimiento;
import com.chd.rental.entity.enums.EstadoMantenimiento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Long> {

    @EntityGraph(attributePaths = "vehiculo")
    List<Mantenimiento> findAllByOrderByFechaInicioDesc();

    @EntityGraph(attributePaths = "vehiculo")
    List<Mantenimiento> findByVehiculoIdOrderByFechaInicioDesc(Long idVehiculo);

    @EntityGraph(attributePaths = "vehiculo")
    Optional<Mantenimiento> findWithVehiculoById(Long id);

    boolean existsByVehiculoIdAndEstadoInAndIdNot(Long idVehiculo, Collection<EstadoMantenimiento> estados, Long id);

    boolean existsByVehiculoId(Long idVehiculo);
}
