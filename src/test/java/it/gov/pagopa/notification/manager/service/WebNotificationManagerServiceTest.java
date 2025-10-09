package it.gov.pagopa.notification.manager.service;

import it.gov.pagopa.notification.manager.config.EmailNotificationProperties;
import it.gov.pagopa.notification.manager.connector.EmailNotificationConnector;
import it.gov.pagopa.notification.manager.dto.EmailMessageDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationReminderQueueDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.EmailTemplates.EMAIL_OUTCOME_THREE_DAY_REMINDER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebNotificationManagerServiceImplTest {

    private EmailNotificationConnector emailNotificationConnector;

    private WebNotificationManagerServiceImpl service;

    @BeforeEach
    void setUp() {
        emailNotificationConnector = Mockito.mock(EmailNotificationConnector.class);
        EmailNotificationProperties emailNotificationProperties = Mockito.mock(EmailNotificationProperties.class);
        EmailNotificationProperties.Subject subjectProps = Mockito.mock(EmailNotificationProperties.Subject.class);

        when(emailNotificationProperties.getSubject()).thenReturn(subjectProps);
        when(subjectProps.getOkThreeDayReminder()).thenReturn("Il tuo bonus scade tra 3 giorni!");

        service = new WebNotificationManagerServiceImpl(emailNotificationConnector, emailNotificationProperties);
    }

    @Test
    void sendReminderMail_buildsEmailMessageAndSends() {
        NotificationReminderQueueDTO dto = Mockito.mock(NotificationReminderQueueDTO.class);
        when(dto.getName()).thenReturn("Mario");
        when(dto.getUserMail()).thenReturn("mario.rossi@example.com");
        when(dto.getUserId()).thenReturn("USER123");

        service.sendReminderMail(dto);

        ArgumentCaptor<EmailMessageDTO> captor = ArgumentCaptor.forClass(EmailMessageDTO.class);
        verify(emailNotificationConnector, times(1)).sendEmail(captor.capture());

        EmailMessageDTO sent = captor.getValue();
        assertNotNull(sent);
        assertEquals(EMAIL_OUTCOME_THREE_DAY_REMINDER, sent.getTemplateName(), "Template name errato");
        assertEquals("mario.rossi@example.com", sent.getRecipientEmail(), "Recipient errato");
        assertEquals("Il tuo bonus scade tra 3 giorni!", sent.getSubject(), "Subject errato");
        assertNull(sent.getSenderEmail(), "Sender email deve essere null");
        assertNull(sent.getContent(), "Content deve essere null");

        assertNotNull(sent.getTemplateValues(), "Template values null");
        assertEquals("Mario", sent.getTemplateValues().get("name"), "Placeholder name errato");
    }

    @Test
    void sendReminderMail_doesNotPropagateException() {
        NotificationReminderQueueDTO dto = Mockito.mock(NotificationReminderQueueDTO.class);
        when(dto.getName()).thenReturn("Luigi");
        when(dto.getSurname()).thenReturn("Bianchi");
        when(dto.getUserMail()).thenReturn("luigi.bianchi@example.com");
        when(dto.getUserId()).thenReturn("USER456");

        doThrow(new RuntimeException("SMTP down"))
                .when(emailNotificationConnector)
                .sendEmail(any(EmailMessageDTO.class));

        assertDoesNotThrow(() -> service.sendReminderMail(dto));

        verify(emailNotificationConnector, times(1)).sendEmail(any(EmailMessageDTO.class));
    }

    @Test
    void sendNotification_doesNotPropagateException() {
        NotificationReminderQueueDTO dto = Mockito.mock(NotificationReminderQueueDTO.class);
        when(dto.getUserId()).thenReturn("USER789");

        EmailMessageDTO toSend = EmailMessageDTO.builder()
                .templateName(EMAIL_OUTCOME_THREE_DAY_REMINDER)
                .recipientEmail("test@example.com")
                .subject("Subject")
                .build();

        doThrow(new RuntimeException("Generic error"))
                .when(emailNotificationConnector)
                .sendEmail(any(EmailMessageDTO.class));

        assertDoesNotThrow(() -> service.sendNotification(toSend, dto));
        verify(emailNotificationConnector, times(1)).sendEmail(any(EmailMessageDTO.class));
    }
}

