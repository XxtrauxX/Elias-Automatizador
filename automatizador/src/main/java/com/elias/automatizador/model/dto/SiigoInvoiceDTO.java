package com.elias.automatizador.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SiigoInvoiceDTO {
    private String id;
    private String name;

    @JsonProperty("balance")
    private BigDecimal balance;

    @JsonProperty("total")
    private BigDecimal total;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SiigoInvoiceResponse {
        private List<SiigoInvoiceDTO> results;
    }
}
