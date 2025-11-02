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
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy").withLocale(Locale.ITALIAN);

        Map<String, String> templateValues = new HashMap<>();
        templateValues.put("name", notificationQueueDTO.getName());
        templateValues.put("voucherEndDate", notificationQueueDTO.getVoucherEndDate().format(formatter));

        EmailMessageDTO emailMessageDTO = EmailMessageDTO.builder()
                .templateName(EMAIL_OUTCOME_THREE_DAY_REMINDER)
                .recipientEmail(notificationQueueDTO.getUserMail())
                .senderEmail(null)
                .templateValues(templateValues)
                .subject(replaceMessageItem(emailNotificationProperties.getSubject().getOkThreeDayReminder(),
                        NotificationConstants.EXPIRING_DAY_KEY,
                        String.valueOf(notificationQueueDTO.getExpiringDay())))
                .content(null)
                .build();
        sendNotification(emailMessageDTO, notificationQueueDTO);
    }

    private String replaceMessageItem(String message, String key, String value) {
        return message.replace(
                NotificationConstants.MARKDOWN_TAG + key + NotificationConstants.MARKDOWN_TAG,
                StringUtils.hasLength(value) ? value : NotificationConstants.MARKDOWN_NA);
    }

    void sendNotification(EmailMessageDTO notificationToSend, NotificationReminderQueueDTO notificationQueueDTO) {
        long startTime = System.currentTimeMillis();
        String sanitizedUserId = sanitizeString(notificationQueueDTO.getUserId());
        try {
            emailNotificationConnector.sendEmail(notificationToSend);
            saveNotification(notificationToSend, notificationQueueDTO, NotificationConstants.NOTIFICATION_STATUS_OK, null, startTime);
        } catch (Exception e) {
            log.error("[NOTIFY] Failed to send email notification for user {}", sanitizedUserId, e);
            saveNotification(notificationToSend, notificationQueueDTO, NotificationConstants.NOTIFICATION_STATUS_KO, LocalDateTime.now(), startTime);
        }
    }

    private void saveNotification(EmailMessageDTO emailMessageDTO,
                                  NotificationReminderQueueDTO notificationReminderQueueDTO,
                                  String notificationStatus,
                                  LocalDateTime statusKoTimeStamp,
                                  long startTime){
        if (notificationReminderQueueDTO == null) {
            return;
        }
        Notification notification = notificationMapper.createNotificationFromNotificationReminderQuequeDTO(emailMessageDTO,
                notificationReminderQueueDTO);
        notification.setNotificationStatus(notificationStatus);
        if(statusKoTimeStamp != null){
            notification.setStatusKoTimestamp(statusKoTimeStamp);
        }

        notificationManagerRepository.save(notification);
        performanceLog(startTime);
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
