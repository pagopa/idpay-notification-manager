package it.gov.pagopa.notification.manager.service.onboarding;

import io.micrometer.common.util.StringUtils;
import it.gov.pagopa.notification.manager.config.EmailNotificationProperties;
import it.gov.pagopa.notification.manager.config.ProfilesNotificationProperties;
import it.gov.pagopa.notification.manager.connector.EmailNotificationConnector;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EmailMessageDTO;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeNotificationDTO;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationMapper;
import it.gov.pagopa.notification.manager.model.Notification;
import it.gov.pagopa.notification.manager.repository.NotificationManagerRepository;
import it.gov.pagopa.notification.manager.service.io.ProfilesNotificationCatalog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.EmailTemplates.*;
import static it.gov.pagopa.notification.manager.utils.Utilities.sanitizeString;

@Slf4j
@Service
public class OnboardingWebNotificationImpl extends BaseOnboardingNotification<EmailMessageDTO> implements OnboardingWebNotification {

    private final EmailNotificationConnector emailNotificationConnector;
    private final EmailNotificationProperties emailNotificationProperties;

    private final NotificationManagerRepository notificationManagerRepository;
    private final NotificationMapper notificationMapper;

    private final ProfilesNotificationCatalog profilesNotificationCatalog;

    private static final String MANAGED_ENTITY = "managedEntity";
    private static final String REASON = "reason";

    private record RejectionEmailDetails(
            OnboardingRejectionReason firstReason,
            String template,
            String subject,
            boolean shouldMapManagedEntity
    ) {}

    public OnboardingWebNotificationImpl(EmailNotificationConnector emailNotificationConnector,
                                         EmailNotificationProperties emailNotificationProperties,
                                         NotificationManagerRepository notificationManagerRepository,
                                         NotificationMapper notificationMapper,
                                         InitiativeRestConnector initiativeRestConnector,
                                         ProfilesNotificationCatalog profilesNotificationCatalog){
        super(initiativeRestConnector);
        this.emailNotificationConnector = emailNotificationConnector;
        this.emailNotificationProperties = emailNotificationProperties;
        this.notificationManagerRepository = notificationManagerRepository;
        this.notificationMapper = notificationMapper;
        this.profilesNotificationCatalog = profilesNotificationCatalog;
    }

    @Override
    EmailMessageDTO processOnboardingJoined(EvaluationDTO evaluationDTO) {
        Map<String, String> templateValues = new HashMap<>();
        templateValues.put("name", evaluationDTO.getName());
        String body = String.format(EMAIL_OUTCOME_FAMILY_UNIT, evaluationDTO.getEmailFlux());
        return createNotification(evaluationDTO, emailNotificationProperties.getSubject().getKoFamilyUnit(), body, templateValues);
    }

    @Override
    EmailMessageDTO processOnboardingKo(EvaluationDTO evaluationDTO) {
        // compute rejection email details based on the first reason
        RejectionEmailDetails computeRejectionEmailDetails = computeRejectionEmailDetails(evaluationDTO);
        final OnboardingRejectionReason firstReason = computeRejectionEmailDetails.firstReason();
        final String template = computeRejectionEmailDetails.template();
        final String subject = computeRejectionEmailDetails.subject();
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
                templateValues.put("assistedLink", assistedLink);
            }
            setReasonDetail(firstReason, templateValues);

        }

        return createNotification(evaluationDTO, subject, template, templateValues);
    }

    private RejectionEmailDetails computeRejectionEmailDetails(EvaluationDTO evaluationDTO) {
        List<OnboardingRejectionReason> reasons = evaluationDTO.getOnboardingRejectionReasons();
        OnboardingRejectionReason firstReason = (reasons != null && !reasons.isEmpty()) ? reasons.getFirst() : null;

        if (firstReason == null || firstReason.getCode() == null) {
            return new RejectionEmailDetails(
                    null,
                    String.format(EMAIL_OUTCOME_GENERIC_ERROR, evaluationDTO.getEmailFlux()),
                    emailNotificationProperties.getSubject().getKoGenericError(),
                    false
            );
        }

        return switch (firstReason.getCode()) {
            case REJECTION_REASON_INITIATIVE_ENDED -> new RejectionEmailDetails(
                    firstReason,
                    String.format(EMAIL_OUTCOME_THANKS, evaluationDTO.getEmailFlux()),
                    emailNotificationProperties.getSubject().getKoThanks(),
                    false
            );
            case FAMILY_BONUS_ALREADY_USED -> new RejectionEmailDetails(
                    firstReason,
                    String.format(EMAIL_OUTCOME_ALREADY_USED_KO, evaluationDTO.getEmailFlux()),
                    emailNotificationProperties.getSubject().getKoAlreadyUsed(),
                    true
            );
            default -> new RejectionEmailDetails(
                    firstReason,
                    String.format(EMAIL_OUTCOME_GENERIC_ERROR, evaluationDTO.getEmailFlux()),
                    emailNotificationProperties.getSubject().getKoGenericError(),
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
        String template = String.format(
                isPartial ? EMAIL_OUTCOME_PARTIAL : EMAIL_OUTCOME_OK,
                evaluationDTO.getEmailFlux()
        );


        Map<String, String> templateValues = new HashMap<>();
        templateValues.put("name", evaluationDTO.getName());

        if (evaluationDTO.getBeneficiaryBudgetCents() != null) {
            long amount = evaluationDTO.getBeneficiaryBudgetCents() / 100;
            templateValues.put("amount", String.valueOf(amount));
        }

        String subject = isPartial ?
                emailNotificationProperties.getSubject().getPartial() :
                emailNotificationProperties.getSubject().getOk();


        return createNotification(evaluationDTO, subject, template, templateValues);
    }

    private EmailMessageDTO createNotification(EvaluationDTO evaluationDTO, String subject, String body, Map<String, String> bodyValues) {
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
    public void handleInitiativeFetchError(EvaluationDTO evaluationDTO) {
        EmailMessageDTO emailMessageDTO = getNotificationFromEvalStatus(evaluationDTO);
        if (emailMessageDTO == null) {
            return;
        }
        saveNotification(emailMessageDTO, evaluationDTO, NotificationConstants.NOTIFICATION_STATUS_KO, LocalDateTime.now(ZoneId.of(ZONE_ID_ROME)), System.currentTimeMillis());
    }

    @Override
    String sendNotification(EmailMessageDTO notificationToSend, EvaluationDTO evaluationDTO, boolean initiativeFetchFailed) {
        long startTime = System.currentTimeMillis();
        String sanitizedUserId = sanitizeString(evaluationDTO.getUserId());
        String sanitizedInitiativeId = sanitizeString(evaluationDTO.getInitiativeId());
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
            replaceBadTemplateName(notification);
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

    private void replaceBadTemplateName(Notification notification) {
        if (!StringUtils.isBlank(notification.getTemplateName()) && notification.getTemplateName().startsWith(BAD_START_EMAIL_TEMPLATE)) {
            log.info("[NOTIFY] Replacing bad template name for notification with id {} and initiativeId {}",
                    sanitizeString(notification.getId()),
                    sanitizeString(notification.getInitiativeId()));
            InitiativeNotificationDTO initiativeNotificationDTO = initiativeRestConnector.getInitiativeDetailInfo(notification.getInitiativeId());
            String replacement = notification.getTemplateName().replace(BAD_START_EMAIL_TEMPLATE, "Email_" + initiativeNotificationDTO.getEmailFlux());
            notification.setTemplateName(replacement);
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
