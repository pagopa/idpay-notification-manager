package it.gov.pagopa.notification.manager.service.onboarding;

import feign.FeignException;
import it.gov.pagopa.notification.manager.config.ProfilesNotificationProperties;
import it.gov.pagopa.notification.manager.connector.IOBackEndRestConnector;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationResource;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationDTOMapper;
import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.exception.NotFoundNotificationEventTemplate;
import it.gov.pagopa.notification.manager.model.NotificationContent;
import it.gov.pagopa.notification.manager.service.io.IoNotificationContentService;
import it.gov.pagopa.notification.manager.service.io.NotificationEventResolver;
import it.gov.pagopa.notification.manager.service.io.ProfilesNotificationCatalog;
import it.gov.pagopa.notification.manager.service.io.placeholder.OnboardingPlaceholder;
import it.gov.pagopa.notification.manager.service.io.placeholder.OnboardingPlaceholderContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static it.gov.pagopa.notification.manager.utils.Utilities.sanitizeString;

@Slf4j
@Service
public class OnboardingIoNotificationImpl extends BaseOnboardingNotification<NotificationDTO> implements OnboardingIoNotification {

    public static final String NOT_FOUND_NOTIFICATION_EVENT_TEMPLATE_ERROR_MESSAGE = "Unable to notify user %s: notification event %s was not found for profile %s";
    private final IoNotificationContentService notificationContentService;
    private final ProfilesNotificationCatalog profilesNotificationCatalog;
    private final NotificationDTOMapper notificationDTOMapper;
    private final IOBackEndRestConnector ioBackEndRestConnector;

    private final Long timeToLive;

    public OnboardingIoNotificationImpl(IoNotificationContentService notificationContentService,
            ProfilesNotificationCatalog profilesNotificationCatalog,
            NotificationDTOMapper notificationDTOMapper,
            IOBackEndRestConnector ioBackEndRestConnector,
            InitiativeRestConnector initiativeRestConnector,
            @Value("${rest-client.notification.backend-io.ttl}") Long timeToLive) {
        super(initiativeRestConnector);
        this.notificationContentService = notificationContentService;
        this.profilesNotificationCatalog = profilesNotificationCatalog;
        this.notificationDTOMapper = notificationDTOMapper;
        this.ioBackEndRestConnector = ioBackEndRestConnector;
        this.timeToLive = timeToLive;
    }

    @Override
    NotificationDTO processOnboardingJoined(EvaluationDTO evaluationDTO) {
        NotificationContent notificationContent = retrieveNotificationContent(evaluationDTO, NotificationEvent.ONBOARDING_JOINED)
                .orElseThrow(() -> new NotFoundNotificationEventTemplate(NOT_FOUND_NOTIFICATION_EVENT_TEMPLATE_ERROR_MESSAGE.formatted(sanitizeString(evaluationDTO.getUserId()), NotificationEvent.ONBOARDING_JOINED, evaluationDTO.getEmailFlux())));
        return createNotification(evaluationDTO, notificationContent);
    }

    @Override
    NotificationDTO processOnboardingKo(EvaluationDTO evaluationDTO) {
        NotificationEvent notificationEvent =
                NotificationEventResolver.resolveOnboardingStatusKoEventByRejectionReasons(evaluationDTO.getOnboardingRejectionReasons());

        NotificationContent finalContent = null;
        if(!NotificationEvent.ONBOARDING_KO_GENERIC.equals(notificationEvent)) {

            Optional<NotificationContent> content = retrieveNotificationContent(evaluationDTO, notificationEvent);
            if (content.isPresent()) {
                finalContent = content.get();
            }
        }

        if(finalContent == null) {
            finalContent = retrieveNotificationContent(evaluationDTO, NotificationEvent.ONBOARDING_KO_GENERIC)
                    .orElseThrow(() -> new NotFoundNotificationEventTemplate(NOT_FOUND_NOTIFICATION_EVENT_TEMPLATE_ERROR_MESSAGE.formatted(sanitizeString(evaluationDTO.getUserId()), notificationEvent, evaluationDTO.getEmailFlux())));
        }

        return createNotification(evaluationDTO, finalContent);
    }

    @Override
    protected NotificationDTO generateOnboardingOkNotification(boolean isPartial, EvaluationDTO evaluationDTO) {
        log.info("[NOTIFY][ONBOARDING_STATUS_OK] Starting onboarding notification process. Beneficiary reward is{} partial.", isPartial ? "" : " not");
        NotificationEvent notificationEvent = isPartial ? NotificationEvent.ONBOARDING_OK_PARTIAL : NotificationEvent.ONBOARDING_OK;
        NotificationContent notificationContent = retrieveNotificationContent(evaluationDTO, notificationEvent)
                .orElseThrow(() -> new NotFoundNotificationEventTemplate(NOT_FOUND_NOTIFICATION_EVENT_TEMPLATE_ERROR_MESSAGE.formatted(evaluationDTO.getUserId(), notificationEvent, evaluationDTO.getEmailFlux())));
        return createNotification(
                evaluationDTO,
                notificationContent
        );
    }

    private NotificationDTO createNotification(EvaluationDTO evaluationDTO, NotificationContent content) {
        return notificationDTOMapper.map(
                evaluationDTO.getFiscalCode(),
                timeToLive,
                content.subject(),
                content.markdown()
        );
    }

    private Optional<NotificationContent> retrieveNotificationContent(EvaluationDTO evaluationDTO, NotificationEvent event) {
        String assistedLink = null;
        Optional<ProfilesNotificationProperties.ProfileConfigVariables> profileConfigVariables =
                profilesNotificationCatalog.retrieveProfileVariables(evaluationDTO.getEmailFlux());
        if (profileConfigVariables.isPresent()) {
            assistedLink = profileConfigVariables.get().getAssistedLink();
        }

        return notificationContentService.resolveProfileAndFindAndRender(
                evaluationDTO.getEmailFlux(),
                event,
                OnboardingPlaceholderContext.from(evaluationDTO, assistedLink),
                List.of(OnboardingPlaceholder.values())
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
}
