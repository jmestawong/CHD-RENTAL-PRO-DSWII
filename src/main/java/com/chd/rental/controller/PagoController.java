package com.chd.rental.controller;

import com.chd.rental.dto.PagoRequest;
import com.chd.rental.dto.PagoResponse;
import com.chd.rental.service.PagoService;
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
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "5. Pagos", description = "Pagos de contratos (escritura: ADMIN o PAGOS)")
public class PagoController {

    private final PagoService pagoService;

    @GetMapping
    @Operation(summary = "Listar pagos (filtro opcional: idContrato)")
    public List<PagoResponse> listar(@RequestParam(required = false) Long idContrato) {
        return pagoService.listar(idContrato);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener pago por id")
    public PagoResponse obtener(@PathVariable Long id) {
        return pagoService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Registrar pago")
    public ResponseEntity<PagoResponse> crear(@Valid @RequestBody PagoRequest request) {
        PagoResponse creado = pagoService.crear(request);
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(creado.getId()))
                .body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar pago")
    public PagoResponse actualizar(@PathVariable Long id, @Valid @RequestBody PagoRequest request) {
        return pagoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar pago (solo ADMIN)")
    public void eliminar(@PathVariable Long id) {
        pagoService.eliminar(id);
    }
}
