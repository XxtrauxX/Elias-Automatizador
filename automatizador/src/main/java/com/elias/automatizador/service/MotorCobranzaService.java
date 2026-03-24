package com.elias.automatizador.service;

import com.elias.automatizador.model.dto.CobranzaRequestDTO;
import com.elias.automatizador.model.dto.SiigoInvoiceDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MotorCobranzaService {

    private final SiigoAuthService authService;
    private final WebClient.Builder webClientBuilder;
    private final TemplateEngine templateEngine;
    private final GraphEmailService graphEmailService;
    private final SharePointService sharePointService;

    public void procesarCobranza(CobranzaRequestDTO request) {
        System.out.println("⚡ Iniciando procesamiento de cobranza para NIT: " + request.getNit());
        
        try {
            // 1. Obtener Token
            String token = authService.obtenerToken();

            // 2. Por cada factura enviada desde n8n, buscar exclusivamente su public_url en Siigo
            List<CobranzaRequestDTO.FacturaN8n> facturasAProcesar = request.getFacturas();
            
            if (facturasAProcesar == null || facturasAProcesar.isEmpty()) {
                System.out.println("✅ El payload de n8n no incluye facturas. Omitiendo envío para NIT: " + request.getNit());
                return;
            }

            for (CobranzaRequestDTO.FacturaN8n factura : facturasAProcesar) {
                try {
                    SiigoInvoiceDTO.SiigoInvoiceResponse response = webClientBuilder.build()
                            .get()
                            .uri(uriBuilder -> uriBuilder
                                    .scheme("https")
                                    .host("api.siigo.com")
                                    .path("/v1/invoices")
                                    .queryParam("name", factura.getNumero())
                                    .build())
                            .header("Authorization", "Bearer " + token)
                            .header("Partner-Id", "CobranzaProyeccion")
                            .retrieve()
                            .bodyToMono(SiigoInvoiceDTO.SiigoInvoiceResponse.class)
                            .timeout(Duration.ofSeconds(10))
                            .block();

                    if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                        SiigoInvoiceDTO rawInvoice = response.getResults().get(0);
                        if (rawInvoice.getPublicUrl() != null) {
                            factura.setPublicUrl(rawInvoice.getPublicUrl());
                            factura.setId(rawInvoice.getId()); // Actualizamos el ID con el devuelto por la API
                        }
                    } else {
                        System.out.println("⚠️ Factura no encontrada en Siigo para el número: " + factura.getNumero());
                    }
                } catch (Exception ex) {
                    System.err.println("⚠️ No se pudo obtener la URL pública de la factura " + factura.getNumero() + " - " + ex.getMessage());
                }
            }

            // 3. Renderizar Plantilla HTML con Thymeleaf
            Context context = new Context();
            context.setVariable("nombre", request.getNombre());
            
            String mensajeIA = request.getMensajeIA();
            if (mensajeIA != null) {
                mensajeIA = mensajeIA.replace("\n", "<br>");
                mensajeIA = mensajeIA.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
            }
            context.setVariable("mensajeIA", mensajeIA);
            
            context.setVariable("facturas", facturasAProcesar);

            String htmlContent = templateEngine.process("email-cobranza", context);

            // 4. Enviar mediante Graph API
            String asunto = "Estado de Cuenta y Gestión de Cobranza - " + request.getNombre();
            graphEmailService.sendEmail(request.getDestinatario(), asunto, htmlContent);

            System.out.println("✅ Cobranza procesada exitosamente para: " + request.getDestinatario());

            // 5. Registrar trazabilidad en SharePoint (Asincrónicamente para no bloquear la respuesta)
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                sharePointService.registrarTrazabilidadCorreo(request.getNit(), "correo");
            });

        } catch (Exception e) {
            System.err.println("❌ Error en el proceso de cobranza para NIT " + request.getNit() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error en motor de cobranza", e);
        }
    }
}
