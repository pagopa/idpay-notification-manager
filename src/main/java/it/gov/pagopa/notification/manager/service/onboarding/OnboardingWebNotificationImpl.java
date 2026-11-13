package it.gov.pagopa.notification.manager.service.onboarding;

import io.micrometer.common.util.StringUtils;
import it.gov.pagopa.notification.manager.config.ProfilesNotificationProperties;
import it.gov.pagopa.notification.manager.connector.EmailNotificationConnector;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EmailMessageDTO;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeNotificationDTO;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationMapper;
import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.exception.NotFoundNotificationEventTemplate;
import it.gov.pagopa.notification.manager.model.EmailNotificationContent;
import it.gov.pagopa.notification.manager.model.Notification;
import it.gov.pagopa.notification.manager.repository.NotificationManagerRepository;
import it.gov.pagopa.notification.manager.service.email.EmailNotificationContentService;
import it.gov.pagopa.notification.manager.service.ProfilesNotificationCatalog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.EmailTemplates.*;
import static it.gov.pagopa.notification.manager.enums.NotificationEvent.*;
import static it.gov.pagopa.notification.manager.utils.Utilities.sanitizeString;

@Slf4j
@Service
public class OnboardingWebNotificationImpl extends BaseOnboardingNotification<EmailMessageDTO> implements OnboardingWebNotification {

    public static final String NOT_FOUND_NOTIFICATION_EVENT_TEMPLATE_ERROR_MESSAGE = "Unable to retry notifying user %s: notification event %s was not found for profile %s";

    private final EmailNotificationConnector emailNotificationConnector;

    private final NotificationManagerRepository notificationManagerRepository;
    private final NotificationMapper notificationMapper;

    private final EmailNotificationContentService emailNotificationContentService;
    private final ProfilesNotificationCatalog profilesNotificationCatalog;
    private final boolean refreshEmailContentOnRetry;

    private static final String MANAGED_ENTITY = "managedEntity";
    private static final String REASON = "reason";
    private static final String ASSISTED_LINK = "assistedLink";

    private record RejectionEmailDetails(
            OnboardingRejectionReason firstReason,
            NotificationEvent event,
            boolean shouldMapManagedEntity
    ) {}

    public OnboardingWebNotificationImpl(EmailNotificationConnector emailNotificationConnector,
                                         NotificationManagerRepository notificationManagerRepository,
                                         NotificationMapper notificationMapper,
                                         InitiativeRestConnector initiativeRestConnector,
                                         EmailNotificationContentService emailNotificationContentService,
                                         ProfilesNotificationCatalog profilesNotificationCatalog,
                                         @Value("${notification.manager.recover.refresh-email-content:false}") boolean refreshEmailContentOnRetry){
        super(initiativeRestConnector);
        this.emailNotificationConnector = emailNotificationConnector;
        this.notificationManagerRepository = notificationManagerRepository;
        this.notificationMapper = notificationMapper;
        this.emailNotificationContentService = emailNotificationContentService;
        this.profilesNotificationCatalog = profilesNotificationCatalog;
        this.refreshEmailContentOnRetry = refreshEmailContentOnRetry;
    }

    @Override
    EmailMessageDTO processOnboardingJoined(EvaluationDTO evaluationDTO) {
        Map<String, String> templateValues = new HashMap<>();
        templateValues.put("name", evaluationDTO.getName());
        return createNotification(evaluationDTO, ONBOARDING_JOINED, templateValues);
    }

    @Override
    EmailMessageDTO processOnboardingKo(EvaluationDTO evaluationDTO) {
        // compute rejection email details based on the first reason
        RejectionEmailDetails computeRejectionEmailDetails = computeRejectionEmailDetails(evaluationDTO);
        final OnboardingRejectionReason firstReason = computeRejectionEmailDetails.firstReason();
        final NotificationEvent event = computeRejectionEmailDetails.event();
        final boolean shouldMapManagedEntity = computeRejectionEmailDetails.shouldMapManagedEntity();

        Map<String, String> templateValues = new HashMap<>();
        templateValues.put("name", evaluationDTO.getName());

        if (shouldMapManagedEntity && firstReason != null) {
            templateValues.put(MANAGED_ENTITY, firstReason.getAuthority() != null
                    ? firstReason.getAuthority()
                    : "Assistenza");
            if(templateValues.get(MANAGED_ENTITY) != null && templateValues.get(MANAGED_ENTITY).equalsIgnoreCase("Assistenza")){
                String assistedLink = null;
                Optional<ProfilesNotificationProperties.ProfileConfigVariables> profileConfigVariables =
                        profilesNotificationCatalog.retrieveProfileVariables(evaluationDTO.getEmailFlux());
                if (profileConfigVariables.isPresent()) {
                    assistedLink = profileConfigVariables.get().getAssistedLink();
                }
                templateValues.put(ASSISTED_LINK, assistedLink);
            }
            setReasonDetail(firstReason, templateValues);

        }

        return createNotification(evaluationDTO, event, templateValues);
    }

    private RejectionEmailDetails computeRejectionEmailDetails(EvaluationDTO evaluationDTO) {
        List<OnboardingRejectionReason> reasons = evaluationDTO.getOnboardingRejectionReasons();
        OnboardingRejectionReason firstReason = (reasons != null && !reasons.isEmpty()) ? reasons.getFirst() : null;

        if (firstReason == null || firstReason.getCode() == null) {
            return new RejectionEmailDetails(
                    null,
                    ONBOARDING_KO_GENERIC,
                    false
            );
        }

        return switch (firstReason.getCode()) {
            case REJECTION_REASON_INITIATIVE_ENDED -> new RejectionEmailDetails(
                    firstReason,
                    ONBOARDING_KO_INITIATIVE_ENDED,
                    false
            );
            case FAMILY_BONUS_ALREADY_USED -> new RejectionEmailDetails(
                    firstReason,
                    ONBOARDING_KO_ALREADY_USED,
                    true
            );
            default -> new RejectionEmailDetails(
                    firstReason,
                    ONBOARDING_KO_GENERIC,
                    true
            );
        };

    }

    private void setReasonDetail(OnboardingRejectionReason firstReason, Map<String, String> templateValues) {
        String detail = firstReason.getDetail();
        String codeDetail = firstReason.getCode().getDetail();

        String reason = detail != null && !detail.isBlank()
                ? detail
                : codeDetail;

        templateValues.put(REASON, reason);
    }

    @Override
    protected EmailMessageDTO generateOnboardingOkNotification(boolean isPartial, EvaluationDTO evaluationDTO) {
        NotificationEvent event = isPartial
                ? ONBOARDING_OK_PARTIAL
                : ONBOARDING_OK;

        // template values
        Map<String, String> templateValues = new HashMap<>();
        templateValues.put("name", evaluationDTO.getName());
        if (evaluationDTO.getBeneficiaryBudgetCents() != null) {
            long amount = evaluationDTO.getBeneficiaryBudgetCents() / 100;
            templateValues.put("amount", String.valueOf(amount));
        }

        return createNotification(evaluationDTO, event, templateValues);
    }

    private EmailMessageDTO createNotification(EvaluationDTO evaluationDTO, NotificationEvent event, Map<String, String> bodyValues) {
        // set NotificationEvent for recovery operations
        evaluationDTO.setEvent(event);

        // try to retrieve content for profile and event
        Optional<EmailNotificationContent> content = emailNotificationContentService.
                resolveProfileAndFindWithDefaultFallback(evaluationDTO.getEmailFlux(), event);

        return content.map(emailNotificationContent -> EmailMessageDTO.builder()
                .templateName(emailNotificationContent.templateName())
                .recipientEmail(evaluationDTO.getUserMail())
                .senderEmail(null)
                .templateValues(bodyValues)
                .subject(emailNotificationContent.subject())
                .content(null)
                .build()
                )
                .orElse(null);

    }

    @Override
    public void handleInitiativeFetchError(EvaluationDTO evaluationDTO) {
        EmailMessageDTO emailMessageDTO = getNotificationFromEvalStatus(evaluationDTO);
        if (emailMessageDTO == null) {
            return;
        }

        emailMessageDTO.setSubject("Initiative fetch error subject");
        emailMessageDTO.setTemplateName(BAD_START_EMAIL_TEMPLATE + "/" + evaluationDTO.getEvent().name());

        saveNotification(emailMessageDTO, evaluationDTO, NotificationConstants.NOTIFICATION_STATUS_KO, LocalDateTime.now(ZoneId.of(ZONE_ID_ROME)), System.currentTimeMillis());
    }

    @Override
    String sendNotification(EmailMessageDTO notificationToSend, EvaluationDTO evaluationDTO) {
        long startTime = System.currentTimeMillis();
        String sanitizedUserId = sanitizeString(evaluationDTO.getUserId());
        String sanitizedInitiativeId = sanitizeString(evaluationDTO.getInitiativeId());

        if (notificationToSend.getTemplateName().startsWith(BAD_START_EMAIL_TEMPLATE)) {
            log.error("[NOTIFY] Notification not sent to user {} because data hasn't been compute properly for initiative {}", sanitizedUserId, sanitizedInitiativeId);
            saveNotification(notificationToSend, evaluationDTO, NotificationConstants.NOTIFICATION_STATUS_KO, LocalDateTime.now(ZoneId.of(ZONE_ID_ROME)), startTime);
            return null;
        }

        try {
            emailNotificationConnector.sendEmail(notificationToSend);
            performanceLog(startTime, "NOTIFY");
            log.info("[NOTIFY] OnboardingMail sent to user {} and initiative {}", sanitizedUserId, sanitizedInitiativeId);
        } catch (Exception e) {
            log.error("[NOTIFY] Failed to send email notification for user {} and initiative {}", sanitizedUserId, sanitizedInitiativeId, e);
            saveNotification(notificationToSend, evaluationDTO, NotificationConstants.NOTIFICATION_STATUS_KO, LocalDateTime.now(ZoneId.of(ZONE_ID_ROME)), startTime);
        }
        return null;
    }


    public boolean notify(Notification notification) {
        long startTime = System.currentTimeMillis();
        String sanitizedUserId = sanitizeString(notification.getUserId());
        String sanitizedInitiativeId = sanitizeString(notification.getInitiativeId());
        try {
            refreshNotificationDataIfNeeded(notification);
            EmailMessageDTO emailMessageDTO = notificationMapper.notificationToEmailMessageDTO(notification);
            emailNotificationConnector.sendEmail(emailMessageDTO);
            if(notification.getId() != null){
                notificationManagerRepository.deleteById(notification.getId());
            }
            log.info("[NOTIFY] OnboardingMail re-sent for user {} and initiative {}", sanitizedUserId, sanitizedInitiativeId);
            performanceLog(startTime, "NOTIFY");
            return true;
        } catch (Exception e) {
            log.error("[NOTIFY] Failed to re-send OnboardingMail for user {} and initiative {}", sanitizedUserId, sanitizedInitiativeId, e);
            finalizeAndSave(notification, NotificationConstants.NOTIFICATION_STATUS_KO, LocalDateTime.now(ZoneId.of(ZONE_ID_ROME)));
            performanceLog(startTime, "NOTIFY");
            return false;
        }
    }

    private void refreshNotificationDataIfNeeded(Notification notification) {
        if (refreshEmailContentOnRetry || (!StringUtils.isBlank(notification.getTemplateName())
                && notification.getTemplateName().startsWith(BAD_START_EMAIL_TEMPLATE))) {
            log.info("[NOTIFY] Refreshing notification data for notification with id {} and initiativeId {}",
                    sanitizeString(notification.getId()),
                    sanitizeString(notification.getInitiativeId()));

            // fetch initiative data to retrieve emailFlux
            InitiativeNotificationDTO initiativeNotificationDTO = initiativeRestConnector.getInitiativeDetailInfo(notification.getInitiativeId());

            // fetch content for profile and event
            EmailNotificationContent content = emailNotificationContentService.
                    resolveProfileAndFind(initiativeNotificationDTO.getEmailFlux(), notification.getEvent())
                    .orElseThrow(() -> new NotFoundNotificationEventTemplate(NOT_FOUND_NOTIFICATION_EVENT_TEMPLATE_ERROR_MESSAGE.formatted(sanitizeString(notification.getId()), notification.getEvent(), initiativeNotificationDTO.getEmailFlux())));

            // replace subject and templateName with the correct ones
            notification.setSubject(content.subject());
            notification.setTemplateName(content.templateName());

            // refresh assisted link if present in template values
            Map<String, String> templateValues = notification.getTemplateValues();
            if (templateValues != null && templateValues.containsKey(ASSISTED_LINK)) {
                String assistedLink = profilesNotificationCatalog.retrieveProfileVariables(initiativeNotificationDTO.getEmailFlux())
                        .map(ProfilesNotificationProperties.ProfileConfigVariables::getAssistedLink)
                        .orElse(null);
                if (assistedLink == null) {
                    log.warn("[NOTIFY] Assisted link not configured for profile {}",
                            sanitizeString(initiativeNotificationDTO.getEmailFlux()));
                }
                Map<String, String> refreshedValues = new HashMap<>(templateValues);
                refreshedValues.put(ASSISTED_LINK, assistedLink);
                notification.setTemplateValues(refreshedValues);
            }
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
        finalizeAndSave(notification, notificationStatus, statusKoTimeStamp);
        performanceLog(startTime, "NOTIFY");
    }


    private void  finalizeAndSave(Notification  notification,
                                  String  notificationStatus,
                                  LocalDateTime statusKoTimeStamp) {
        notification.setNotificationStatus(notificationStatus);
        if  (statusKoTimeStamp !=  null)  {
            notification.setStatusKoTimestamp(statusKoTimeStamp);
        }
        notificationManagerRepository.save(notification);
    }

    private void performanceLog(long startTime, String flowName) {
        log.info(
                "[PERFORMANCE_LOG] [{}] Time occurred to perform business logic: {} ms",
                flowName,
                System.currentTimeMillis() - startTime);
    }

}
