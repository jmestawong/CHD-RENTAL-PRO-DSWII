package com.chd.rental.controller;

import com.chd.rental.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas del servicio REST de login: credenciales correctas e incorrectas,
// validaciones y acceso a servicios protegidos sin token o con un token invalido
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("POST /api/auth/login")
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Credenciales correctas: 200 con token JWT y datos del usuario")
    void loginCorrecto() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@chdrental.com\",\"password\":\"Admin123*\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuario.rol").value("ADMIN"))
                .andExpect(jsonPath("$.usuario.password").doesNotExist());
    }

    @Test
    @DisplayName("La contrasena del usuario esta cifrada con BCrypt en la BD")
    void passwordCifradoEnBd() {
        String hash = usuarioRepository.findByEmailIgnoreCase("admin@chdrental.com").orElseThrow().getPassword();
        assertThat(hash).startsWith("$2a$").isNotEqualTo("Admin123*");
    }

    @Test
    @DisplayName("Contrasena incorrecta: 401")
    void loginIncorrecto() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@chdrental.com\",\"password\":\"otra\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Email o contrasena incorrectos"));
    }

    @Test
    @DisplayName("Usuario inexistente: 401 con el mismo mensaje (no revela si el email existe)")
    void usuarioInexistente() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@chdrental.com\",\"password\":\"Admin123*\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Email o contrasena incorrectos"));
    }

    @Test
    @DisplayName("Campos vacios o email invalido: 400 con detalle por campo")
    void validaciones() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").exists())
                .andExpect(jsonPath("$.errores.password").exists());
    }

    @Test
    @DisplayName("Acceso a un servicio protegido sin token: 401")
    void sinToken() throws Exception {
        mvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token invalido: 401")
    void tokenInvalido() throws Exception {
        mvc.perform(get("/api/clientes").header("Authorization", "Bearer token.falso.123"))
                .andExpect(status().isUnauthorized());
    }
}
