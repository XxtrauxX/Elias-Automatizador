package com.elias.automatizador.controller;

import com.elias.automatizador.model.Contacto;
import com.elias.automatizador.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/contacto")
@RequiredArgsConstructor
public class ContactoApiController {

    private final ContactService contactService;

    /**
     * Consulta información de un contacto por su NIT.
     * Implementa lógica híbrida: busca en MySQL primero y luego en Siigo.
     * Ideal para integraciones con n8n u otros agentes de IA.
     * 
     * @param nit Número de Identificación Tributaria
     * @return ResponseEntity con el objeto Contacto o JSON de error específico
     */
    @GetMapping("/{nit}")
    public ResponseEntity<Object> obtenerContacto(@PathVariable String nit) {
        try {
            Contacto contacto = contactService.obtenerContactoHibrido(nit);

            if (contacto != null) {
                return ResponseEntity.ok(contacto);
            } else {
                // Respuesta personalizada para 404 solicitada por el usuario
                Map<String, String> errorDetails = new HashMap<>();
                errorDetails.put("status", "not_found");
                errorDetails.put("message", "NIT no registrado en DB ni Siigo");
                return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            // Manejo de errores generales de servidor
            Map<String, String> errorDetails = new HashMap<>();
            errorDetails.put("status", "server_error");
            errorDetails.put("message", "Error interno al consultar el NIT: " + e.getMessage());
            return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
