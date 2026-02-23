package com.elias.automatizador.service;

import com.elias.automatizador.model.dto.SiigoCustomerDTO;
import com.elias.automatizador.model.dto.SiigoInvoiceDTO;
import com.elias.automatizador.model.dto.SiigoResponseWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SiigoClient {

    private final SiigoAuthService authService;
    private final WebClient.Builder webClientBuilder;

    public SiigoCustomerDTO buscarClientePorNit(String nit) {
        String token = authService.obtenerToken();

        try {
            // 1. Hacemos la petición al endpoint de clientes
            SiigoResponseWrapper wrapper = webClientBuilder.build()
                    .get()
                    .uri("https://api.siigo.com/v1/customers?identification=" + nit)
                    .header("Authorization", "Bearer " + token)
                    .header("Partner-Id", "CobranzaProyeccion")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(SiigoResponseWrapper.class)
                    .timeout(Duration.ofSeconds(10)) // Timeout de seguridad
                    .block();

            // 2. Si hay resultados en la lista 'results', devolvemos el primero
            if (wrapper != null && wrapper.getResults() != null && !wrapper.getResults().isEmpty()) {
                System.out.println("✅ Cliente encontrado en Siigo: " + wrapper.getResults().get(0).getName());
                return wrapper.getResults().get(0);
            }
        } catch (Exception e) {
            System.out.println("❌ Error consultando cliente: " + e.getMessage());
        }

        return null;
    }

    public BigDecimal consultarSaldoPendiente(String nit) {
        String token = authService.obtenerToken();
        try {
            // Consultamos facturas del cliente.
            // Nota: Se asume que el endpoint de facturas filtra por identificación del
            // cliente.
            SiigoInvoiceDTO.SiigoInvoiceResponse response = webClientBuilder.build()
                    .get()
                    .uri("https://api.siigo.com/v1/invoices?customer_identification=" + nit)
                    .header("Authorization", "Bearer " + token)
                    .header("Partner-Id", "CobranzaProyeccion")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(SiigoInvoiceDTO.SiigoInvoiceResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response != null && response.getResults() != null) {
                return response.getResults().stream()
                        .map(SiigoInvoiceDTO::getBalance)
                        .filter(balance -> balance != null && balance.compareTo(BigDecimal.ZERO) > 0)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        } catch (Exception e) {
            System.out.println("❌ Error consultando facturas para NIT " + nit + ": " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }
}