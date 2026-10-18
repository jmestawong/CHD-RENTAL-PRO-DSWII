package com.chd.rental.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Reglas de calculo del contrato (dias, subtotal, IGV y total).
 * Se ejecuta siempre en el servidor para que el cliente no pueda alterar los importes.
 */
public final class CalculadoraContrato {

    public static final BigDecimal TASA_IGV = new BigDecimal("0.18");

    private CalculadoraContrato() {
    }

    /** Dias de alquiler: diferencia entre fechas; un alquiler del mismo dia cuenta como 1 dia. */
    public static int calcularDias(LocalDate inicio, LocalDate fin) {
        long dias = ChronoUnit.DAYS.between(inicio, fin);
        return (int) Math.max(1, dias);
    }

    public static Importes calcular(BigDecimal tarifaDiaria, int dias) {
        BigDecimal subtotal = tarifaDiaria.multiply(BigDecimal.valueOf(dias)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal igv = subtotal.multiply(TASA_IGV).setScale(2, RoundingMode.HALF_UP);
        return new Importes(subtotal, igv, subtotal.add(igv));
    }

    public record Importes(BigDecimal subtotal, BigDecimal igv, BigDecimal total) {
    }
}
