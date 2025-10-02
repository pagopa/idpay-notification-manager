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

@Slf4j
@Service
public class OnboardingIoNotificationImpl extends BaseOnboardingNotification<NotificationDTO> implements OnboardingIoNotification {

    private final NotificationProperties notificationProperties;
    private final NotificationDTOMapper notificationDTOMapper;
    private final IOBackEndRestConnector ioBackEndRestConnector;


    private final Long timeToLive;

    public OnboardingIoNotificationImpl(NotificationProperties notificationProperties,
            NotificationDTOMapper notificationDTOMapper,
            IOBackEndRestConnector ioBackEndRestConnector,
            @Value("${rest-client.notification.backend-io.ttl}") Long timeToLive) {
        this.notificationProperties = notificationProperties;
        this.notificationDTOMapper = notificationDTOMapper;
        this.ioBackEndRestConnector = ioBackEndRestConnector;
        this.timeToLive = timeToLive;
    }

    @Override
    NotificationDTO processOnboardingJoined(EvaluationDTO evaluationDTO) {
        return null; //TODO UPBE-208
    }

    @Override
    NotificationDTO processOnboardingKo(EvaluationDTO evaluationDTO) {
        return null; //TODO UPBE-208
    }

    @Override
    String flowName() {
        return "";
    }

    @Override
    protected NotificationDTO generateOnboardingOkNotification(boolean isPartial, EvaluationDTO evaluationDTO) {
        String subject = isPartial ? notificationProperties.getSubject().getOkPartialBel() : notificationProperties.getSubject().getOkBel();
        String markdown = replaceMessageItem(
                notificationProperties.getMarkdown().getOkCta(),
                NotificationConstants.INITIATIVE_ID_KEY,
                evaluationDTO.getInitiativeId())
                .concat(this.notificationProperties.getMarkdown().getDoubleNewLine());
        Map<String, String> placeholders = null;

        if(isPartial) {
            placeholders = Map.of(
                    NotificationConstants.INITIATIVE_NAME_KEY, evaluationDTO.getInitiativeName(),
                    NotificationConstants.REWARD_AMOUNT_KEY, String.valueOf(CommonUtilities.centsToEuro(evaluationDTO.getBeneficiaryBudgetCents()))
            );
            markdown = markdown.concat(this.notificationProperties.getMarkdown().getOkPartialBel());
        } else {
            placeholders = Map.of(
                    NotificationConstants.INITIATIVE_NAME_KEY, evaluationDTO.getInitiativeName(),
                    NotificationConstants.REWARD_AMOUNT_KEY, String.valueOf(CommonUtilities.centsToEuro(evaluationDTO.getBeneficiaryBudgetCents()))
            );
            markdown = markdown.concat(this.notificationProperties.getMarkdown().getOkBel());
        }

        return createNotification(evaluationDTO, subject, markdown, placeholders);
    }

    @Override
    NotificationDTO createNotification(EvaluationDTO evaluationDTO, String subject, String body, Map<String, String> bodyValues) {
        String markdown = replaceMessageItems(body, bodyValues);
        return notificationDTOMapper.map(evaluationDTO.getFiscalCode(), timeToLive, subject, markdown);
    }

    @Override
    String sendNotification(NotificationDTO notificationToSend, EvaluationDTO evaluationDTO) {
        try {
            NotificationResource notificationResource =
                    ioBackEndRestConnector.notify(notificationToSend, evaluationDTO.getIoToken());
            log.info("[NOTIFY] Notification sent");
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
}
