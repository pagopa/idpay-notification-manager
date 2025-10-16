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
        try {
            emailNotificationConnector.sendEmail(notificationToSend);
            notificationSent(notificationToSend, evaluationDTO);
        } catch (Exception e) {
            log.error("[NOTIFY] Failed to send email notification for user {}", evaluationDTO.getUserId(), e);
            notificationKO(notificationToSend, evaluationDTO, startTime);
        }
        return null;
    }


    public boolean notify(Notification notification) {
        long startTime = System.currentTimeMillis();
        try {
            EmailMessageDTO emailMessageDTO = notificationMapper.notificationToEmailMessageDTO(notification);
            emailNotificationConnector.sendEmail(emailMessageDTO);
            notificationSent(notification);
            return true;
        } catch (Exception e) {
            log.error("[NOTIFY] Failed to send email notification for user {}", notification.getUserId(), e);
            notificationKO(notification, startTime);
            return false;
        }
    }

    private void notificationKO(EmailMessageDTO emailMessageDTO, EvaluationDTO evaluationDTO, long startTime) {
        if (emailMessageDTO == null) {
            return;
        }
        Notification notification = notificationMapper.evaluationToNotification(evaluationDTO);
        notification.setTemplateName(emailMessageDTO.getTemplateName());
        notification.setTemplateValues(emailMessageDTO.getTemplateValues());
        notification.setSubject(emailMessageDTO.getSubject());
        notification.setContent(emailMessageDTO.getContent());
        notification.setSenderEmail(emailMessageDTO.getSenderEmail());
        notification.setRecipientEmail(emailMessageDTO.getRecipientEmail());
        notification.setNotificationStatus(NotificationConstants.NOTIFICATION_STATUS_KO);
        notification.setStatusKoTimestamp(LocalDateTime.now());
        notificationManagerRepository.save(notification);

        performanceLog(startTime);
    }

    private void notificationKO(Notification notification, long startTime) {
        if (notification == null) {
            return;
        }
        notification.setNotificationStatus(NotificationConstants.NOTIFICATION_STATUS_KO);
        notificationManagerRepository.save(notification);

        performanceLog(startTime);
    }

    private void notificationSent(EmailMessageDTO emailMessageDTO, EvaluationDTO evaluationDTO) {
        if (emailMessageDTO == null) {
            return;
        }
        Notification notification = notificationMapper.evaluationToNotification(evaluationDTO);
        notification.setTemplateName(emailMessageDTO.getTemplateName());
        notification.setTemplateValues(emailMessageDTO.getTemplateValues());
        notification.setSubject(emailMessageDTO.getSubject());
        notification.setContent(emailMessageDTO.getContent());
        notification.setSenderEmail(emailMessageDTO.getSenderEmail());
        notification.setRecipientEmail(emailMessageDTO.getRecipientEmail());
        notification.setNotificationStatus(NotificationConstants.NOTIFICATION_STATUS_OK);
        notificationManagerRepository.save(notification);
    }

    private void notificationSent(Notification notification) {
        if (notification == null) {
            return;
        }
        notification.setNotificationStatus(NotificationConstants.NOTIFICATION_STATUS_OK);
        notificationManagerRepository.save(notification);
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

}
