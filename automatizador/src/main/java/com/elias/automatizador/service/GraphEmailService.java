package com.elias.automatizador.service;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.EmailAddress;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.item.sendmail.SendMailPostRequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GraphEmailService {

    @Value("${microsoft.graph.tenant-id}")
    private String tenantId;

    @Value("${microsoft.graph.client-id}")
    private String clientId;

    @Value("${microsoft.graph.client-secret}")
    private String clientSecret;

    @Value("${MAIL_SENDER_ADDRESS:odiaz@grupoproyeccion.com}")
    private String senderAddress;

    private GraphServiceClient graphClient;

    private GraphServiceClient getGraphClient() {
        if (this.graphClient == null) {
            if (tenantId == null || tenantId.isEmpty() || clientId == null || clientId.isEmpty() || clientSecret == null || clientSecret.isEmpty()) {
                throw new IllegalStateException("Las credenciales de Microsoft Graph no están configuradas correctamente en el entorno (tenant, client_id, client_secret).");
            }
            ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                    .tenantId(tenantId)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .build();

            this.graphClient = new GraphServiceClient(credential, new String[]{"https://graph.microsoft.com/.default"});
        }
        return this.graphClient;
    }

    public void sendEmail(String toAddress, String subject, String htmlContent) {
        try {
            Message message = new Message();
            message.setSubject(subject);

            ItemBody body = new ItemBody();
            body.setContentType(BodyType.Html);
            body.setContent(htmlContent);
            message.setBody(body);

            EmailAddress emailAddress = new EmailAddress();
            emailAddress.setAddress(toAddress);

            Recipient toRecipient = new Recipient();
            toRecipient.setEmailAddress(emailAddress);

            message.setToRecipients(Collections.singletonList(toRecipient));

            SendMailPostRequestBody sendMailPostRequestBody = new SendMailPostRequestBody();
            sendMailPostRequestBody.setMessage(message);
            sendMailPostRequestBody.setSaveToSentItems(true);

            getGraphClient().users().byUserId(senderAddress)
                    .sendMail()
                    .post(sendMailPostRequestBody);

            System.out.println("✅ Correo enviado exitosamente a: " + toAddress + " desde " + senderAddress);

        } catch (Exception e) {
            System.err.println("❌ Error enviando correo vía Graph API: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error enviando correo: " + e.getMessage());
        }
    }
}
