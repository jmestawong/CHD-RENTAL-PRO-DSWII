package com.chd.rental.repository;

import com.chd.rental.entity.Usuario;
import com.chd.rental.entity.enums.EstadoRegistro;
import com.chd.rental.entity.enums.RolUsuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

// Pruebas de usuarios; tambien se comprueba que el password quede guardado como hash BCrypt
@DataJpaTest
@DisplayName("UsuarioRepository - capa de acceso a datos")
class UsuarioRepositoryTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("Insertar: la contrasena se almacena como hash BCrypt, no en texto plano")
    void insertarConPasswordCifrado() {
        Usuario u = DatosPrueba.usuario("nuevo@chdrental.com");
        u.setPassword(encoder.encode("ClaveSegura1"));
        repository.saveAndFlush(u);
        em.clear();

        Usuario leido = repository.findByEmailIgnoreCase("NUEVO@chdrental.com").orElseThrow();
        assertThat(leido.getPassword()).startsWith("$2a$").hasSize(60).isNotEqualTo("ClaveSegura1");
        assertThat(encoder.matches("ClaveSegura1", leido.getPassword())).isTrue();
        assertThat(leido.getFechaCreacion()).isNotNull();
    }

    @Test
    @DisplayName("Actualizar: rol y estado")
    void actualizar() {
        Usuario u = repository.save(DatosPrueba.usuario("editar@chdrental.com"));
        em.flush();
        em.clear();

        Usuario editar = repository.findById(u.getId()).orElseThrow();
        editar.setRol(RolUsuario.GERENCIA);
        editar.setEstado(EstadoRegistro.INACTIVO);
        repository.saveAndFlush(editar);
        em.clear();

        Usuario leido = repository.findById(u.getId()).orElseThrow();
        assertThat(leido.getRol()).isEqualTo(RolUsuario.GERENCIA);
        assertThat(leido.getEstado()).isEqualTo(EstadoRegistro.INACTIVO);
    }

    @Test
    @DisplayName("Eliminar: el usuario deja de existir")
    void eliminar() {
        Usuario u = repository.save(DatosPrueba.usuario("borrar@chdrental.com"));

        repository.deleteById(u.getId());
        repository.flush();

        assertThat(repository.existsByEmailIgnoreCase("borrar@chdrental.com")).isFalse();
    }

    @Test
    @DisplayName("Listar: devuelve todos los usuarios registrados")
    void listar() {
        repository.save(DatosPrueba.usuario("uno@chdrental.com"));
        repository.save(DatosPrueba.usuario("dos@chdrental.com"));

        assertThat(repository.findAll())
                .extracting(Usuario::getEmail)
                .containsExactlyInAnyOrder("uno@chdrental.com", "dos@chdrental.com");
    }
}
