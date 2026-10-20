package com.chd.rental.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Pruebas completas de los servicios GET, POST, PUT y DELETE: se hace login, se usa el token
// y se revisa la respuesta HTTP y los datos guardados (base H2 en modo PostgreSQL)
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Servicios REST GET / POST / PUT / DELETE")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private String token(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + mapper.readTree(body).get("token").asText();
    }

    private String admin() throws Exception {
        return token("admin@chdrental.com", "Admin123*");
    }

    private ResultActions enviar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req,
                                 String token, String json) throws Exception {
        return mvc.perform(req.header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    @DisplayName("Clientes: POST 201, GET 200, PUT 200, DELETE 204 y GET 404")
    void crudClientes() throws Exception {
        String t = admin();
        String json = """
                {"razonSocial":"Transportes Huascaran SAC","ruc":"20601234567","sector":"transporte",
                 "email":"ventas@huascaran.pe","telefono":"043456789"}""";

        String creado = enviar(post("/api/clientes"), t, json)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(creado).get("id").asLong();

        mvc.perform(get("/api/clientes/" + id).header("Authorization", t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruc").value("20601234567"));

        mvc.perform(get("/api/clientes").param("buscar", "huascaran").header("Authorization", t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        enviar(put("/api/clientes/" + id), t, json.replace("Transportes Huascaran SAC", "Transportes Huascaran SAA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razonSocial").value("Transportes Huascaran SAA"));

        mvc.perform(delete("/api/clientes/" + id).header("Authorization", t))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/clientes/" + id).header("Authorization", t))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Clientes: validaciones (400) y RUC duplicado (409)")
    void validacionesClientes() throws Exception {
        String t = admin();
        enviar(post("/api/clientes"), t, "{\"razonSocial\":\"\",\"ruc\":\"123\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.razonSocial").exists())
                .andExpect(jsonPath("$.errores.ruc").exists());

        enviar(post("/api/clientes"), t, "{\"razonSocial\":\"Duplicado\",\"ruc\":\"20512345671\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un cliente con el RUC 20512345671"));
    }

    @Test
    @DisplayName("Vehiculos: no permite modificar placa/marca/modelo y valida tarifa")
    void reglasVehiculo() throws Exception {
        String t = admin();
        enviar(put("/api/vehiculos/1"), t, """
                {"placa":"ZZZ-999","marca":"Toyota","modelo":"Hilux","anio":2023,"tarifaDiaria":250}""")
                .andExpect(status().isConflict());

        enviar(post("/api/vehiculos"), t, """
                {"placa":"XYZ-100","marca":"Ford","modelo":"Ranger","anio":2024,"tarifaDiaria":0}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tarifaDiaria").exists());
    }

    @Test
    @DisplayName("Contratos: calcula importes, bloquea cruce de fechas y sincroniza el vehiculo")
    void flujoContrato() throws Exception {
        String t = token("operador@chdrental.com", "Chd12345*");
        LocalDate ini = LocalDate.now().plusDays(1);
        String json = """
                {"idCliente":1,"idVehiculo":1,"fechaInicio":"%s","fechaFin":"%s",
                 "garantia":500,"combustibleInicial":"LLENO","lugarEntrega":"Huaraz"}"""
                .formatted(ini, ini.plusDays(5));

        String creado = enviar(post("/api/contratos"), t, json)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroContrato").value(org.hamcrest.Matchers.startsWith("CTR-")))
                .andExpect(jsonPath("$.diasAlquiler").value(5))
                .andExpect(jsonPath("$.subtotal").value(1250.00))
                .andExpect(jsonPath("$.igv").value(225.00))
                .andExpect(jsonPath("$.total").value(1475.00))
                .andExpect(jsonPath("$.usuarioNombre").value("Operador de Flota"))
                .andReturn().getResponse().getContentAsString();
        JsonNode contrato = mapper.readTree(creado);

        mvc.perform(get("/api/vehiculos/1").header("Authorization", t))
                .andExpect(jsonPath("$.estado").value("ALQUILADO"));

        // mismo vehiculo con fechas cruzadas -> 409
        enviar(post("/api/contratos"), t, json).andExpect(status().isConflict());

        // finalizar el contrato libera el vehiculo y actualiza el kilometraje
        String finalizar = json.replace("\"garantia\":500", "\"garantia\":500,\"estado\":\"FINALIZADO\","
                + "\"kilometrajeFinal\":42800,\"combustibleFinal\":\"3/4\"");
        enviar(put("/api/contratos/" + contrato.get("id").asLong()), t, finalizar)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADO"));

        mvc.perform(get("/api/vehiculos/1").header("Authorization", t))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.kilometraje").value(42800));
    }

    @Test
    @DisplayName("Pagos y mantenimientos: POST / GET / PUT / DELETE")
    void pagosYMantenimientos() throws Exception {
        String t = admin();
        LocalDate ini = LocalDate.now().plusDays(10);
        long idContrato = mapper.readTree(enviar(post("/api/contratos"), t, """
                {"idCliente":2,"idVehiculo":2,"fechaInicio":"%s","fechaFin":"%s"}"""
                .formatted(ini, ini.plusDays(2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        String pago = """
                {"idContrato":%d,"numeroComprobante":"F001-100","tipoComprobante":"FACTURA",
                 "fechaPago":"%s","monto":300,"metodoPago":"TRANSFERENCIA","banco":"BCP"}"""
                .formatted(idContrato, LocalDate.now());
        long idPago = mapper.readTree(enviar(post("/api/pagos"), t, pago)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        enviar(put("/api/pagos/" + idPago), t, pago.replace("\"monto\":300", "\"monto\":542.80"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monto").value(542.80));

        // el contrato con pagos no puede eliminarse
        mvc.perform(delete("/api/contratos/" + idContrato).header("Authorization", t))
                .andExpect(status().isConflict());

        mvc.perform(delete("/api/pagos/" + idPago).header("Authorization", t))
                .andExpect(status().isNoContent());

        String mant = """
                {"idVehiculo":3,"tipo":"PREVENTIVO","fechaInicio":"%s","descripcion":"Cambio de aceite",
                 "estado":"EN_PROCESO","costo":350}""".formatted(LocalDate.now());
        long idMant = mapper.readTree(enviar(post("/api/mantenimientos"), t, mant)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/api/vehiculos/3").header("Authorization", t))
                .andExpect(jsonPath("$.estado").value("MANTENIMIENTO"));

        enviar(put("/api/mantenimientos/" + idMant), t,
                mant.replace("EN_PROCESO", "COMPLETADO").replace("\"costo\":350", "\"costo\":350,\"kilometraje\":15500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADO"));

        mvc.perform(get("/api/vehiculos/3").header("Authorization", t))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.kilometraje").value(15500));

        mvc.perform(delete("/api/mantenimientos/" + idMant).header("Authorization", t))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Autorizacion por rol: GERENCIA solo lectura, OPERADOR no elimina ni administra usuarios")
    void permisosPorRol() throws Exception {
        String gerencia = token("gerencia@chdrental.com", "Chd12345*");
        String operador = token("operador@chdrental.com", "Chd12345*");

        mvc.perform(get("/api/vehiculos").header("Authorization", gerencia))
                .andExpect(status().isOk());
        enviar(post("/api/clientes"), gerencia, "{\"razonSocial\":\"X\",\"ruc\":\"20999999999\"}")
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/clientes/1").header("Authorization", operador))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios").header("Authorization", operador))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Usuarios: el ADMIN crea un usuario y este puede iniciar sesion")
    void crudUsuarios() throws Exception {
        String t = admin();
        String nuevo = """
                {"nombreCompleto":"Nuevo Operador","email":"nuevo@chdrental.com",
                 "password":"ClaveNueva1","rol":"OPERADOR"}""";
        long id = mapper.readTree(enviar(post("/api/usuarios"), t, nuevo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        token("nuevo@chdrental.com", "ClaveNueva1");

        enviar(put("/api/usuarios/" + id), t, nuevo.replace("OPERADOR", "GERENCIA").replace(",\n \"password\":\"ClaveNueva1\"", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("GERENCIA"));

        mvc.perform(delete("/api/usuarios/" + id).header("Authorization", t))
                .andExpect(status().isNoContent());
    }
}
