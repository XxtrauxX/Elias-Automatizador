package com.elias.automatizador.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DebtInfo {
    private String nit;
    private BigDecimal amount;
    private String rowAddress; // Para saber qué celda actualizar luego
}
