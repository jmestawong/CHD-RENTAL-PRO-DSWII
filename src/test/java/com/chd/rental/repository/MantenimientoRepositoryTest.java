package com.chd.rental.repository;

import com.chd.rental.entity.Mantenimiento;
import com.chd.rental.entity.Vehiculo;
import com.chd.rental.entity.enums.EstadoMantenimiento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

// Pruebas de mantenimientos y de la consulta que verifica si un vehiculo tiene mantenimientos abiertos
@DataJpaTest
@DisplayName("MantenimientoRepository - capa de acceso a datos")
class MantenimientoRepositoryTest {

    @Autowired
    private MantenimientoRepository repository;

    @Autowired
    private TestEntityManager em;

    private Vehiculo vehiculo;

    @BeforeEach
    void setUp() {
        vehiculo = em.persist(DatosPrueba.vehiculo("JJJ-999"));
    }

    @Test
    @DisplayName("Insertar: estado PROGRAMADO por defecto")
    void insertar() {
        Mantenimiento m = repository.save(DatosPrueba.mantenimiento(vehiculo));
        em.flush();
        em.clear();

        Mantenimiento leido = repository.findWithVehiculoById(m.getId()).orElseThrow();
        assertThat(leido.getEstado()).isEqualTo(EstadoMantenimiento.PROGRAMADO);
        assertThat(leido.getVehiculo().getPlaca()).isEqualTo("JJJ-999");
    }

    @Test
    @DisplayName("Actualizar: completar mantenimiento con fecha de fin")
    void actualizar() {
        Mantenimiento m = repository.save(DatosPrueba.mantenimiento(vehiculo));
        em.flush();
        em.clear();

        Mantenimiento editar = repository.findById(m.getId()).orElseThrow();
        editar.setEstado(EstadoMantenimiento.COMPLETADO);
        editar.setFechaFin(LocalDate.now());
        repository.saveAndFlush(editar);
        em.clear();

        Mantenimiento leido = repository.findById(m.getId()).orElseThrow();
        assertThat(leido.getEstado()).isEqualTo(EstadoMantenimiento.COMPLETADO);
        assertThat(leido.getFechaFin()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Eliminar: el mantenimiento deja de existir")
    void eliminar() {
        Mantenimiento m = repository.save(DatosPrueba.mantenimiento(vehiculo));

        repository.delete(m);
        repository.flush();

        assertThat(repository.existsByVehiculoId(vehiculo.getId())).isFalse();
    }

    @Test
    @DisplayName("Listar: por vehiculo y deteccion de mantenimientos abiertos")
    void listar() {
        Mantenimiento abierto = repository.save(DatosPrueba.mantenimiento(vehiculo));
        Mantenimiento cerrado = DatosPrueba.mantenimiento(vehiculo);
        cerrado.setEstado(EstadoMantenimiento.COMPLETADO);
        repository.save(cerrado);

        assertThat(repository.findAllByOrderByFechaInicioDesc()).hasSize(2);
        assertThat(repository.findByVehiculoIdOrderByFechaInicioDesc(vehiculo.getId())).hasSize(2);
        var abiertos = EnumSet.of(EstadoMantenimiento.PROGRAMADO, EstadoMantenimiento.EN_PROCESO);
        assertThat(repository.existsByVehiculoIdAndEstadoInAndIdNot(vehiculo.getId(), abiertos, -1L)).isTrue();
        assertThat(repository.existsByVehiculoIdAndEstadoInAndIdNot(vehiculo.getId(), abiertos, abierto.getId()))
                .isFalse();
    }
}
