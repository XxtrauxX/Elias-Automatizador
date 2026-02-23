package com.elias.automatizador.controller;

import com.elias.automatizador.model.Contacto;
import com.elias.automatizador.service.ContactService;
import com.elias.automatizador.service.SharePointService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test/siigo")
@RequiredArgsConstructor
public class TestController {

    private final ContactService contactService;
    private final SharePointService sharePointService;

    @Value("${microsoft.graph.drive-id:}")
    private String driveId;

    @Value("${microsoft.graph.excel-item-id:}")
    private String itemId;

    // Endpoint existente para Siigo y MySQL
    @GetMapping("/cliente/{nit}")
    public Contacto probarConexion(@PathVariable String nit) {
        return contactService.obtenerContactoHibrido(nit);
    }

    // Nuevo endpoint para verificar lectura de NITs en Excel
    @GetMapping("/sharepoint/verificar-nits")
    public String verificarLecturaNits() {
        try {
            String sheetName = sharePointService.findWednesdaySheet(driveId, itemId);
            if (sheetName == null)
                return "No se encontró la hoja de cálculo.";

            sharePointService.verificarLecturaNits(driveId, itemId, sheetName);
            return "Verificación de NITs iniciada. Revisa la consola para ver los resultados de la hoja: " + sheetName;
        } catch (Exception e) {
            return "Error en la verificación: " + e.getMessage();
        }
    }

    // Nuevo endpoint para autorizar SharePoint en modo Daemon
    @GetMapping("/sharepoint/autorizar")
    public String autorizarSharePoint() {
        try {
            sharePointService.probarConexionDaemon();
            return "Proceso de conexión Daemon iniciado. Revisa la consola para ver los resultados.";
        } catch (Exception e) {
            return "Error al intentar conectar con SharePoint: " + e.getMessage();
        }
    }
}