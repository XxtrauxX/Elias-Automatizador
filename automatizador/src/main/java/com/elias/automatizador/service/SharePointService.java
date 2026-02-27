package com.elias.automatizador.service;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.elias.automatizador.model.BotConfig;
import com.elias.automatizador.model.DebtInfo;
import com.elias.automatizador.model.ExtraccionRegistro;
import com.elias.automatizador.repository.ExtraccionRegistroRepository;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.models.WorkbookWorksheet;
import com.microsoft.kiota.RequestInformation;
import com.microsoft.kiota.HttpMethod;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SharePointService {

    @Value("${microsoft.graph.tenant-id}")
    private String tenantId;

    @Value("${microsoft.graph.client-id}")
    private String clientId;

    @Value("${microsoft.graph.client-secret}")
    private String clientSecret;

    @Value("${microsoft.graph.drive-id}")
    private String driveId;

    @Value("${microsoft.graph.excel-item-id}")
    private String itemId;

    private final ExtraccionRegistroRepository extraccionRegistroRepository;

    private GraphServiceClient graphClient;

    @PostConstruct
    public void init() {
        System.out.println("🔧 SharePointService cargado. DriveID: " + (driveId != null ? "PRESENTE" : "MISSING")
                + " | ItemID: " + (itemId != null ? "PRESENTE" : "MISSING"));
    }

    private GraphServiceClient getGraphClient() {
        if (graphClient == null) {
            String[] scopes = new String[] { "https://graph.microsoft.com/.default" };
            var credential = new ClientSecretCredentialBuilder()
                    .tenantId(tenantId)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .build();
            graphClient = new GraphServiceClient(credential, scopes);
        }
        return graphClient;
    }

    public void probarConexionDaemon() {
        try {
            System.out.println("--- 🚀 PROBANDO CONEXIÓN DAEMON ---");
            var client = getGraphClient();
            var drives = client.drives().get();
            if (drives != null && drives.getValue() != null && !drives.getValue().isEmpty()) {
                System.out.println("✅ Conexión exitosa. Drive: " + drives.getValue().get(0).getName());
            }
        } catch (Exception e) {
            System.err.println("❌ Fallo en conexión Daemon: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String findLastSheetName(String driveId, String itemId) {
        var client = getGraphClient();
        try {
            var worksheets = client.drives().byDriveId(driveId).items().byDriveItemId(itemId).workbook().worksheets()
                    .get();

            if (worksheets != null && worksheets.getValue() != null && !worksheets.getValue().isEmpty()) {
                // Retornar el nombre de la última hoja
                return worksheets.getValue().get(worksheets.getValue().size() - 1).getName();
            }
        } catch (Exception e) {
            System.err.println("❌ Error buscando la última hoja: " + e.getMessage());
        }
        return null;
    }

    public String findWednesdaySheet(String driveId, String itemId) {
        var client = getGraphClient();
        String expectedName = LocalDate.now().format(DateTimeFormatter.ofPattern("d-MMMM", new Locale("es", "ES")))
                .toLowerCase();

        try {
            var worksheets = client.drives().byDriveId(driveId).items().byDriveItemId(itemId).workbook().worksheets()
                    .get();

            if (worksheets != null && worksheets.getValue() != null) {
                for (WorkbookWorksheet sheet : worksheets.getValue()) {
                    if (sheet.getName().toLowerCase().contains(expectedName)) {
                        return sheet.getName();
                    }
                }
                return worksheets.getValue().get(worksheets.getValue().size() - 1).getName();
            }
        } catch (Exception e) {
            System.err.println("❌ Error buscando hoja: " + e.getMessage());
        }
        return null;
    }

    public List<String> listWorksheets(String driveId, String itemId) {
        List<String> sheetNames = new ArrayList<>();
        var client = getGraphClient();
        try {
            var worksheets = client.drives().byDriveId(driveId).items().byDriveItemId(itemId).workbook().worksheets()
                    .get();
            if (worksheets != null && worksheets.getValue() != null) {
                for (WorkbookWorksheet sheet : worksheets.getValue()) {
                    sheetNames.add(sheet.getName());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error enlistando hojas: " + e.getMessage());
        }
        return sheetNames;
    }

    public List<DebtInfo> readDebtsFromSheet(String driveId, String itemId, String sheetName) {
        List<DebtInfo> debts = new ArrayList<>();
        var client = getGraphClient();

        try {
            // Obtenemos los datos crudos como JSON para evitar problemas de tipos del SDK
            // Usamos la configuración dinámica si se provee
            String range = "A2:B100";
            RequestInformation requestInfo = client.drives().byDriveId(driveId).items().byDriveItemId(itemId)
                    .workbook().worksheets().byWorkbookWorksheetId(sheetName)
                    .rangeWithAddress(range).toGetRequestInformation();

            // Usamos el requestAdapter para enviar la petición y recibir un InputStream o
            // String
            java.io.InputStream stream = client.getRequestAdapter().sendPrimitive(requestInfo, null,
                    java.io.InputStream.class);
            String jsonContent = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

            JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
            JsonArray values = root.getAsJsonArray("values");

            for (int i = 0; i < values.size(); i++) {
                JsonArray row = values.get(i).getAsJsonArray();
                if (row.size() >= 2) {
                    JsonElement nitElem = row.get(0);
                    JsonElement amountElem = row.get(1);

                    String nit = nitElem.isJsonNull() ? "" : nitElem.getAsString();
                    String amountStr = amountElem.isJsonNull() ? "" : amountElem.getAsString();

                    if (!nit.isEmpty() && !amountStr.isEmpty()) {
                        try {
                            BigDecimal amount = new BigDecimal(amountStr.replaceAll("[^\\d.]", ""));
                            debts.add(new DebtInfo(nit, amount, "C" + (i + 2)));
                        } catch (Exception ex) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error leyendo datos de la hoja: " + e.getMessage());
        }
        return debts;
    }

    public List<DebtInfo> readDebtsWithConfig(String driveId, String itemId, BotConfig config,
            String fallbackSheetName) {
        List<DebtInfo> debts = new ArrayList<>();
        var client = getGraphClient();

        String sheetNameFromDB = config.getHojaNombre();
        String sheetName = (sheetNameFromDB == null || sheetNameFromDB.trim().isEmpty()
                || sheetNameFromDB.equalsIgnoreCase("Cartera"))
                        ? fallbackSheetName
                        : sheetNameFromDB;

        String batchId = "BATCH-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));

        try {
            System.out.println("--- 🔍 LEYENDO DEUDAS DINÁMICAMENTE (CON PERSISTENCIA) ---");
            System.out.println("📍 Hoja: [" + sheetName + "] | Lote: [" + batchId + "]");

            // 1. Detectar última fila dinámicamente
            int lastRow = getLastRow(driveId, itemId, sheetName);
            if (lastRow < 8) {
                System.out.println("⚠️ No hay datos suficientes (última fila: " + lastRow + ").");
                return debts;
            }

            // 2. Definir rango A8:O{lastRow}
            String rangeAddress = "A8:O" + lastRow;
            System.out.println("📍 Rango final detectado: [" + rangeAddress + "]");

            RequestInformation requestInfo = client.drives().byDriveId(driveId).items().byDriveItemId(itemId)
                    .workbook().worksheets().byWorkbookWorksheetId(sheetName)
                    .rangeWithAddress(rangeAddress).toGetRequestInformation();

            java.io.InputStream stream = client.getRequestAdapter().sendPrimitive(requestInfo, null,
                    java.io.InputStream.class);
            String jsonContent = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

            JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
            JsonArray values = root.getAsJsonArray("values");

            if (values == null || values.isEmpty()) {
                System.out.println("⚠️ No se encontraron datos en el rango especificado.");
                return debts;
            }

            List<ExtraccionRegistro> registrosParaPersistir = new ArrayList<>();
            System.out.println("📊 Procesando " + values.size() + " filas del Excel...");

            for (int i = 0; i < values.size(); i++) {
                JsonArray row = values.get(i).getAsJsonArray();

                // Serialización Estructurada: Objeto JSON con llaves A-O
                JsonObject rowMetadata = new JsonObject();
                for (int j = 0; j < 15; j++) {
                    char colLetter = (char) ('A' + j);
                    String colKey = String.valueOf(colLetter);

                    if (j < row.size()) {
                        JsonElement elem = row.get(j);
                        rowMetadata.addProperty(colKey, elem.isJsonNull() ? "" : elem.getAsString().trim());
                    } else {
                        rowMetadata.addProperty(colKey, "");
                    }
                }

                String nit = rowMetadata.get("A").getAsString(); // Columna A
                String montoVencidoStr = rowMetadata.get("M").getAsString(); // Columna M

                if (nit == null || nit.isEmpty() || nit.equals("0")) {
                    continue;
                }

                BigDecimal amount = BigDecimal.ZERO;
                try {
                    String cleanAmount = montoVencidoStr.replaceAll("[^\\d.]", "");
                    if (!cleanAmount.isEmpty()) {
                        amount = new BigDecimal(cleanAmount);
                    }
                } catch (Exception ex) {
                    // Ignorar error de parseo
                }

                // Generar dirección de celda para evidencia (Columna P es el siguiente a O)
                String evidenceCell = "P" + (8 + i);
                debts.add(new DebtInfo(nit, amount, evidenceCell));

                // Preparar para persistir en BD
                ExtraccionRegistro registro = ExtraccionRegistro.builder()
                        .batchId(batchId)
                        .nit(nit)
                        .montoVencido(amount)
                        .metadata(new com.google.gson.Gson().toJson(rowMetadata))
                        .build();
                registrosParaPersistir.add(registro);

                System.out.println("✅ Encontrado: NIT=" + nit + " | Saldo=" + amount);
            }

            // 3. Persistencia por lote en BD
            if (!registrosParaPersistir.isEmpty()) {
                extraccionRegistroRepository.saveAll(registrosParaPersistir);
                System.out.println("[AUDITORÍA] Lote " + batchId + " persistido en DB. " + registrosParaPersistir.size()
                        + " registros.");
            }

            System.out.println("🎯 Total deudas encontradas: " + debts.size());
        } catch (Exception e) {
            System.err.println("❌ Error en readDebtsWithConfig: " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("Token not found")) {
                this.graphClient = null;
            }
        }
        return debts;
    }

    private int getLastRow(String driveId, String itemId, String sheetName) {
        try {
            var client = getGraphClient();
            RequestInformation usedRangeRequest = client.drives().byDriveId(driveId).items().byDriveItemId(itemId)
                    .workbook().worksheets().byWorkbookWorksheetId(sheetName)
                    .usedRange().toGetRequestInformation();

            java.io.InputStream stream = client.getRequestAdapter().sendPrimitive(usedRangeRequest, null,
                    java.io.InputStream.class);
            String json = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String address = root.get("address").getAsString();
            String rangePart = address.contains("!") ? address.split("!")[1] : address;
            String lastRowStr = rangePart.replaceAll("[^0-9]+$", "").replaceAll("^.*:", "").replaceAll("[^0-9]", "");
            return Integer.parseInt(lastRowStr);
        } catch (Exception e) {
            System.err.println("❌ Error detectando última fila: " + e.getMessage());
            return 100; // Fallback seguro
        }
    }

    public void writeEvidenceToSheet(String driveId, String itemId, String sheetName, String cellAddress,
            String channel) {
        var client = getGraphClient();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String message = "✅ Enviado por " + channel + " el " + date;

        try {
            // Construimos el cuerpo del PATCH manualmente
            String body = "{\"values\": [[\"" + message + "\"]]}";

            RequestInformation requestInfo = client.drives().byDriveId(driveId).items().byDriveItemId(itemId)
                    .workbook().worksheets().byWorkbookWorksheetId(sheetName)
                    .rangeWithAddress(cellAddress).toGetRequestInformation();

            requestInfo.httpMethod = HttpMethod.PATCH;
            requestInfo.setContentFromScalar(client.getRequestAdapter(), "application/json", body);

            client.getRequestAdapter().sendPrimitive(requestInfo, null, Void.class);

            System.out.println("📝 Evidencia escrita exitosamente en " + cellAddress);
        } catch (Exception e) {
            System.err.println("❌ Error escribiendo evidencia en Excel: " + e.getMessage());
        }
    }

    /**
     * Procesa una extracción por lote desde la fila 8 hasta la última con contenido
     * (UsedRange).
     * Extrae columnas A-O y persiste en base de datos.
     */
    public String processBatchExtraction(String driveId, String itemId, String sheetName) {
        BotConfig dummyConfig = new BotConfig();
        dummyConfig.setHojaNombre(sheetName);
        dummyConfig.setFilaInicio(8);
        dummyConfig.setColumnaInicio("A");
        dummyConfig.setColumnaFin("O");

        List<DebtInfo> result = readDebtsWithConfig(driveId, itemId, dummyConfig, sheetName);
        return "[TEST] Proceso completado. Registros en lista: " + result.size();
    }
}
