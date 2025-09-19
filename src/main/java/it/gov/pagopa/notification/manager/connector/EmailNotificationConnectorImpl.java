package it.gov.pagopa.notification.manager.connector;

import it.gov.pagopa.notification.manager.dto.EmailMessageDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationConnectorImpl implements EmailNotificationConnector {

    private final EmailNotificationFeignClient emailNotificationFeignClient;

    public EmailNotificationConnectorImpl(EmailNotificationFeignClient emailNotificationFeignClient) {
        this.emailNotificationFeignClient = emailNotificationFeignClient;
    }

    @Override
    public ResponseEntity<Void> sendEmail(EmailMessageDTO emailMessageDTO) {
        return emailNotificationFeignClient.sendEmail(emailMessageDTO);
    }
}
