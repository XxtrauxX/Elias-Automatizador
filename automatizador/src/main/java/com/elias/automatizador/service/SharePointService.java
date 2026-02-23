package com.elias.automatizador.service;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.elias.automatizador.model.DebtInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.models.WorkbookWorksheet;
import com.microsoft.kiota.RequestInformation;
import com.microsoft.kiota.HttpMethod;
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

    private GraphServiceClient graphClient;

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

    public List<DebtInfo> readDebtsFromSheet(String driveId, String itemId, String sheetName) {
        List<DebtInfo> debts = new ArrayList<>();
        var client = getGraphClient();

        try {
            // Obtenemos los datos crudos como JSON para evitar problemas de tipos del SDK
            RequestInformation requestInfo = client.drives().byDriveId(driveId).items().byDriveItemId(itemId)
                    .workbook().worksheets().byWorkbookWorksheetId(sheetName)
                    .rangeWithAddress("A2:B100").toGetRequestInformation();

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

    public List<DebtInfo> verificarLecturaNits(String driveId, String itemId, String sheetName) {
        List<DebtInfo> debts = new ArrayList<>();
        var client = getGraphClient();

        try {
            System.out.println("--- 🔍 VERIFICANDO LECTURA DE NITS (Rango A8:K100) ---");
            RequestInformation requestInfo = client.drives().byDriveId(driveId).items().byDriveItemId(itemId)
                    .workbook().worksheets().byWorkbookWorksheetId(sheetName)
                    .rangeWithAddress("A8:K100").toGetRequestInformation();

            java.io.InputStream stream = client.getRequestAdapter().sendPrimitive(requestInfo, null,
                    java.io.InputStream.class);
            String jsonContent = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

            JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
            JsonArray values = root.getAsJsonArray("values");

            for (int i = 0; i < values.size(); i++) {
                JsonArray row = values.get(i).getAsJsonArray();
                // Col A (0) = NIT, Col B (1) = Nombre, Col K (10) = Saldo
                if (row.size() >= 11) {
                    JsonElement nitElem = row.get(0);
                    JsonElement nameElem = row.get(1);
                    JsonElement amountElem = row.get(10);

                    String nit = nitElem.isJsonNull() ? "" : nitElem.getAsString().trim();
                    String name = nameElem.isJsonNull() ? "" : nameElem.getAsString().trim();
                    String amountStr = amountElem.isJsonNull() ? "" : amountElem.getAsString().trim();

                    if (!nit.isEmpty() && !nit.equals("0") && !amountStr.isEmpty()) {
                        try {
                            // Limpiar el monto de caracteres no numéricos
                            String cleanAmount = amountStr.replaceAll("[^\\d.]", "");
                            if (cleanAmount.isEmpty())
                                cleanAmount = "0";
                            BigDecimal amount = new BigDecimal(cleanAmount);

                            System.out.println("📍 Fila " + (i + 8) + ": NIT=" + nit + " | Cliente=" + name
                                    + " | Saldo=" + amount);

                            debts.add(new DebtInfo(nit, amount, "L" + (i + 8))); // Escribiremos evidencia en Col L
                        } catch (Exception ex) {
                            // Fila no procesable
                        }
                    }
                }
            }
            System.out.println("✅ Verificación completada. Se encontraron " + debts.size() + " registros válidos.");
        } catch (Exception e) {
            System.err.println("❌ Error en verificarLecturaNits: " + e.getMessage());
            e.printStackTrace();
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
