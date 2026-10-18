package com.chd.rental.repository;

import com.chd.rental.entity.Cliente;
import com.chd.rental.entity.Contrato;
import com.chd.rental.entity.Usuario;
import com.chd.rental.entity.Vehiculo;
import com.chd.rental.entity.enums.EstadoContrato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("ContratoRepository - capa de acceso a datos")
class ContratoRepositoryTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 11, 10);

    @Autowired
    private ContratoRepository repository;

    @Autowired
    private TestEntityManager em;

    private Cliente cliente;
    private Vehiculo vehiculo;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        cliente = em.persist(DatosPrueba.cliente("20777777777"));
        vehiculo = em.persist(DatosPrueba.vehiculo("GGG-777"));
        usuario = em.persist(DatosPrueba.usuario("operador@test.com"));
    }

    @Test
    @DisplayName("Insertar: guarda el contrato con sus relaciones")
    void insertar() {
        Contrato c = repository.save(DatosPrueba.contrato(cliente, vehiculo, usuario, INICIO, FIN));
        em.flush();
        em.clear();

        Contrato leido = repository.findWithRelacionesById(c.getId()).orElseThrow();
        assertThat(leido.getCliente().getRuc()).isEqualTo("20777777777");
        assertThat(leido.getVehiculo().getPlaca()).isEqualTo("GGG-777");
        assertThat(leido.getTotal()).isEqualByComparingTo("1475.00");
        assertThat(leido.getFechaCreacion()).isNotNull();
    }

    @Test
    @DisplayName("Actualizar: cambia estado y kilometraje final")
    void actualizar() {
        Contrato c = repository.save(DatosPrueba.contrato(cliente, vehiculo, usuario, INICIO, FIN));
        em.flush();
        em.clear();

        Contrato editar = repository.findById(c.getId()).orElseThrow();
        editar.setEstado(EstadoContrato.FINALIZADO);
        editar.setKilometrajeFinal(11200);
        repository.saveAndFlush(editar);
        em.clear();

        Contrato leido = repository.findById(c.getId()).orElseThrow();
        assertThat(leido.getEstado()).isEqualTo(EstadoContrato.FINALIZADO);
        assertThat(leido.getKilometrajeFinal()).isEqualTo(11200);
    }

    @Test
    @DisplayName("Eliminar: el contrato deja de existir")
    void eliminar() {
        Contrato c = repository.save(DatosPrueba.contrato(cliente, vehiculo, usuario, INICIO, FIN));

        repository.delete(c);
        repository.flush();

        assertThat(repository.findById(c.getId())).isEmpty();
        assertThat(repository.existsByVehiculoId(vehiculo.getId())).isFalse();
    }

    @Test
    @DisplayName("Listar: todos y filtrados por estado")
    void listar() {
        repository.save(DatosPrueba.contrato(cliente, vehiculo, usuario, INICIO, FIN));
        Contrato cancelado = DatosPrueba.contrato(cliente, vehiculo, usuario, INICIO.plusMonths(1), FIN.plusMonths(1));
        cancelado.setEstado(EstadoContrato.CANCELADO);
        repository.save(cancelado);

        List<Contrato> todos = repository.findAllByOrderByFechaCreacionDesc();
        List<Contrato> activos = repository.findByEstadoOrderByFechaInicioDesc(EstadoContrato.ACTIVO);

        assertThat(todos).hasSize(2);
        assertThat(activos).hasSize(1);
        assertThat(activos.get(0).getCliente().getRazonSocial()).isNotBlank();
    }

    @Test
    @DisplayName("Consulta de cruce de fechas: detecta solapamiento e ignora contratos cancelados")
    void contarCruces() {
        Contrato activo = repository.save(DatosPrueba.contrato(cliente, vehiculo, usuario, INICIO, FIN));
        Contrato cancelado = DatosPrueba.contrato(cliente, vehiculo, usuario, LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5));
        cancelado.setEstado(EstadoContrato.CANCELADO);
        repository.save(cancelado);
        em.flush();

        Long v = vehiculo.getId();
        assertThat(repository.contarCruces(v, LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 15), null))
                .isEqualTo(1);
        assertThat(repository.contarCruces(v, LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 20), null))
                .isZero();
        assertThat(repository.contarCruces(v, LocalDate.of(2026, 12, 2), LocalDate.of(2026, 12, 3), null))
                .as("los contratos CANCELADOS no bloquean el vehiculo").isZero();
        assertThat(repository.contarCruces(v, INICIO, FIN, activo.getId()))
                .as("al actualizar se excluye el propio contrato").isZero();
    }
}
