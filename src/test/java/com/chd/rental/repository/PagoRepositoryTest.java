package com.chd.rental.repository;

import com.chd.rental.entity.Contrato;
import com.chd.rental.entity.Pago;
import com.chd.rental.entity.enums.EstadoPago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// Pruebas de la capa de acceso a datos de pagos (insertar, actualizar, eliminar y listar)
@DataJpaTest
@DisplayName("PagoRepository - capa de acceso a datos")
class PagoRepositoryTest {

    @Autowired
    private PagoRepository repository;

    @Autowired
    private TestEntityManager em;

    private Contrato contrato;

    @BeforeEach
    void setUp() {
        contrato = em.persist(DatosPrueba.contrato(
                em.persist(DatosPrueba.cliente("20888888888")),
                em.persist(DatosPrueba.vehiculo("HHH-888")),
                em.persist(DatosPrueba.usuario("pagos@test.com")),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 6)));
    }

    @Test
    @DisplayName("Insertar: pago asociado al contrato, estado CONFIRMADO por defecto")
    void insertar() {
        Pago p = repository.save(DatosPrueba.pago(contrato, "F001-1"));
        em.flush();
        em.clear();

        Pago leido = repository.findWithContratoById(p.getId()).orElseThrow();
        assertThat(leido.getEstado()).isEqualTo(EstadoPago.CONFIRMADO);
        assertThat(leido.getContrato().getId()).isEqualTo(contrato.getId());
        assertThat(repository.existsByNumeroComprobante("F001-1")).isTrue();
    }

    @Test
    @DisplayName("Actualizar: monto y estado")
    void actualizar() {
        Pago p = repository.save(DatosPrueba.pago(contrato, "F001-2"));
        em.flush();
        em.clear();

        Pago editar = repository.findById(p.getId()).orElseThrow();
        editar.setMonto(new BigDecimal("1475.00"));
        editar.setEstado(EstadoPago.ANULADO);
        repository.saveAndFlush(editar);
        em.clear();

        Pago leido = repository.findById(p.getId()).orElseThrow();
        assertThat(leido.getMonto()).isEqualByComparingTo("1475.00");
        assertThat(leido.getEstado()).isEqualTo(EstadoPago.ANULADO);
    }

    @Test
    @DisplayName("Eliminar: el pago deja de existir")
    void eliminar() {
        Pago p = repository.save(DatosPrueba.pago(contrato, "F001-3"));

        repository.deleteById(p.getId());
        repository.flush();

        assertThat(repository.existsByContratoId(contrato.getId())).isFalse();
    }

    @Test
    @DisplayName("Listar: todos y por contrato")
    void listar() {
        repository.save(DatosPrueba.pago(contrato, "F001-4"));
        repository.save(DatosPrueba.pago(contrato, "F001-5"));

        assertThat(repository.findAllByOrderByFechaPagoDesc()).hasSize(2);
        assertThat(repository.findByContratoIdOrderByFechaPagoDesc(contrato.getId()))
                .extracting(Pago::getNumeroComprobante)
                .containsExactlyInAnyOrder("F001-4", "F001-5");
    }
}
