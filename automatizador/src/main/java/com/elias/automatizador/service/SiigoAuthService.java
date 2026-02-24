package com.elias.automatizador.service;

import com.elias.automatizador.model.SiigoToken;
import com.elias.automatizador.repository.SiigoTokenRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SiigoAuthService {

    private final WebClient.Builder webClientBuilder;
    private final SiigoTokenRepository tokenRepository;
    private String tokenActual;

    @Value("${siigo.username}")
    private String username;

    @Value("${siigo.access_key}")
    private String accessKey;

    public String obtenerToken() {
        // 1. Si ya lo tenemos en memoria, lo devolvemos
        if (tokenActual != null) {
            return tokenActual;
        }

        // 2. Intentamos recuperarlo de la base de datos
        Optional<SiigoToken> siigoTokenOpt = tokenRepository.findFirstByOrderByCreatedAtDesc();
        if (siigoTokenOpt.isPresent()) {
            SiigoToken siigoToken = siigoTokenOpt.get();
            // Comprobamos si el token tiene menos de 23 horas (margen de seguridad)
            if (Duration.between(siigoToken.getCreatedAt(), LocalDateTime.now()).toHours() < 23) {
                System.out.println(
                        "✅ Reutilizando token de Siigo desde MySQL (Creado el: " + siigoToken.getCreatedAt() + ")");
                this.tokenActual = siigoToken.getAccessToken();
                return this.tokenActual;
            } else {
                System.out.println("⚠️ El token de Siigo en MySQL ha expirado. Solicitando uno nuevo...");
            }
        }

        // 3. Si no hay token o ha expirado, autenticamos de nuevo
        tokenActual = autenticar();

        // 4. Guardamos el nuevo token en la base de datos
        SiigoToken nuevoToken = new SiigoToken();
        nuevoToken.setAccessToken(tokenActual);
        tokenRepository.save(nuevoToken);
        System.out.println("💾 Nuevo token de Siigo guardado en MySQL.");

        return tokenActual;
    }

    private String autenticar() {
        Map<String, String> body = Map.of(
                "username", username,
                "access_key", accessKey);

        try {
            System.out.println("🚀 Intentando nueva autenticación en Siigo para: " + username);
            AuthResponse response = webClientBuilder.build().post()
                    .uri("https://api.siigo.com/auth")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(AuthResponse.class)
                    .timeout(Duration.ofSeconds(10)) // Evita que se quede pegado
                    .block();

            return response != null ? response.getAccess_token() : null;
        } catch (Exception e) {
            System.out.println("❌ Error en autenticación Siigo: " + e.getMessage());
            throw e;
        }
    }

    @Data
    private static class AuthResponse {
        private String access_token;
    }
}