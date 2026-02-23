package com.elias.automatizador.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Service
@Slf4j
public class SiigoAuthService {

    private final WebClient webClient;
    private String tokenActual;

    @Value("${siigo.username}")
    private String username;

    @Value("${siigo.access_key}")
    private String accessKey;

    public SiigoAuthService(WebClient.Builder webClientBuilder) {
        // Inicializamos el cliente sin URL base para usar rutas completas
        this.webClient = webClientBuilder.build();
    }

    public String obtenerToken() {
        if (tokenActual == null) {
            tokenActual = autenticar();
        }
        return tokenActual;
    }


private String autenticar() {
    Map<String, String> body = Map.of(
        "username", username,
        "access_key", accessKey
    );

    try {
        System.out.println("Intentando login en Siigo para: " + username);
        return webClient.post()
            .uri("https://api.siigo.com/auth")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(AuthResponse.class)
            .timeout(java.time.Duration.ofSeconds(10)) // Evita que se quede pegado
            .map(AuthResponse::getAccess_token)
            .block();
    } catch (Exception e) {
        System.out.println("Error en autenticación: " + e.getMessage());
        throw e;
    }
}

    @Data
    private static class AuthResponse {
        private String access_token;
    }
}