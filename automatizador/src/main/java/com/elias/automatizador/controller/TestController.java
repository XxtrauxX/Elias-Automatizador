package com.elias.automatizador.controller;

import com.elias.automatizador.model.BotConfig;
import com.elias.automatizador.model.Contacto;
import com.elias.automatizador.model.ProcesamientoLog;
import com.elias.automatizador.repository.BotConfigRepository;
import com.elias.automatizador.repository.SiigoTokenRepository;
import com.elias.automatizador.repository.ProcesamientoLogRepository;
import com.elias.automatizador.service.ContactService;
import com.elias.automatizador.service.SharePointService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/test/siigo")
@RequiredArgsConstructor
public class TestController {

    private final ContactService contactService;
    private final SharePointService sharePointService;
    private final com.elias.automatizador.service.SiigoClient siigoClient;
    private final BotConfigRepository botConfigRepository;
    private final SiigoTokenRepository tokenRepository;
    private final ProcesamientoLogRepository procesamientoLogRepository;

    @Value("${microsoft.graph.drive-id:}")
    private String driveId;

    @Value("${microsoft.graph.excel-item-id:}")
    private String itemId;

    // Endpoint solicitado para comprobar saldo en Siigo
    @GetMapping("/comprobar-saldo/{nit}")
    public String comprobarSaldo(@PathVariable String nit) {
        try {
            java.math.BigDecimal saldo = siigoClient.consultarSaldoPendiente(nit);
            return "El saldo pendiente en Siigo para el NIT " + nit + " es: $" + saldo;
        } catch (Exception e) {
            return "Error al consultar saldo en Siigo: " + e.getMessage();
        }
    }

    // Endpoint existente para Siigo y MySQL
    @GetMapping("/cliente/{nit}")
    public Contacto probarConexion(@PathVariable String nit) {
        return contactService.obtenerContactoHibrido(nit);
    }

    // Nuevo endpoint para verificar lectura de NITs en Excel
    @GetMapping("/sharepoint/verificar-nits")
    public String verificarLecturaNits() {
        try {
            BotConfig config = botConfigRepository.findById(1).orElse(null);
            System.out.println("🚀 Iniciando identificación de hojas en SharePoint...");
            List<String> sheets = sharePointService.listWorksheets(driveId, itemId);
            System.out.println("📂 Hojas encontradas: " + sheets);

            String sheetName = sharePointService.findLastSheetName(driveId, itemId);
            System.out.println("📍 Última hoja detectada: [" + sheetName + "]");

            long tokenCount = tokenRepository != null ? tokenRepository.count() : -1;
            long processedSheetsCount = procesamientoLogRepository != null ? procesamientoLogRepository.count() : -1;
            boolean alreadyProcessed = procesamientoLogRepository.existsByNombreHoja(sheetName);

            sharePointService.readDebtsWithConfig(driveId, itemId, config, sheetName);

            // Guardar log si no existe (para que el usuario vea que funciona)
            if (!alreadyProcessed) {
                ProcesamientoLog log = new ProcesamientoLog();
                log.setNombreHoja(sheetName);
                procesamientoLogRepository.save(log);
                System.out.println("💾 Hoja [" + sheetName + "] registrada desde el TestController.");
            }

            return "Verificación terminada. \n" +
                    "Última hoja detectada: " + sheetName + "\n" +
                    "¿Ya procesada?: " + (alreadyProcessed ? "SÍ (Se saltará en el bot)" : "NO") + "\n" +
                    "Tokens en DB: " + (tokenCount == -1 ? "Error" : tokenCount) + "\n" +
                    "Hojas totales procesadas: " + (processedSheetsCount == -1 ? "Error" : processedSheetsCount) + "\n"
                    +
                    "Revisa la consola para más detalles.";
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

    // Nuevo endpoint para probar la extracción por lote dinámica
    @GetMapping("/sharepoint/procesar-lote")
    public String procesarLote() {
        try {
            String sheetName = sharePointService.findLastSheetName(driveId, itemId);
            if (sheetName == null) {
                return "Error: No se pudo detectar la última hoja.";
            }
            return sharePointService.processBatchExtraction(driveId, itemId, sheetName);
        } catch (Exception e) {
            return "Error en el procesamiento del lote: " + e.getMessage();
        }
    }
}