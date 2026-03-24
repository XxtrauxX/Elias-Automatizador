package com.elias.automatizador.controller;

import com.elias.automatizador.model.dto.CobranzaRequestDTO;
import com.elias.automatizador.service.MotorCobranzaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cobranza")
@RequiredArgsConstructor
public class CobranzaApiController {

    private final MotorCobranzaService motorCobranzaService;

    @PostMapping("/ejecutar")
    public ResponseEntity<Map<String, String>> ejecutarCobranza(@RequestBody CobranzaRequestDTO request) {
        System.out.println("📥 Petición de cobranza recibida de n8n para: " + request.getNit());
        
        try {
            motorCobranzaService.procesarCobranza(request);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Cobranza procesada y correo enviado exitosamente"));
        } catch (Exception e) {
            System.err.println("❌ Error procesando endpoint de cobranza: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
