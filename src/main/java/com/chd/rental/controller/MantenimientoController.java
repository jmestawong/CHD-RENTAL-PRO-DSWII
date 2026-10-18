package com.chd.rental.controller;

import com.chd.rental.dto.MantenimientoRequest;
import com.chd.rental.dto.MantenimientoResponse;
import com.chd.rental.service.MantenimientoService;
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
@RequestMapping("/api/mantenimientos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "6. Mantenimientos", description = "Mantenimientos preventivos y correctivos de la flota")
public class MantenimientoController {

    private final MantenimientoService mantenimientoService;

    @GetMapping
    @Operation(summary = "Listar mantenimientos (filtro opcional: idVehiculo)")
    public List<MantenimientoResponse> listar(@RequestParam(required = false) Long idVehiculo) {
        return mantenimientoService.listar(idVehiculo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener mantenimiento por id")
    public MantenimientoResponse obtener(@PathVariable Long id) {
        return mantenimientoService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Registrar mantenimiento (el vehiculo pasa a MANTENIMIENTO si esta abierto)")
    public ResponseEntity<MantenimientoResponse> crear(@Valid @RequestBody MantenimientoRequest request) {
        MantenimientoResponse creado = mantenimientoService.crear(request);
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(creado.getId()))
                .body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar o completar mantenimiento")
    public MantenimientoResponse actualizar(@PathVariable Long id, @Valid @RequestBody MantenimientoRequest request) {
        return mantenimientoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar mantenimiento (solo ADMIN)")
    public void eliminar(@PathVariable Long id) {
        mantenimientoService.eliminar(id);
    }
}
