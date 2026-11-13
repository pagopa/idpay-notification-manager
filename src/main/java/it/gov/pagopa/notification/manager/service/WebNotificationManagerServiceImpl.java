package it.gov.pagopa.notification.manager.service;

import feign.FeignException;
import it.gov.pagopa.notification.manager.connector.EmailNotificationConnector;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.dto.EmailMessageDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationReminderQueueDTO;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeNotificationDTO;
import it.gov.pagopa.notification.manager.model.EmailNotificationContent;
import it.gov.pagopa.notification.manager.service.email.EmailNotificationContentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static it.gov.pagopa.notification.manager.enums.NotificationEvent.VOUCHER_EXPIRING;

@Service
@Slf4j
public class WebNotificationManagerServiceImpl implements  WebNotificationManagerService{

    private final EmailNotificationConnector emailNotificationConnector;
    private final InitiativeRestConnector initiativeRestConnector;
    private final EmailNotificationContentService emailNotificationContentService;

    public WebNotificationManagerServiceImpl(EmailNotificationConnector emailNotificationConnector,
                                             InitiativeRestConnector initiativeRestConnector,
                                             EmailNotificationContentService emailNotificationContentService
    ) {
        this.emailNotificationConnector = emailNotificationConnector;
        this.initiativeRestConnector = initiativeRestConnector;
        this.emailNotificationContentService = emailNotificationContentService;
    }

    @Override
    public void sendReminderMail(NotificationReminderQueueDTO notificationQueueDTO) {
        long startTime = System.currentTimeMillis();
        String sanitizedUserId = sanitizeString(notificationQueueDTO.getUserId());
        String sanitizedInitiativeId = sanitizeString(notificationQueueDTO.getInitiativeId());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy").withLocale(Locale.ITALIAN);

        // fetch initiative data to retrieve the profile for the email notification content
        InitiativeNotificationDTO initiativeNotificationDTO;
        try {
            initiativeNotificationDTO = initiativeRestConnector.getInitiativeDetailInfo(notificationQueueDTO.getInitiativeId());
        } catch (FeignException e) {
            log.error("[NOTIFY] Failed to send email notification for user {} and initiative {}", sanitizedUserId, sanitizedInitiativeId, e);
            performanceLog(startTime);
            return;
        }

        // retrieve profiled content for the reminder email notification
        Optional<EmailNotificationContent> content = emailNotificationContentService.
                resolveProfileAndFind(initiativeNotificationDTO.getEmailFlux(), VOUCHER_EXPIRING);
        if (content.isEmpty()) {
            log.error("[NOTIFY] Failed to send email notification for user {} and initiative {}: no content found for profile {} and event {}",
                    sanitizedUserId, sanitizedInitiativeId, initiativeNotificationDTO.getEmailFlux(), VOUCHER_EXPIRING);
            performanceLog(startTime);
            return;
        }

        // map template values
        Map<String, String> templateValues = new HashMap<>();
        templateValues.put("name", notificationQueueDTO.getName());
        templateValues.put("voucherEndDate", notificationQueueDTO.getVoucherEndDate().format(formatter));

        EmailMessageDTO emailMessageDTO = EmailMessageDTO.builder()
                .templateName(content.get().templateName())
                .recipientEmail(notificationQueueDTO.getUserMail())
                .senderEmail(null)
                .templateValues(templateValues)
                .subject(content.get().subject())
                .content(null)
                .build();
        sendNotification(emailMessageDTO, notificationQueueDTO);
    }

    void sendNotification(EmailMessageDTO notificationToSend, NotificationReminderQueueDTO notificationQueueDTO) {
        long startTime = System.currentTimeMillis();
        String sanitizedUserId = sanitizeString(notificationQueueDTO.getUserId());
        String sanitizedInitiativeId = sanitizeString(notificationQueueDTO.getInitiativeId());
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
