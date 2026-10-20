package com.chd.rental.repository;

import com.chd.rental.entity.Cliente;
import com.chd.rental.entity.enums.EstadoRegistro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Pruebas de la capa de acceso a datos de clientes: insertar, actualizar, eliminar y listar.
// @DataJpaTest usa una base H2 en memoria y deshace los cambios al terminar cada prueba.
@DataJpaTest
@DisplayName("ClienteRepository - capa de acceso a datos")
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository repository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("Insertar: genera id, fecha de registro y estado ACTIVO por defecto")
    void insertar() {
        Cliente guardado = repository.save(DatosPrueba.cliente("20111111111"));

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getFechaRegistro()).isNotNull();
        assertThat(guardado.getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
        assertThat(repository.findByRuc("20111111111")).isPresent();
    }

    @Test
    @DisplayName("Insertar: rechaza RUC duplicado por restriccion UNIQUE")
    void insertarRucDuplicado() {
        repository.saveAndFlush(DatosPrueba.cliente("20222222222"));

        assertThatThrownBy(() -> repository.saveAndFlush(DatosPrueba.cliente("20222222222")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Actualizar: persiste los cambios de razon social y estado")
    void actualizar() {
        Cliente c = repository.save(DatosPrueba.cliente("20333333333"));
        em.flush();
        em.clear();

        Cliente aEditar = repository.findById(c.getId()).orElseThrow();
        aEditar.setRazonSocial("Razon Social Actualizada");
        aEditar.setEstado(EstadoRegistro.INACTIVO);
        repository.saveAndFlush(aEditar);
        em.clear();

        Cliente leido = repository.findById(c.getId()).orElseThrow();
        assertThat(leido.getRazonSocial()).isEqualTo("Razon Social Actualizada");
        assertThat(leido.getEstado()).isEqualTo(EstadoRegistro.INACTIVO);
    }

    @Test
    @DisplayName("Eliminar: el registro deja de existir")
    void eliminar() {
        Cliente c = repository.save(DatosPrueba.cliente("20444444444"));

        repository.deleteById(c.getId());
        repository.flush();

        assertThat(repository.findById(c.getId())).isEmpty();
    }

    @Test
    @DisplayName("Listar: todos, por estado y por texto")
    void listar() {
        repository.save(DatosPrueba.cliente("20555555555"));
        Cliente inactivo = DatosPrueba.cliente("20666666666");
        inactivo.setEstado(EstadoRegistro.INACTIVO);
        inactivo.setRazonSocial("Minera Inactiva SAC");
        repository.save(inactivo);

        List<Cliente> todos = repository.findAll();
        List<Cliente> activos = repository.findByEstadoOrderByRazonSocialAsc(EstadoRegistro.ACTIVO);
        List<Cliente> porTexto = repository.findByRazonSocialContainingIgnoreCaseOrderByRazonSocialAsc("minera");

        assertThat(todos).hasSize(2);
        assertThat(activos).extracting(Cliente::getRuc).containsExactly("20555555555");
        assertThat(porTexto).extracting(Cliente::getRuc).containsExactly("20666666666");
    }
}
