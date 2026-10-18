package com.chd.rental.repository;

import com.chd.rental.entity.Pago;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    @EntityGraph(attributePaths = {"contrato", "contrato.cliente"})
    List<Pago> findAllByOrderByFechaPagoDesc();

    @EntityGraph(attributePaths = {"contrato", "contrato.cliente"})
    List<Pago> findByContratoIdOrderByFechaPagoDesc(Long idContrato);

    @EntityGraph(attributePaths = {"contrato", "contrato.cliente"})
    Optional<Pago> findWithContratoById(Long id);

    boolean existsByNumeroComprobante(String numeroComprobante);

    boolean existsByContratoId(Long idContrato);
}
