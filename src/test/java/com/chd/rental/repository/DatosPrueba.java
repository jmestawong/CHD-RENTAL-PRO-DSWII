package com.chd.rental.repository;

import com.chd.rental.entity.*;
import com.chd.rental.entity.enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fabrica de entidades para las pruebas de la capa de acceso a datos. */
final class DatosPrueba {

    private DatosPrueba() {
    }

    static Usuario usuario(String email) {
        return Usuario.builder()
                .nombreCompleto("Usuario Prueba")
                .email(email)
                .password("$2a$10$abcdefghijklmnopqrstuuJ8vJ0n4V0m6lqZ6eYpQfH3pY7a8b9cW") // hash BCrypt de ejemplo
                .rol(RolUsuario.OPERADOR)
                .build();
    }

    static Cliente cliente(String ruc) {
        return Cliente.builder()
                .razonSocial("Cliente " + ruc)
                .ruc(ruc)
                .sector("mineria")
                .email("contacto@cliente.pe")
                .build();
    }

    static Vehiculo vehiculo(String placa) {
        return Vehiculo.builder()
                .placa(placa)
                .marca("Toyota")
                .modelo("Hilux")
                .anio(2023)
                .tipo("4x4 pickup")
                .kilometraje(10000)
                .tarifaDiaria(new BigDecimal("250.00"))
                .build();
    }

    static Contrato contrato(Cliente c, Vehiculo v, Usuario u, LocalDate inicio, LocalDate fin) {
        return Contrato.builder()
                .numeroContrato("CTR-" + System.nanoTime() % 1_000_000)
                .cliente(c)
                .vehiculo(v)
                .usuario(u)
                .fechaInicio(inicio)
                .fechaFin(fin)
                .diasAlquiler(5)
                .tarifaDiaria(new BigDecimal("250.00"))
                .subtotal(new BigDecimal("1250.00"))
                .igv(new BigDecimal("225.00"))
                .total(new BigDecimal("1475.00"))
                .garantia(new BigDecimal("500.00"))
                .estado(EstadoContrato.ACTIVO)
                .build();
    }

    static Pago pago(Contrato contrato, String comprobante) {
        return Pago.builder()
                .contrato(contrato)
                .numeroComprobante(comprobante)
                .tipoComprobante(TipoComprobante.FACTURA)
                .fechaPago(LocalDate.now())
                .monto(new BigDecimal("737.50"))
                .metodoPago(MetodoPago.TRANSFERENCIA)
                .banco("BCP")
                .build();
    }

    static Mantenimiento mantenimiento(Vehiculo v) {
        return Mantenimiento.builder()
                .vehiculo(v)
                .tipo(TipoMantenimiento.PREVENTIVO)
                .fechaInicio(LocalDate.now())
                .kilometraje(10000)
                .descripcion("Cambio de aceite y filtros")
                .taller("Taller Huaraz")
                .costo(new BigDecimal("450.00"))
                .build();
    }
}
