package it.gov.pagopa.notification.manager.service.onboarding;

import feign.FeignException;
import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.notification.manager.config.NotificationProperties;
import it.gov.pagopa.notification.manager.connector.IOBackEndRestConnector;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationResource;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationDTOMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.EmailTemplates.*;
import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode.REJECTION_REASON_INITIATIVE_ENDED;

@Slf4j
@Service
public class OnboardingIoNotificationImpl extends BaseOnboardingNotification<NotificationDTO> implements OnboardingIoNotification {

    private final NotificationProperties notificationProperties;
    private final NotificationDTOMapper notificationDTOMapper;
    private final IOBackEndRestConnector ioBackEndRestConnector;

    private final Long timeToLive;

    private final String assistedLink;

    public OnboardingIoNotificationImpl(NotificationProperties notificationProperties,
            NotificationDTOMapper notificationDTOMapper,
            IOBackEndRestConnector ioBackEndRestConnector,
            @Value("${rest-client.notification.backend-io.ttl}") Long timeToLive,
            @Value("notification.manager.email.assisted-link") String assistedLink) {
        this.notificationProperties = notificationProperties;
        this.notificationDTOMapper = notificationDTOMapper;
        this.ioBackEndRestConnector = ioBackEndRestConnector;
        this.timeToLive = timeToLive;
        this.assistedLink = assistedLink;
    }

    @Override
    NotificationDTO processOnboardingJoined(EvaluationDTO evaluationDTO) {
        String subject = notificationProperties.getSubject().getJoinedBel();
        String markdown = notificationProperties.getMarkdown().getJoinedBel().concat(this.notificationProperties.getMarkdown().getDoubleNewLine());
        return createNotification(evaluationDTO, subject, markdown, null);
    }

    @Override
    NotificationDTO processOnboardingKo(EvaluationDTO evaluationDTO) {
        var reasons = evaluationDTO.getOnboardingRejectionReasons();
        var firstReason = (reasons != null && !reasons.isEmpty()) ? reasons.getFirst() : null;

        final boolean initiativeEnded = firstReason != null
                && REJECTION_REASON_INITIATIVE_ENDED.equals(firstReason.getCode());

        final String markdown = initiativeEnded
                ? notificationProperties.getMarkdown().getKoThanksBel()
                : notificationProperties.getMarkdown().getKoGenericBel();
        final String subject  = initiativeEnded
                ? notificationProperties.getSubject().getKoThanksBel()
                : notificationProperties.getSubject().getKoGenericBel();

        Map<String, String> placeholders = null;

        if (!initiativeEnded && firstReason != null) {
            placeholders = Map.of(
                    NotificationConstants.MANAGED_ENTITY_KEY, firstReason.getAuthority() != null
                            ? firstReason.getAuthority()
                            : "[Assistenza](" + assistedLink + ")"
            );
        }
        return createNotification(evaluationDTO, subject, markdown, placeholders);
    }

    @Override
    protected NotificationDTO generateOnboardingOkNotification(boolean isPartial, EvaluationDTO evaluationDTO) {
        log.info("[NOTIFY][ONBOARDING_STATUS_OK] Starting onboarding notification process. Beneficiary reward is{} partial.", isPartial ? "" : " not");
        String subject = isPartial ? notificationProperties.getSubject().getOkPartialBel() : notificationProperties.getSubject().getOkBel();
        String markdown = replaceMessageItem(
                notificationProperties.getMarkdown().getOkCta(),
                NotificationConstants.INITIATIVE_ID_KEY,
                evaluationDTO.getInitiativeId())
                .concat(this.notificationProperties.getMarkdown().getDoubleNewLine());
        Map<String, String> placeholders;

        if(isPartial) {
            placeholders = Map.of(
                    NotificationConstants.INITIATIVE_NAME_KEY, evaluationDTO.getInitiativeName(),
                    NotificationConstants.REWARD_AMOUNT_KEY, String.valueOf(CommonUtilities.centsToEuro(evaluationDTO.getBeneficiaryBudgetCents()))
            );
            markdown = markdown.concat(this.notificationProperties.getMarkdown().getOkPartialBel());
        } else {
            placeholders = Map.of(
                    NotificationConstants.INITIATIVE_NAME_KEY, evaluationDTO.getInitiativeName(),
                    NotificationConstants.REWARD_AMOUNT_KEY, evaluationDTO.getBeneficiaryBudgetCents() != null ? String.valueOf(CommonUtilities.centsToEuro(evaluationDTO.getBeneficiaryBudgetCents())) : ""
            );
            markdown = markdown.concat(this.notificationProperties.getMarkdown().getOkBel());
        }

        return createNotification(evaluationDTO, subject, markdown, placeholders);
    }

    @Override
    NotificationDTO createNotification(EvaluationDTO evaluationDTO, String subject, String body, Map<String, String> bodyValues) {
        String markdown = bodyValues != null ? replaceMessageItems(body, bodyValues) : body;
        return notificationDTOMapper.map(evaluationDTO.getFiscalCode(), timeToLive, subject, markdown);
    }

    @Override
    String sendNotification(NotificationDTO notificationToSend, EvaluationDTO evaluationDTO) {
        String sanitizedUserId = sanitizeString(evaluationDTO.getUserId());
        String sanitizedInitiativeId = sanitizeString(evaluationDTO.getInitiativeId());
        try {
            NotificationResource notificationResource =
                    ioBackEndRestConnector.notify(notificationToSend, evaluationDTO.getIoToken());
            String sanitizedNotificationId = sanitizeString(notificationResource.getId());
            log.info("[NOTIFY] [SENT_NOTIFICATION_OK] -  Notification {} sent to user {} and initiative {}",
                    sanitizedNotificationId, sanitizedUserId, sanitizedInitiativeId);
            return notificationResource.getId();
        } catch (FeignException e) {
            log.error("[NOTIFY] [{}] Cannot send notification: {}", e.status(), e.contentUTF8());
            return null;
        }

    }
    private String replaceMessageItems(String message, Map<String, String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            message = replaceMessageItem(message, entry.getKey(), entry.getValue());
        }
        return message;
    }

    private String replaceMessageItem(String message, String key, String value) {
        return message.replace(
                NotificationConstants.MARKDOWN_TAG + key + NotificationConstants.MARKDOWN_TAG,
                StringUtils.hasLength(value) ? value : NotificationConstants.MARKDOWN_NA);
    }

    public static String sanitizeString(String str){
        return str == null? null: str.replaceAll("[\\r\\n]", "").replaceAll("[^\\w\\s-]", "");
    }
}
