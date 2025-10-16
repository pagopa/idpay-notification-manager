package it.gov.pagopa.notification.manager.service.onboarding;

import it.gov.pagopa.notification.manager.config.EmailNotificationProperties;
import it.gov.pagopa.notification.manager.connector.EmailNotificationConnector;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EmailMessageDTO;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationMapper;
import it.gov.pagopa.notification.manager.model.Notification;
import it.gov.pagopa.notification.manager.repository.NotificationManagerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.EmailTemplates.*;
import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode.REJECTION_REASON_INITIATIVE_ENDED;

@Slf4j
@Service
public class OnboardingWebNotificationImpl extends BaseOnboardingNotification<EmailMessageDTO> implements OnboardingWebNotification {

    private final EmailNotificationConnector emailNotificationConnector;
    private final EmailNotificationProperties emailNotificationProperties;

    private final NotificationManagerRepository notificationManagerRepository;
    private final NotificationMapper notificationMapper;

    public OnboardingWebNotificationImpl(EmailNotificationConnector emailNotificationConnector,
                                         EmailNotificationProperties emailNotificationProperties, NotificationManagerRepository notificationManagerRepository, NotificationMapper notificationMapper) {
        this.emailNotificationConnector = emailNotificationConnector;
        this.emailNotificationProperties = emailNotificationProperties;
        this.notificationManagerRepository = notificationManagerRepository;
        this.notificationMapper = notificationMapper;
    }


    @Override
    EmailMessageDTO processOnboardingJoined(EvaluationDTO evaluationDTO) {
        Map<String, String> templateValues = new HashMap<>();
        templateValues.put("name", evaluationDTO.getName());
        return createNotification(evaluationDTO, emailNotificationProperties.getSubject().getKoFamilyUnit(), EMAIL_OUTCOME_FAMILY_UNIT, templateValues);
    }

    @Override
    EmailMessageDTO processOnboardingKo(EvaluationDTO evaluationDTO) {
        var reasons = evaluationDTO.getOnboardingRejectionReasons();
        var firstReason = (reasons != null && !reasons.isEmpty()) ? reasons.getFirst() : null;

        final boolean initiativeEnded = firstReason != null
                && REJECTION_REASON_INITIATIVE_ENDED.equals(firstReason.getCode());

        final String template = initiativeEnded ? EMAIL_OUTCOME_THANKS : EMAIL_OUTCOME_GENERIC_ERROR;
        final String subject = initiativeEnded
                ? emailNotificationProperties.getSubject().getKoThanks()
                : emailNotificationProperties.getSubject().getKoGenericError();

        Map<String, String> templateValues = new HashMap<>();
        templateValues.put("name", evaluationDTO.getName());

        if (!initiativeEnded && firstReason != null) {
            templateValues.put("reason", firstReason.getDetail() != null ? firstReason.getDetail() : "REASON");
            templateValues.put("managedEntity", firstReason.getAuthorityLabel() != null ? firstReason.getAuthorityLabel() : "HELPDESK");
        }

        return createNotification(evaluationDTO, subject, template, templateValues);
    }

    @Override
    protected EmailMessageDTO generateOnboardingOkNotification(boolean isPartial, EvaluationDTO evaluationDTO) {
        String template = isPartial ? EMAIL_OUTCOME_PARTIAL : EMAIL_OUTCOME_OK;

        Map<String, String> templateValues = new HashMap<>();
        templateValues.put("name", evaluationDTO.getName());

        if (evaluationDTO.getBeneficiaryBudgetCents() != null) {
            long amount = evaluationDTO.getBeneficiaryBudgetCents() / 100;
            templateValues.put("amount", String.valueOf(amount));
        }

        String subject = EMAIL_OUTCOME_OK.equals(template) ?
                emailNotificationProperties.getSubject().getOk() :
                emailNotificationProperties.getSubject().getPartial();

        return createNotification(evaluationDTO, subject, template, templateValues);
    }

    @Override
    EmailMessageDTO createNotification(EvaluationDTO evaluationDTO, String subject, String body, Map<String, String> bodyValues) {
        return EmailMessageDTO.builder()
                .templateName(body)
                .recipientEmail(evaluationDTO.getUserMail())
                .senderEmail(null)
                .templateValues(bodyValues)
                .subject(subject)
                .content(null)
                .build();
    }

    @Override
    String sendNotification(EmailMessageDTO notificationToSend, EvaluationDTO evaluationDTO) {
        long startTime = System.currentTimeMillis();
        String sanitizedUserId = sanitizeString(evaluationDTO.getUserId());
        try {
            emailNotificationConnector.sendEmail(notificationToSend);
            saveNotification(notificationToSend, evaluationDTO, NotificationConstants.NOTIFICATION_STATUS_OK, null, startTime);
        } catch (Exception e) {
            log.error("[NOTIFY] Failed to send email notification for user {}", sanitizedUserId, e);
            saveNotification(notificationToSend, evaluationDTO, NotificationConstants.NOTIFICATION_STATUS_KO, LocalDateTime.now(), startTime);
        }
        return null;
    }


    public boolean notify(Notification notification) {
        long startTime = System.currentTimeMillis();
        try {
            EmailMessageDTO emailMessageDTO = notificationMapper.notificationToEmailMessageDTO(notification);
            emailNotificationConnector.sendEmail(emailMessageDTO);
            saveNotification(notification, NotificationConstants.NOTIFICATION_STATUS_OK, null, startTime);
            return true;
        } catch (Exception e) {
            log.error("[NOTIFY] Failed to send email notification for user {}", notification.getUserId(), e);
            saveNotification(notification, NotificationConstants.NOTIFICATION_STATUS_KO, LocalDateTime.now(), startTime);
            return false;
        }
    }

    private void saveNotification(EmailMessageDTO emailMessageDTO,
                                  EvaluationDTO evaluationDTO,
                                  String notificationStatus,
                                  LocalDateTime statusKoTimeStamp,
                                  long startTime){
        if (emailMessageDTO == null) {
            return;
        }
        Notification notification = notificationMapper.createNotificationFromEmailMessageDTO(emailMessageDTO,
                evaluationDTO);
        finalizeAndSave(notification, notificationStatus, statusKoTimeStamp, startTime);
    }

    private void saveNotification(Notification notification,
                                  String notificationStatus,
                                  LocalDateTime statusKoTimeStamp,
                                  long startTime){
        if (notification == null) {
            return;
        }
        finalizeAndSave(notification, notificationStatus, statusKoTimeStamp, startTime);
    }

    private void  finalizeAndSave(Notification  notification,
                                  String  notificationStatus,
                                  LocalDateTime statusKoTimeStamp,
                                  long  startTime) {
        notification.setNotificationStatus(notificationStatus);
        if  (statusKoTimeStamp !=  null)  {
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
