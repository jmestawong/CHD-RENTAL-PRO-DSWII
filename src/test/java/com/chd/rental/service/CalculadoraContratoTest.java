package com.chd.rental.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// Pruebas del calculo de dias, subtotal, IGV y total del contrato
@DisplayName("CalculadoraContrato - reglas de calculo")
class CalculadoraContratoTest {

    @Test
    @DisplayName("Dias: diferencia entre fechas, minimo 1 dia")
    void dias() {
        LocalDate hoy = LocalDate.of(2026, 11, 1);
        assertThat(CalculadoraContrato.calcularDias(hoy, hoy.plusDays(7))).isEqualTo(7);
        assertThat(CalculadoraContrato.calcularDias(hoy, hoy)).isEqualTo(1);
    }

    @Test
    @DisplayName("Importes: subtotal = tarifa x dias, IGV 18 %, total = subtotal + IGV")
    void importes() {
        var imp = CalculadoraContrato.calcular(new BigDecimal("235.50"), 3);

        assertThat(imp.subtotal()).isEqualByComparingTo("706.50");
        assertThat(imp.igv()).isEqualByComparingTo("127.17");
        assertThat(imp.total()).isEqualByComparingTo("833.67");
    }
}
