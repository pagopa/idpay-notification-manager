package it.gov.pagopa.notification.manager.service.onboarding;

import it.gov.pagopa.notification.manager.config.EmailNotificationProperties;
import it.gov.pagopa.notification.manager.connector.EmailNotificationConnector;
import it.gov.pagopa.notification.manager.dto.EmailMessageDTO;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.EmailTemplates.*;
import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode.REJECTION_REASON_INITIATIVE_ENDED;

@Slf4j
@Service
public class OnboardingWebNotificationImpl extends BaseOnboardingNotification<EmailMessageDTO> implements OnboardingWebNotification{

    private final EmailNotificationConnector emailNotificationConnector;
    private final EmailNotificationProperties emailNotificationProperties;

    public OnboardingWebNotificationImpl(EmailNotificationConnector emailNotificationConnector,
                                         EmailNotificationProperties emailNotificationProperties) {
        this.emailNotificationConnector = emailNotificationConnector;
        this.emailNotificationProperties = emailNotificationProperties;
    }


    @Override
    EmailMessageDTO processOnboardingJoined(EvaluationDTO evaluationDTO) {
        Map<String, String> templateValues = new HashMap<>();
        templateValues.put("name", evaluationDTO.getUserId());
        return createNotification(evaluationDTO, emailNotificationProperties.getSubject().getKoFamilyUnit(), EMAIL_OUTCOME_FAMILY_UNIT, templateValues);
    }

    @Override
    EmailMessageDTO processOnboardingKo(EvaluationDTO evaluationDTO) {
        var reasons = evaluationDTO.getOnboardingRejectionReasons();
        var firstReason = (reasons != null && !reasons.isEmpty()) ? reasons.getFirst() : null;

        final boolean initiativeEnded = firstReason != null
                && REJECTION_REASON_INITIATIVE_ENDED.equals(firstReason.getCode());

        final String template = initiativeEnded ? EMAIL_OUTCOME_THANKS : EMAIL_OUTCOME_GENERIC_ERROR;
        final String subject  = initiativeEnded
                ? emailNotificationProperties.getSubject().getKoThanks()
                : emailNotificationProperties.getSubject().getKoGenericError();

        Map<String, String> templateValues = new HashMap<>();
        templateValues.put("name", evaluationDTO.getUserId());

        if (!initiativeEnded && firstReason != null) {
            templateValues.put("reason", firstReason.getDetail() != null ? firstReason.getDetail() : "REASON");
            templateValues.put("managedEntity", firstReason.getAuthorityLabel() != null ? firstReason.getAuthorityLabel() : "HELPDESK");
        }

        return createNotification(evaluationDTO, subject, template, templateValues);
    }

    @Override
    protected EmailMessageDTO generateOnboardingOkNotification(boolean isPartial, EvaluationDTO evaluationDTO) {
        String template = isPartial ? EMAIL_OUTCOME_PARTIAL: EMAIL_OUTCOME_OK;

        Map<String, String> templateValues = new HashMap<>();
        templateValues.put("name", evaluationDTO.getUserId());

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
        try {
            emailNotificationConnector.sendEmail(notificationToSend);
        } catch (Exception e) {
            log.error("[NOTIFY] Failed to send email notification for user {}", evaluationDTO.getUserId(), e);
        }
        return null;
    }
}
