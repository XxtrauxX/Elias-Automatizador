package com.elias.automatizador.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CobranzaRequestDTO {
    private String destinatario;
    private String nombre;
    private String nit;
    private String mensajeIA;
    private List<FacturaN8n> facturas;

    @Data
    public static class FacturaN8n {
        private String id;
        private String numero;
        private BigDecimal monto;
        private String vencimiento;
        private String publicUrl; // Poblado posterior en el Motor
    }
}
