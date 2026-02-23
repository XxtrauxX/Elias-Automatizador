package com.elias.automatizador.service;

import com.elias.automatizador.model.dto.SiigoCustomerDTO;
import com.elias.automatizador.model.dto.SiigoResponseWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;

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
            } else {
                System.out.println("⚠️ No se encontraron resultados en Siigo para el NIT: " + nit);
            }

        } catch (Exception e) {
            System.out.println("❌ Error crítico consultando cliente: " + e.getMessage());
        }

        return null;
    }
}