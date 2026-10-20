package com.chd.rental.repository;

import com.chd.rental.entity.Contrato;
import com.chd.rental.entity.enums.EstadoContrato;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {

    // con EntityGraph se traen cliente, vehiculo y usuario en la misma consulta
    @EntityGraph(attributePaths = {"cliente", "vehiculo", "usuario"})
    List<Contrato> findAllByOrderByFechaCreacionDesc();

    @EntityGraph(attributePaths = {"cliente", "vehiculo", "usuario"})
    List<Contrato> findByEstadoOrderByFechaInicioDesc(EstadoContrato estado);

    @EntityGraph(attributePaths = {"cliente", "vehiculo", "usuario"})
    Optional<Contrato> findWithRelacionesById(Long id);

    boolean existsByClienteId(Long idCliente);

    boolean existsByVehiculoId(Long idVehiculo);

    boolean existsByUsuarioId(Long idUsuario);

    // Cuenta los contratos PENDIENTE o ACTIVO del vehiculo cuyas fechas se cruzan con las
    // nuevas. Al actualizar se excluye el mismo contrato para que no choque consigo mismo.
    @Query("""
            SELECT COUNT(c) FROM Contrato c
            WHERE c.vehiculo.id = :idVehiculo
              AND c.estado IN (com.chd.rental.entity.enums.EstadoContrato.PENDIENTE,
                               com.chd.rental.entity.enums.EstadoContrato.ACTIVO)
              AND c.fechaInicio <= :fin
              AND c.fechaFin >= :inicio
              AND (:excluirId IS NULL OR c.id <> :excluirId)
            """)
    long contarCruces(@Param("idVehiculo") Long idVehiculo,
                      @Param("inicio") LocalDate inicio,
                      @Param("fin") LocalDate fin,
                      @Param("excluirId") Long excluirId);

    boolean existsByVehiculoIdAndEstado(Long idVehiculo, EstadoContrato estado);
}
