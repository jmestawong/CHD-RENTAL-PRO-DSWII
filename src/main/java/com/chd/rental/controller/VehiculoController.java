package com.chd.rental.controller;

import com.chd.rental.dto.VehiculoRequest;
import com.chd.rental.entity.Vehiculo;
import com.chd.rental.entity.enums.EstadoVehiculo;
import com.chd.rental.service.VehiculoService;
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
@RequestMapping("/api/vehiculos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "3. Vehiculos", description = "CRUD de la flota de camionetas 4x4")
public class VehiculoController {

    private final VehiculoService vehiculoService;

    @GetMapping
    @Operation(summary = "Listar vehiculos (filtro opcional: estado)")
    public List<Vehiculo> listar(@RequestParam(required = false) EstadoVehiculo estado) {
        return vehiculoService.listar(estado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener vehiculo por id")
    public Vehiculo obtener(@PathVariable Long id) {
        return vehiculoService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Registrar vehiculo")
    public ResponseEntity<Vehiculo> crear(@Valid @RequestBody VehiculoRequest request) {
        Vehiculo creado = vehiculoService.crear(request);
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(creado.getId()))
                .body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar datos operativos (placa, marca y modelo no se modifican)")
    public Vehiculo actualizar(@PathVariable Long id, @Valid @RequestBody VehiculoRequest request) {
        return vehiculoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar vehiculo (solo ADMIN, sin historial)")
    public void eliminar(@PathVariable Long id) {
        vehiculoService.eliminar(id);
    }
}
