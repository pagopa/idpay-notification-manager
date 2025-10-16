package it.gov.pagopa.notification.manager.service;

import it.gov.pagopa.notification.manager.config.EmailNotificationProperties;
import it.gov.pagopa.notification.manager.connector.EmailNotificationConnector;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EmailMessageDTO;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationReminderQueueDTO;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationMapper;
import it.gov.pagopa.notification.manager.model.Notification;
import it.gov.pagopa.notification.manager.repository.NotificationManagerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.EmailTemplates.EMAIL_OUTCOME_THREE_DAY_REMINDER;

@Service
@Slf4j
public class WebNotificationManagerServiceImpl implements  WebNotificationManagerService{

    private final EmailNotificationConnector emailNotificationConnector;
    private final EmailNotificationProperties emailNotificationProperties;
    private final NotificationManagerRepository notificationManagerRepository;
    private final NotificationMapper notificationMapper;

    public WebNotificationManagerServiceImpl(EmailNotificationConnector emailNotificationConnector,
                                             EmailNotificationProperties emailNotificationProperties,
                                             NotificationManagerRepository notificationManagerRepository,
                                             NotificationMapper notificationMapper) {
        this.emailNotificationConnector = emailNotificationConnector;
        this.emailNotificationProperties = emailNotificationProperties;
        this.notificationManagerRepository = notificationManagerRepository;
        this.notificationMapper = notificationMapper;
    }

    @Override
    public void sendReminderMail(NotificationReminderQueueDTO notificationQueueDTO) {
        Map<String, String> templateValues = new HashMap<>();
        templateValues.put("name", notificationQueueDTO.getName());

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
        long startTime = System.currentTimeMillis();
        String sanitizedUserId = sanitizeString(notificationQueueDTO.getUserId());
        try {
            emailNotificationConnector.sendEmail(notificationToSend);
            notificationSent(notificationToSend, notificationQueueDTO);
        } catch (Exception e) {
            log.error("[NOTIFY] Failed to send email notification for user {}", sanitizedUserId, e);
            notificationKO(notificationToSend, notificationQueueDTO, startTime);
        }
    }

    private void notificationSent(EmailMessageDTO emailMessageDTO, NotificationReminderQueueDTO notificationReminderQueueDTO) {
        if (notificationReminderQueueDTO == null) {
            return;
        }
        Notification notification = createNotificationFromNotificationreminderQuequeDTO(emailMessageDTO, notificationReminderQueueDTO);
        notification.setNotificationStatus(NotificationConstants.NOTIFICATION_STATUS_OK);
        notificationManagerRepository.save(notification);
    }

    private void notificationKO(EmailMessageDTO emailMessageDTO, NotificationReminderQueueDTO notificationReminderQueueDTO, long startTime) {
        if (notificationReminderQueueDTO == null) {
            return;
        }
        Notification notification = createNotificationFromNotificationreminderQuequeDTO(emailMessageDTO, notificationReminderQueueDTO);

        notification.setNotificationStatus(NotificationConstants.NOTIFICATION_STATUS_KO);
        notification.setStatusKoTimestamp(LocalDateTime.now());
        notificationManagerRepository.save(notification);

        performanceLog(startTime);
    }

    private Notification createNotificationFromNotificationreminderQuequeDTO(EmailMessageDTO emailMessageDTO,
                                                                             NotificationReminderQueueDTO notificationReminderQueueDTO){

        Notification notification = notificationMapper.toEntity(notificationReminderQueueDTO);
        notification.setTemplateName(emailMessageDTO.getTemplateName());
        notification.setTemplateValues(emailMessageDTO.getTemplateValues());
        notification.setSubject(emailMessageDTO.getSubject());
        notification.setContent(emailMessageDTO.getContent());
        notification.setSenderEmail(emailMessageDTO.getSenderEmail());
        notification.setRecipientEmail(emailMessageDTO.getRecipientEmail());
        return notification;
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
