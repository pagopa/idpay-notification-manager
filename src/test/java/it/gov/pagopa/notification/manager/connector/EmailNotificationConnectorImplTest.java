package it.gov.pagopa.notification.manager.connector;

import it.gov.pagopa.notification.manager.dto.EmailMessageDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailNotificationConnectorImplTest {

    @Mock
    private EmailNotificationFeignClient feignClient;

    @InjectMocks
    private EmailNotificationConnectorImpl connector;

    @Test
    void sendEmail_shouldDelegateToFeignClient() {
        EmailMessageDTO email = EmailMessageDTO.builder()
                .recipientEmail("test@pagopa.it")
                .senderEmail("noreply@pagopa.it")
                .subject("Test")
                .content("Body")
                .build();

        when(feignClient.sendEmail(email)).thenReturn(ResponseEntity.ok().build());

        ResponseEntity<Void> response = connector.sendEmail(email);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(feignClient).sendEmail(email);
    }
}
