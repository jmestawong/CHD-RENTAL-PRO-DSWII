package com.chd.rental.repository;

import com.chd.rental.entity.Vehiculo;
import com.chd.rental.entity.enums.EstadoVehiculo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// Pruebas de la capa de acceso a datos de vehiculos (insertar, actualizar, eliminar y listar)
@DataJpaTest
@DisplayName("VehiculoRepository - capa de acceso a datos")
class VehiculoRepositoryTest {

    @Autowired
    private VehiculoRepository repository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("Insertar: estado DISPONIBLE por defecto y busqueda por placa")
    void insertar() {
        Vehiculo v = repository.save(DatosPrueba.vehiculo("AAA-111"));

        assertThat(v.getId()).isNotNull();
        assertThat(v.getEstado()).isEqualTo(EstadoVehiculo.DISPONIBLE);
        assertThat(repository.findByPlacaIgnoreCase("aaa-111")).isPresent();
        assertThat(repository.existsByPlacaIgnoreCase("AAA-111")).isTrue();
    }

    @Test
    @DisplayName("Actualizar: tarifa, kilometraje y estado")
    void actualizar() {
        Vehiculo v = repository.save(DatosPrueba.vehiculo("BBB-222"));
        em.flush();
        em.clear();

        Vehiculo editar = repository.findById(v.getId()).orElseThrow();
        editar.setTarifaDiaria(new BigDecimal("300.00"));
        editar.setKilometraje(12500);
        editar.setEstado(EstadoVehiculo.MANTENIMIENTO);
        repository.saveAndFlush(editar);
        em.clear();

        Vehiculo leido = repository.findById(v.getId()).orElseThrow();
        assertThat(leido.getTarifaDiaria()).isEqualByComparingTo("300.00");
        assertThat(leido.getKilometraje()).isEqualTo(12500);
        assertThat(leido.getEstado()).isEqualTo(EstadoVehiculo.MANTENIMIENTO);
    }

    @Test
    @DisplayName("Eliminar: el vehiculo deja de existir")
    void eliminar() {
        Vehiculo v = repository.save(DatosPrueba.vehiculo("CCC-333"));

        repository.delete(v);
        repository.flush();

        assertThat(repository.existsById(v.getId())).isFalse();
    }

    @Test
    @DisplayName("Listar: filtra por estado ordenando por marca y modelo")
    void listar() {
        Vehiculo hilux = DatosPrueba.vehiculo("DDD-444");
        Vehiculo frontier = DatosPrueba.vehiculo("EEE-555");
        frontier.setMarca("Nissan");
        frontier.setModelo("Frontier");
        Vehiculo alquilado = DatosPrueba.vehiculo("FFF-666");
        alquilado.setEstado(EstadoVehiculo.ALQUILADO);
        repository.save(hilux);
        repository.save(frontier);
        repository.save(alquilado);

        assertThat(repository.findAll()).hasSize(3);
        assertThat(repository.findByEstadoOrderByMarcaAscModeloAsc(EstadoVehiculo.DISPONIBLE))
                .extracting(Vehiculo::getPlaca)
                .containsExactly("EEE-555", "DDD-444");
    }
}
