package com.chd.rental.controller;

import com.chd.rental.dto.ContratoRequest;
import com.chd.rental.dto.ContratoResponse;
import com.chd.rental.entity.enums.EstadoContrato;
import com.chd.rental.service.ContratoService;
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
@RequestMapping("/api/contratos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "4. Contratos", description = "Proceso de negocio: contratos de alquiler")
public class ContratoController {

    private final ContratoService contratoService;

    @GetMapping
    @Operation(summary = "Listar contratos (filtro opcional: estado)")
    public List<ContratoResponse> listar(@RequestParam(required = false) EstadoContrato estado) {
        return contratoService.listar(estado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener contrato por id")
    public ContratoResponse obtener(@PathVariable Long id) {
        return contratoService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Registrar contrato",
            description = "Valida cliente activo, vehiculo elegible y cruce de fechas; calcula dias, subtotal, IGV y total")
    public ResponseEntity<ContratoResponse> crear(@Valid @RequestBody ContratoRequest request) {
        ContratoResponse creado = contratoService.crear(request);
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(creado.getId()))
                .body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar, finalizar o cancelar contrato")
    public ContratoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ContratoRequest request) {
        return contratoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar contrato (solo ADMIN, sin pagos)")
    public void eliminar(@PathVariable Long id) {
        contratoService.eliminar(id);
    }
}
