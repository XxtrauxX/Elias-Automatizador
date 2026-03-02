package com.elias.automatizador.scheduler;

import com.elias.automatizador.model.BotConfig;
import com.elias.automatizador.model.DebtInfo;
import com.elias.automatizador.repository.BotConfigRepository;
import com.elias.automatizador.repository.ProcesamientoLogRepository;
import com.elias.automatizador.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CobranzaScheduler {

    private final SharePointService sharePointService;
    private final SiigoClient siigoClient;
    private final ContactService contactService;
    private final RegistroCobroService loggingService;
    private final BotConfigRepository botConfigRepository;
    private final ProcesamientoLogRepository procesamientoLogRepository;

    @Value("${microsoft.graph.drive-id}")
    private String driveId;

    @Value("${microsoft.graph.excel-item-id}")
    private String itemId;

    // Ejecutar cada Miércoles a las 8:00 AM
    @Scheduled(cron = "0 0 8 * * WED")
    public void ejecutarBotCobranza() {
        System.out.println("🤖 --- INICIANDO BOT ELIAS (COBRANZA MIÉRCOLES) ---");

        try {
            // 1. Localizar la última hoja (Grupo Proyección usa la última pestaña para la
            // carga actual)
            String sheetName = sharePointService.findLastSheetName(driveId, itemId);
            if (sheetName == null) {
                System.err.println("❌ No se pudo localizar la última hoja del archivo.");
                return;
            }

            // 1.1 Control de Idempotencia: Verificar si ya procesamos esta hoja
            if (procesamientoLogRepository.existsByNombreHojaIgnoreCase(sheetName)) {
                System.out.println("⛔ La hoja [" + sheetName
                        + "] ya fue procesada anteriormente (IgnoreCase). Abortando.");
                return;
            }

            System.out.println("📌 Nueva hoja detectada: [" + sheetName + "]. Iniciando extracción...");

            // 2. Leer deudas (usando configuración dinámica)
            BotConfig config = botConfigRepository.findById(1).orElse(null);
            List<DebtInfo> debts;

            if (config != null) {
                System.out.println("⚙️ Usando configuración dinámica de BD: Hoja=" + config.getHojaNombre() +
                        ", Rango=" + config.getColumnaInicio() + config.getFilaInicio() + ":" + config.getColumnaFin());
                debts = sharePointService.readDebtsWithConfig(driveId, itemId, config, sheetName, null);
            } else {
                System.out.println("⚠️ No se encontró configuración en BD, usando valores por defecto.");
                debts = sharePointService.readDebtsFromSheet(driveId, itemId, sheetName);
            }
            System.out.println("📊 Deudas encontradas en Excel: " + debts.size());

            for (DebtInfo debt : debts) {
                // 3. Verificar saldo real en Siigo
                BigDecimal saldoPendiente = siigoClient.consultarSaldoPendiente(debt.getNit());

                if (saldoPendiente.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out
                            .println("✅ NIT " + debt.getNit() + " ya pagó (Saldo: " + saldoPendiente + "). Saltando.");
                    continue;
                }

                System.out.println("🔔 Procesando recordatorio para NIT " + debt.getNit() + " por $" + saldoPendiente);

                // 4. Buscar contacto (DB o Siigo)
                var contacto = contactService.obtenerContactoHibrido(debt.getNit());
                String canal = "WhatsApp"; // Por defecto

                if (contacto != null) {
                    // 5. Enviar mensaje (Lógica ya existente o a implementar en
                    // NotificationService)
                    // Por ahora simulamos el envío para completar el flujo
                    System.out.println("📱 Enviando WhatsApp a " + contacto.getCelular());

                    // Implementar delay para evitar spam
                    Thread.sleep(2000);

                    // 6. Escribir evidencia en Excel
                    sharePointService.writeEvidenceToSheet(driveId, itemId, sheetName, debt.getRowAddress(), canal);

                    // 7. Log en base de datos
                    loggingService.registrarCobro(debt.getNit(), contacto.getNombreEmpresa(), saldoPendiente, canal,
                            "EXITOSO");
                } else {
                    System.err.println("⚠️ No se encontró contacto para NIT " + debt.getNit());
                    loggingService.registrarCobro(debt.getNit(), "DESCONOCIDO", saldoPendiente, "NINGUNO", "FALLIDO");
                }
            }

            System.out.println("🚀 --- BOT ELIAS FINALIZADO CON ÉXITO ---");

        } catch (Exception e) {
            System.err.println("❌ ERROR CRÍTICO EN EL SCHEDULER: " + e.getMessage());
        }
    }
}
