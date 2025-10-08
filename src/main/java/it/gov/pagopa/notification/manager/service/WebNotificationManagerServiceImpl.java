package it.gov.pagopa.notification.manager.service;

import it.gov.pagopa.notification.manager.config.EmailNotificationProperties;
import it.gov.pagopa.notification.manager.connector.EmailNotificationConnector;
import it.gov.pagopa.notification.manager.dto.EmailMessageDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationReminderQueueDTO;
import it.gov.pagopa.notification.manager.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.EmailTemplates.EMAIL_OUTCOME_THREE_DAY_REMINDER;

@Service
@Slf4j
public class WebNotificationManagerServiceImpl implements  WebNotificationManagerService{

    private final EmailNotificationConnector emailNotificationConnector;
    private final EmailNotificationProperties emailNotificationProperties;

    public WebNotificationManagerServiceImpl(EmailNotificationConnector emailNotificationConnector, EmailNotificationProperties emailNotificationProperties) {
        this.emailNotificationConnector = emailNotificationConnector;
        this.emailNotificationProperties = emailNotificationProperties;
    }

    @Override
    public void sendReminderMail(NotificationReminderQueueDTO notificationQueueDTO) {
        Map<String, String> templateValues = new HashMap<>();
        templateValues.put("name", Utils.getNameSurname(notificationQueueDTO));

        EmailMessageDTO emailMessageDTO = EmailMessageDTO.builder()
                .templateName(EMAIL_OUTCOME_THREE_DAY_REMINDER)
                .recipientEmail(notificationQueueDTO.getUserMail())
                .senderEmail(null)
                .templateValues(templateValues)
                .subject(emailNotificationProperties.getSubject().getOkThreeDayReminder())
                .content(null)
                .build();
        sendNotification(emailMessageDTO, notificationQueueDTO);
    }

    void sendNotification(EmailMessageDTO notificationToSend, NotificationReminderQueueDTO notificationQueueDTO) {
        try {
            emailNotificationConnector.sendEmail(notificationToSend);
        } catch (Exception e) {
            log.error("[NOTIFY] Failed to send email notification for user {}", notificationQueueDTO.getUserId(), e);
        }
    }
}
