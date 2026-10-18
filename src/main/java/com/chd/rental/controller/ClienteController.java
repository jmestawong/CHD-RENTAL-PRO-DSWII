package com.chd.rental.controller;

import com.chd.rental.dto.ClienteRequest;
import com.chd.rental.entity.Cliente;
import com.chd.rental.entity.enums.EstadoRegistro;
import com.chd.rental.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "2. Clientes", description = "CRUD de empresas clientes")
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    @Operation(summary = "Listar clientes (filtros opcionales: estado, buscar)")
    public List<Cliente> listar(@RequestParam(required = false) EstadoRegistro estado,
                                @RequestParam(required = false) String buscar) {
        return clienteService.listar(estado, buscar);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cliente por id")
    public Cliente obtener(@PathVariable Long id) {
        return clienteService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Registrar cliente")
    public ResponseEntity<Cliente> crear(@Valid @RequestBody ClienteRequest request) {
        Cliente creado = clienteService.crear(request);
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(creado.getId()))
                .body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar cliente")
    public Cliente actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return clienteService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar cliente (solo ADMIN, sin contratos asociados)")
    public void eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
    }
}
