package com.elias.automatizador.service;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.elias.automatizador.model.BotConfig;
import com.elias.automatizador.model.DebtInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.models.WorkbookWorksheet;
import com.microsoft.kiota.RequestInformation;
import com.microsoft.kiota.HttpMethod;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
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
        String sheetName = sheetNameFromDB;

        // Si la hoja en DB es la por defecto o está vacía, usamos la dinámica del
        // miércoles
        if (sheetName == null || sheetName.trim().isEmpty() || sheetName.equalsIgnoreCase("Cartera")) {
            sheetName = fallbackSheetName;
        }

        String rangeAddress = config.getColumnaInicio() + config.getFilaInicio() + ":" + config.getColumnaFin() + "100";

        try {
            System.out.println("--- 🔍 LEYENDO DEUDAS DINÁMICAMENTE ---");
            System.out.println("📍 Config DB: [Hoja=" + sheetNameFromDB + ", Rango=" + config.getColumnaInicio()
                    + config.getFilaInicio() + ":" + config.getColumnaFin() + "]");
            System.out.println("📍 Usando Hoja: [" + sheetName + "]");
            System.out.println("📍 Rango final: [" + rangeAddress + "]");

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

            System.out.println("📊 Procesando " + values.size() + " filas del Excel...");
            for (int i = 0; i < values.size(); i++) {
                JsonArray row = values.get(i).getAsJsonArray();
                int endColIdx = config.getColumnaFin().toUpperCase().charAt(0)
                        - config.getColumnaInicio().toUpperCase().charAt(0);

                if (row.size() > endColIdx) {
                    JsonElement nitElem = row.get(0);
                    JsonElement amountElem = row.get(endColIdx);

                    String nit = nitElem.isJsonNull() ? "" : nitElem.getAsString().trim();
                    String amountStr = amountElem.isJsonNull() ? "" : amountElem.getAsString().trim();

                    if (!nit.isEmpty() && !nit.equals("0") && !amountStr.isEmpty()) {
                        try {
                            String cleanAmount = amountStr.replaceAll("[^\\d.]", "");
                            if (cleanAmount.isEmpty())
                                cleanAmount = "0";
                            BigDecimal amount = new BigDecimal(cleanAmount);

                            char nextCol = (char) (config.getColumnaFin().toUpperCase().charAt(0) + 1);
                            String evidenceCell = nextCol + String.valueOf(config.getFilaInicio() + i);

                            debts.add(new DebtInfo(nit, amount, evidenceCell));
                            System.out.println("✅ Encontrado: NIT=" + nit + " | Saldo=" + amount);
                        } catch (Exception ex) {
                        }
                    }
                }
            }
            System.out.println("🎯 Total deudas encontradas: " + debts.size());
        } catch (Exception e) {
            System.err.println("❌ Error en readDebtsWithConfig: " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                System.out.println("🔍 Hojas disponibles en el libro:");
                listWorksheets(driveId, itemId).forEach(name -> System.out.println("  - " + name));
            } else if (e.getMessage() != null && e.getMessage().contains("Token not found in the cache")) {
                System.err.println("🚨 Error de caché MSAL: Reiniciando cliente de Graph...");
                this.graphClient = null;
            }
        }
        return debts;
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
}
