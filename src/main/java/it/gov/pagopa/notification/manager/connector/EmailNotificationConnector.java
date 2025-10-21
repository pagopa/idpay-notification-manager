package it.gov.pagopa.notification.manager.connector;

import it.gov.pagopa.notification.manager.dto.EmailMessageDTO;
import org.springframework.http.ResponseEntity;

public interface EmailNotificationConnector {
    ResponseEntity<Void> sendEmail(EmailMessageDTO emailMessageDTO);
}
