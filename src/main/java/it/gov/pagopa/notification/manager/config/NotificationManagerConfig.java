package it.gov.pagopa.notification.manager.config;

import it.gov.pagopa.notification.manager.connector.IOBackEndRestClient;
import it.gov.pagopa.notification.manager.connector.PdvDecryptRestClient;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeFeignRestClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(clients = {IOBackEndRestClient.class, PdvDecryptRestClient.class, InitiativeFeignRestClient.class})
public class NotificationManagerConfig {}
