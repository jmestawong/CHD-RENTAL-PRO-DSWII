package com.chd.rental.repository;

import com.chd.rental.entity.Cliente;
import com.chd.rental.entity.enums.EstadoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByRuc(String ruc);

    boolean existsByRuc(String ruc);

    List<Cliente> findByEstadoOrderByRazonSocialAsc(EstadoRegistro estado);

    List<Cliente> findByRazonSocialContainingIgnoreCaseOrderByRazonSocialAsc(String texto);
}
