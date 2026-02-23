package com.elias.automatizador.controller;

import com.elias.automatizador.model.Contacto;
import com.elias.automatizador.service.ContactService;
import com.elias.automatizador.service.SharePointService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test/siigo")
@RequiredArgsConstructor
public class TestController {

    private final ContactService contactService;
    private final SharePointService sharePointService;

    // Endpoint existente para Siigo y MySQL
    @GetMapping("/cliente/{nit}")
    public Contacto probarConexion(@PathVariable String nit) {
        return contactService.obtenerContactoHibrido(nit);
    }

    // Nuevo endpoint para autorizar SharePoint y listar carpetas que Si lo logramos somos unos cracks increibles
    @GetMapping("/sharepoint/autorizar")
    public String autorizarSharePoint() {
        try {
            sharePointService.probarConexionDelegada();
            return "Proceso de login iniciado. Por favor, revisa tu navegador para autorizar y luego la consola de VS Code.";
        } catch (Exception e) {
            return "Error al intentar conectar con SharePoint: " + e.getMessage();
        }
    }
}