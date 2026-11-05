package it.gov.pagopa.notification.manager.service.onboarding;

import feign.FeignException;
import it.gov.pagopa.notification.manager.connector.IOBackEndRestConnector;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationResource;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationDTOMapper;
import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.NotificationContent;
import it.gov.pagopa.notification.manager.service.io.IoNotificationContentService;
import it.gov.pagopa.notification.manager.service.io.placeholder.OnboardingPlaceholder;
import it.gov.pagopa.notification.manager.service.io.placeholder.OnboardingPlaceholderContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode.REJECTION_REASON_INITIATIVE_ENDED;

@Slf4j
@Service
public class OnboardingIoNotificationImpl extends BaseOnboardingNotification<NotificationDTO> implements OnboardingIoNotification {

    private final IoNotificationContentService notificationContentService;
    private final NotificationDTOMapper notificationDTOMapper;
    private final IOBackEndRestConnector ioBackEndRestConnector;

    private final Long timeToLive;
    private final String assistedLink;

    public OnboardingIoNotificationImpl(IoNotificationContentService notificationContentService,
            NotificationDTOMapper notificationDTOMapper,
            IOBackEndRestConnector ioBackEndRestConnector,
            InitiativeRestConnector initiativeRestConnector,
            @Value("${rest-client.notification.backend-io.ttl}") Long timeToLive,
            @Value("${notification.manager.email.assisted-link}") String assistedLink) {
        super(initiativeRestConnector);
        this.notificationContentService = notificationContentService;
        this.notificationDTOMapper = notificationDTOMapper;
        this.ioBackEndRestConnector = ioBackEndRestConnector;
        this.timeToLive = timeToLive;
        this.assistedLink = assistedLink;
    }

    @Override
    NotificationDTO processOnboardingJoined(EvaluationDTO evaluationDTO) {
        return createNotification(evaluationDTO, NotificationEvent.ONBOARDING_JOINED);
    }

    @Override
    NotificationDTO processOnboardingKo(EvaluationDTO evaluationDTO) {
        var reasons = evaluationDTO.getOnboardingRejectionReasons();
        var firstReason = (reasons != null && !reasons.isEmpty()) ? reasons.getFirst() : null;

        NotificationEvent event = firstReason != null
                && REJECTION_REASON_INITIATIVE_ENDED.equals(firstReason.getCode())
                ? NotificationEvent.ONBOARDING_KO_INITIATIVE_ENDED
                : NotificationEvent.ONBOARDING_KO_GENERIC;
        return createNotification(evaluationDTO, event);
    }

    @Override
    protected NotificationDTO generateOnboardingOkNotification(boolean isPartial, EvaluationDTO evaluationDTO) {
        log.info("[NOTIFY][ONBOARDING_STATUS_OK] Starting onboarding notification process. Beneficiary reward is{} partial.", isPartial ? "" : " not");
        return createNotification(
                evaluationDTO,
                isPartial ? NotificationEvent.ONBOARDING_OK_PARTIAL : NotificationEvent.ONBOARDING_OK
        );
    }

    private NotificationDTO createNotification(EvaluationDTO evaluationDTO, NotificationEvent event) {
        Optional<NotificationContent> content = notificationContentService.resolveProfileAndFindAndRender(
                evaluationDTO.getEmailFlux(),
                event,
                new OnboardingPlaceholderContext(evaluationDTO, assistedLink),
                List.of(OnboardingPlaceholder.values())
        );
        if (content.isEmpty()) {
            return null;
        }
        NotificationContent renderedContent = content.get();
        return notificationDTOMapper.map(
                evaluationDTO.getFiscalCode(),
                timeToLive,
                renderedContent.subject(),
                renderedContent.markdown()
        );
    }

    @Override
    String sendNotification(NotificationDTO notificationToSend, EvaluationDTO evaluationDTO, boolean initiativeFetchFailed) {
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

    // TODO: unused?
    @Override
    NotificationDTO createNotification(EvaluationDTO evaluationDTO, String subject, String body, Map<String, String> bodyValues) {
        String markdown = bodyValues != null ? replaceMessageItems(body, bodyValues) : body;
        return notificationDTOMapper.map(evaluationDTO.getFiscalCode(), timeToLive, subject, markdown);
    }

    // TODO: unused here? should be put somewhere else if used by some other class
    public static String replaceMessageItems(String message, Map<String, String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            message = replaceMessageItem(message, entry.getKey(), entry.getValue());
        }
        return message;
    }

    // TODO: unused here? should be put somewhere else if used by some other class
    public static String replaceMessageItem(String message, String key, String value) {
        return message.replace(
                NotificationConstants.MARKDOWN_TAG + key + NotificationConstants.MARKDOWN_TAG,
                StringUtils.hasLength(value) ? value : NotificationConstants.MARKDOWN_NA);
    }

}
