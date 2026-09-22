package it.gov.pagopa.notification.manager.service;

import feign.FeignException;
import it.gov.pagopa.notification.manager.config.EmailNotificationProperties;
import it.gov.pagopa.notification.manager.connector.EmailNotificationConnector;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.dto.EmailMessageDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationReminderQueueDTO;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeNotificationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.EmailTemplates.EMAIL_OUTCOME_THREE_DAY_REMINDER;

@Service
@Slf4j
public class WebNotificationManagerServiceImpl implements  WebNotificationManagerService{

    private final EmailNotificationConnector emailNotificationConnector;
    private final EmailNotificationProperties emailNotificationProperties;
    private final InitiativeRestConnector initiativeRestConnector;

    public WebNotificationManagerServiceImpl(EmailNotificationConnector emailNotificationConnector,
                                             EmailNotificationProperties emailNotificationProperties,
                                             InitiativeRestConnector initiativeRestConnector
    ) {
        this.emailNotificationConnector = emailNotificationConnector;
        this.emailNotificationProperties = emailNotificationProperties;
        this.initiativeRestConnector = initiativeRestConnector;
    }

    @Override
    public void sendReminderMail(NotificationReminderQueueDTO notificationQueueDTO) {
        long startTime = System.currentTimeMillis();
        String sanitizedUserId = sanitizeString(notificationQueueDTO.getUserId());
        String sanitizedInitiativeId = sanitizeString(notificationQueueDTO.getUserId());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy").withLocale(Locale.ITALIAN);

        InitiativeNotificationDTO initiativeNotificationDTO = null;
        try {
            initiativeNotificationDTO = initiativeRestConnector.getInitiativeDetailInfo(notificationQueueDTO.getInitiativeId());
        } catch (FeignException e) {
            log.error("[NOTIFY] Failed to send email notification for user {} and initiative {}", sanitizedUserId, sanitizedInitiativeId, e);
            performanceLog(startTime);

        }

        Map<String, String> templateValues = new HashMap<>();
        templateValues.put("name", notificationQueueDTO.getName());
        templateValues.put("voucherEndDate", notificationQueueDTO.getVoucherEndDate().format(formatter));

        EmailMessageDTO emailMessageDTO = EmailMessageDTO.builder()
                .templateName(String.format(EMAIL_OUTCOME_THREE_DAY_REMINDER,
                        initiativeNotificationDTO != null ? initiativeNotificationDTO.getEmailFlux() : ""))
                .recipientEmail(notificationQueueDTO.getUserMail())
                .senderEmail(null)
                .templateValues(templateValues)
                .subject(emailNotificationProperties.getSubject().getOkThreeDayReminder())
                .content(null)
                .build();
        sendNotification(emailMessageDTO, notificationQueueDTO);
    }

    void sendNotification(EmailMessageDTO notificationToSend, NotificationReminderQueueDTO notificationQueueDTO) {
        long startTime = System.currentTimeMillis();
        String sanitizedUserId = sanitizeString(notificationQueueDTO.getUserId());
        String sanitizedInitiativeId = sanitizeString(notificationQueueDTO.getUserId());
        try {
            emailNotificationConnector.sendEmail(notificationToSend);
            performanceLog(startTime);
            log.info("[NOTIFY] ReminderMail sent to user {} and initiative {}", sanitizedUserId, sanitizedInitiativeId);
        } catch (Exception e) {
            log.error("[NOTIFY] Failed to send email notification for user {} and initiative {}", sanitizedUserId, sanitizedInitiativeId, e);
            performanceLog(startTime);
        }
    }

    private void performanceLog(long startTime) {
        performanceLog(startTime, "NOTIFY");
    }

    private void performanceLog(long startTime, String flowName) {
        log.info(
                "[PERFORMANCE_LOG] [{}] Time occurred to perform business logic: {} ms",
                flowName,
                System.currentTimeMillis() - startTime);
    }

    public static String sanitizeString(String str){
        return str == null? null: str.replaceAll("[\\r\\n]", "").replaceAll("[^\\w\\s-]", "");
    }

}
