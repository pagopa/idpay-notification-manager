package it.gov.pagopa.notification.manager.service;

import feign.FeignException;
import feign.Request;
import it.gov.pagopa.notification.manager.connector.EmailNotificationConnector;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.dto.EmailMessageDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationReminderQueueDTO;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeNotificationDTO;
import it.gov.pagopa.notification.manager.model.EmailNotificationContent;
import it.gov.pagopa.notification.manager.service.email.EmailNotificationContentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static it.gov.pagopa.notification.manager.enums.NotificationEvent.VOUCHER_EXPIRING;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebNotificationManagerServiceImplTest {

    @Mock
    private EmailNotificationConnector emailNotificationConnector;

    @Mock
    private EmailNotificationContentService emailNotificationContentService;
    @Mock
    private InitiativeRestConnector initiativeRestConnector;

    private WebNotificationManagerServiceImpl service;


    @BeforeEach
    void setUp() {
        service = new WebNotificationManagerServiceImpl(emailNotificationConnector, initiativeRestConnector, emailNotificationContentService);
    }

    @Test
    void sendReminderMail_buildsEmailMessageAndSends() {
        NotificationReminderQueueDTO dto = mock(NotificationReminderQueueDTO.class);
        when(dto.getName()).thenReturn("Mario");
        when(dto.getUserMail()).thenReturn("mario.rossi@example.com");
        when(dto.getUserId()).thenReturn("USER123");
        when(dto.getVoucherEndDate()).thenReturn(LocalDate.now());
        when(dto.getInitiativeId()).thenReturn("initiative123");
        InitiativeNotificationDTO mockInitiative = new InitiativeNotificationDTO();
        mockInitiative.setEmailFlux("DEC26");
        when(initiativeRestConnector.getInitiativeDetailInfo(anyString())).thenReturn(mockInitiative);
        when(emailNotificationContentService.resolveProfileAndFind("DEC26", VOUCHER_EXPIRING))
                .thenReturn(Optional.of(new EmailNotificationContent(
                        "Il tuo bonus sta per scadere!", "Email_DEC26/ThreeDayReminder")));

        service.sendReminderMail(dto);

        ArgumentCaptor<EmailMessageDTO> captor = ArgumentCaptor.forClass(EmailMessageDTO.class);
        verify(emailNotificationConnector, times(1)).sendEmail(captor.capture());

        EmailMessageDTO sent = captor.getValue();
        assertNotNull(sent);
        assertEquals("Email_DEC26/ThreeDayReminder", sent.getTemplateName(), "Template name errato");
        assertEquals("mario.rossi@example.com", sent.getRecipientEmail(), "Recipient errato");
        assertEquals("Il tuo bonus sta per scadere!", sent.getSubject(), "Subject errato");
        assertNull(sent.getSenderEmail(), "Sender email deve essere null");
        assertNull(sent.getContent(), "Content deve essere null");

        assertNotNull(sent.getTemplateValues(), "Template values null");
        assertEquals("Mario", sent.getTemplateValues().get("name"), "Placeholder name errato");
    }

    @Test
    void sendReminderMail_doesNotPropagateException() {
        NotificationReminderQueueDTO dto = mock(NotificationReminderQueueDTO.class);

        when(dto.getVoucherEndDate()).thenReturn(LocalDate.now());
        when(dto.getInitiativeId()).thenReturn("initiative123");
        InitiativeNotificationDTO mockInitiative = new InitiativeNotificationDTO();
        mockInitiative.setEmailFlux("DEC26");
        when(initiativeRestConnector.getInitiativeDetailInfo(anyString())).thenReturn(mockInitiative);
        when(emailNotificationContentService.resolveProfileAndFind("DEC26", VOUCHER_EXPIRING))
                .thenReturn(Optional.of(new EmailNotificationContent(
                        "Il tuo bonus sta per scadere!", "Email_DEC26/ThreeDayReminder")));

        doThrow(new RuntimeException("SMTP down"))
                .when(emailNotificationConnector)
                .sendEmail(any(EmailMessageDTO.class));

        assertDoesNotThrow(() -> service.sendReminderMail(dto));

        verify(emailNotificationConnector, times(1)).sendEmail(any(EmailMessageDTO.class));
    }



    @Test
    void sendNotification_doesNotPropagateException() {
        NotificationReminderQueueDTO dto = mock(NotificationReminderQueueDTO.class);
        when(dto.getUserId()).thenReturn("USER789");

        EmailMessageDTO toSend = EmailMessageDTO.builder()
                .templateName("test-templateName")
                .recipientEmail("test@example.com")
                .subject("Subject")
                .build();

        doThrow(new RuntimeException("Generic error"))
                .when(emailNotificationConnector)
                .sendEmail(any(EmailMessageDTO.class));

        assertDoesNotThrow(() -> service.sendNotification(toSend, dto));
        verify(emailNotificationConnector, times(1)).sendEmail(any(EmailMessageDTO.class));
    }

    @Test
    void sendReminderMail_whenEmailFluxIsNull_usesDefaultContent() {
        NotificationReminderQueueDTO dto = mock(NotificationReminderQueueDTO.class);
        when(dto.getVoucherEndDate()).thenReturn(LocalDate.now());
        when(dto.getInitiativeId()).thenReturn("initiative123");
        when(dto.getName()).thenReturn("Mario");
        when(dto.getUserMail()).thenReturn("mario@example.com");

        when(initiativeRestConnector.getInitiativeDetailInfo(anyString())).thenReturn(new InitiativeNotificationDTO());
        when(emailNotificationContentService.resolveProfileAndFind(null, VOUCHER_EXPIRING))
                .thenReturn(Optional.of(new EmailNotificationContent(
                        "Il tuo bonus sta per scadere!", "Email_DEFAULT/ThreeDayReminder")));

        service.sendReminderMail(dto);

        ArgumentCaptor<EmailMessageDTO> captor = ArgumentCaptor.forClass(EmailMessageDTO.class);
        verify(emailNotificationConnector).sendEmail(captor.capture());

        assertEquals("Email_DEFAULT/ThreeDayReminder", captor.getValue().getTemplateName());
    }

    @Test
    void sendReminderMail_whenContentMissing_doesNotSend() {
        NotificationReminderQueueDTO dto = mock(NotificationReminderQueueDTO.class);
        when(dto.getInitiativeId()).thenReturn("initiative123");
        InitiativeNotificationDTO initiative = new InitiativeNotificationDTO();
        initiative.setEmailFlux("DEC26");
        when(initiativeRestConnector.getInitiativeDetailInfo("initiative123")).thenReturn(initiative);
        when(emailNotificationContentService.resolveProfileAndFind("DEC26", VOUCHER_EXPIRING))
                .thenReturn(Optional.empty());

        service.sendReminderMail(dto);

        verify(emailNotificationConnector, never()).sendEmail(any());
    }

    @Test
    void sendReminderMail_whenInitiativeFetchFails_doesNotResolveOrSend() {
        NotificationReminderQueueDTO dto = mock(NotificationReminderQueueDTO.class);
        when(dto.getInitiativeId()).thenReturn("initiative123");
        when(initiativeRestConnector.getInitiativeDetailInfo("initiative123"))
                .thenThrow(new FeignException.InternalServerError(
                        "Internal Server Error", mock(Request.class), new byte[0], null));

        service.sendReminderMail(dto);

        verifyNoInteractions(emailNotificationContentService, emailNotificationConnector);
    }
}
