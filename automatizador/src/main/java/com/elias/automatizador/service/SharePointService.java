package com.elias.automatizador.service;

import com.azure.identity.DeviceCodeCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.models.Drive;
import com.microsoft.graph.models.Site;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SharePointService {

    @Value("${microsoft.graph.tenant-id}")
    private String tenantId;

    @Value("${microsoft.graph.client-id}")
    private String clientId;

    public void probarConexionDelegada() {
        try {
            System.out.println("--- 🚀 INTENTANDO ACCESO DIRECTO A PROYECCIÓN CONTABLE ---");

            // Reducimos scopes al mínimo absoluto para evitar el bloqueo de admin
            String[] scopes = new String[] { "Files.Read", "User.Read", "offline_access" };

            var credential = new DeviceCodeCredentialBuilder()
                    .tenantId(tenantId)
                    .clientId(clientId)
                    .challengeConsumer(challenge -> {
                        System.out.println("\n************************************************************");
                        System.out.println("🔑 NUEVO CÓDIGO DE VALIDACIÓN:");
                        System.out.println(challenge.getMessage());
                        System.out.println("************************************************************\n");
                    })
                    .build();

            var graphClient = new GraphServiceClient(credential, scopes);

            // Intentamos obtener el sitio directamente por su nombre de host y ruta
            // Esto evita pedir permiso para 'Listar todos los sitios'
            String sitePath = "grupoproyeccion.sharepoint.com:/sites/ProyeccionContable_Clientes";
            var site = graphClient.sites().bySiteId(sitePath).get();

            if (site != null) {
                System.out.println("✅ SITIO LOCALIZADO: " + site.getDisplayName());
                
                // Listamos las bibliotecas de este sitio específico
                var drives = graphClient.sites().bySiteId(site.getId()).drives().get();
                
                if (drives != null && drives.getValue() != null) {
                    for (Drive drive : drives.getValue()) {
                        System.out.println("   📁 Biblioteca: " + drive.getName());
                        System.out.println("   🆔 ID PARA PROPERTIES: " + drive.getId());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ EL MURO DE ADMIN SIGUE ACTIVO: " + e.getMessage());
            System.out.println("\n💡 OSCAR, si esto falla, debes pedirle al administrador global");
            System.out.println("que habilite 'User consent for apps' en la configuración de Entra ID.");
        }
    }
}