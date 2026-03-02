package com.elias.automatizador.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebClient.Builder webClientBuilder;
    private final String WEBHOOK_URL = "http://31.97.140.162:7890/webhook-test/extraccion-completada";

    public void sendExtractionNotification(String batchId, String sheetName, int totalRecords) {
        System.out.println("📤 Enviando notificación Webhook a n8n... Lote: " + batchId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("batch_id", batchId);
        payload.put("sheet_name", sheetName);
        payload.put("total_records", totalRecords);
        payload.put("status", "success");

        try {
            webClientBuilder.build()
                    .post()
                    .uri(WEBHOOK_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                            response -> System.out.println("✅ Webhook enviado exitosamente. Respuesta: " + response),
                            error -> System.err.println("❌ Error enviando Webhook: " + error.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Excepción al intentar enviar Webhook: " + e.getMessage());
        }
    }
}
