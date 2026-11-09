package it.gov.pagopa.notification.manager.service;

import feign.FeignException;
import it.gov.pagopa.notification.manager.config.ProfilesNotificationProperties;
import it.gov.pagopa.notification.manager.connector.IOBackEndRestConnector;
import it.gov.pagopa.notification.manager.connector.PdvDecryptRestConnector;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.*;
import it.gov.pagopa.notification.manager.dto.event.*;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeAdditionalInfoDTO;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeNotificationDTO;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationDTOMapper;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationMapper;
import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.event.producer.OutcomeProducer;
import it.gov.pagopa.notification.manager.exception.NotFoundNotificationEventTemplate;
import it.gov.pagopa.notification.manager.model.Notification;
import it.gov.pagopa.notification.manager.model.NotificationContent;
import it.gov.pagopa.notification.manager.repository.NotificationManagerRepository;
import it.gov.pagopa.notification.manager.repository.NotificationManagerRepositoryExtended;
import it.gov.pagopa.notification.manager.service.io.IoNotificationContentService;
import it.gov.pagopa.notification.manager.service.io.NotificationEventResolver;
import it.gov.pagopa.notification.manager.service.io.ProfilesNotificationCatalog;
import it.gov.pagopa.notification.manager.service.io.placeholder.*;
import it.gov.pagopa.notification.manager.service.onboarding.OnboardingIoNotification;
import it.gov.pagopa.notification.manager.service.onboarding.OnboardingWebNotification;
import it.gov.pagopa.notification.manager.utils.AuditUtilities;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.AnyNotificationConsumer.SubTypes.*;
import static it.gov.pagopa.notification.manager.constants.NotificationConstants.STATUS_ONBOARDING_KO;


@Service
@Slf4j
public class NotificationManagerServiceImpl implements NotificationManagerService {
    public static final String GENERIC_ERROR_LOG = "[NOTIFY][RECOVER] Something went wrong while recovering notifications";
    private final int pageSize;
    private final long delay;
    private final OutcomeProducer outcomeProducer;
    private final InitiativeRestConnector initiativeRestConnector;
    private final IOBackEndRestConnector ioBackEndRestConnector;
    private final NotificationManagerRepository notificationManagerRepository;
    private final NotificationManagerRepositoryExtended notificationManagerRepositoryExtended;
    private final NotificationDTOMapper notificationDTOMapper;
    private final PdvDecryptRestConnector pdvDecryptRestConnector;
    private final NotificationMapper notificationMapper;
    private final AuditUtilities auditUtilities;
    private final OnboardingIoNotification onboardingIoNotification;
    private final OnboardingWebNotification onboardingWebNotification;
    private final IoNotificationContentService ioNotificationContentService;
    private final ProfilesNotificationCatalog profilesNotificationCatalog;
    @Value("${rest-client.notification.backend-io.ttl}")
    private Long timeToLive;
    @Value("${notification.manager.recover.parallelism}")
    private int parallelism;
    @Value("${notification.manager.markdown.double.new.line}")
    private String markdownDoubleNewLine;

    private static final List<Class<? extends AnyOfNotificationQueueDTO>> SUPPORTED_TYPES =
            List.of(
                    NotificationCitizenOnQueueDTO.class,
                    NotificationIbanQueueDTO.class,
                    NotificationRefundQueueDTO.class,
                    NotificationSuspensionQueueDTO.class,
                    NotificationReadmissionQueueDTO.class,
                    NotificationReminderQueueDTO.class,
                    NotificationOnboardingQueueDTO.class
            );

    private static final String LOG_NOTIFICATION_KO = "[NOTIFY] [SENT_NOTIFICATION_KO] -  Failed to send notification for user {} and initiative {}";
    private static final String ZONE_ID_ROME = "Europe/Rome";
    private ExecutorService executorService;

    public NotificationManagerServiceImpl(@Value("${app.delete.paginationSize:100}") int pageSize,
                                          @Value("${app.delete.delayTime:1000}") long delay,
                                          OutcomeProducer outcomeProducer,
                                          InitiativeRestConnector initiativeRestConnector,
                                          IOBackEndRestConnector ioBackEndRestConnector,
                                          NotificationManagerRepository notificationManagerRepository,
                                          NotificationManagerRepositoryExtended notificationManagerRepositoryExtended,
                                          NotificationDTOMapper notificationDTOMapper,
                                          PdvDecryptRestConnector pdvDecryptRestConnector,
                                          NotificationMapper notificationMapper,
                                          AuditUtilities auditUtilities,
                                          OnboardingIoNotification onboardingIoNotification,
                                          OnboardingWebNotification onboardingWebNotification,
                                          IoNotificationContentService ioNotificationContentService,
                                          ProfilesNotificationCatalog profilesNotificationCatalog) {
        this.pageSize = pageSize;
        this.delay = delay;
        this.outcomeProducer = outcomeProducer;
        this.initiativeRestConnector = initiativeRestConnector;
        this.ioBackEndRestConnector = ioBackEndRestConnector;
        this.notificationManagerRepository = notificationManagerRepository;
        this.notificationManagerRepositoryExtended = notificationManagerRepositoryExtended;
        this.notificationDTOMapper = notificationDTOMapper;
        this.pdvDecryptRestConnector = pdvDecryptRestConnector;
        this.notificationMapper = notificationMapper;
        this.auditUtilities = auditUtilities;
        this.onboardingIoNotification = onboardingIoNotification;
        this.onboardingWebNotification = onboardingWebNotification;
        this.ioNotificationContentService = ioNotificationContentService;
        this.profilesNotificationCatalog = profilesNotificationCatalog;
    }

    @PostConstruct
    void init() {
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory("recovery-thread");
        executorService = Executors.newFixedThreadPool(parallelism, threadFactory);
    }

    @PreDestroy
    void close() {
        executorService.shutdown();
    }

    @Scheduled(cron = "${notification.manager.recover.schedule}")
    void schedule() {
        log.debug("[NOTIFY] Starting schedule to recover KO notifications");
        this.recoverKoNotifications();
    }

    @Override
    public void notify(EvaluationDTO evaluationDTO) {
        String sanitizedUserId = sanitizeString(evaluationDTO.getUserId());
        String sanitizedChannel = sanitizeString(evaluationDTO.getChannel().toString());
        long startTime = System.currentTimeMillis();

        if (shouldSkipNotification(evaluationDTO)) {
            return;
        }

        if (evaluationDTO.getChannel().isAppIo()) {
            processAppIoNotification(evaluationDTO, startTime);
        } else if (evaluationDTO.getChannel().isWeb()) {
            onboardingWebNotification.processNotification(evaluationDTO);
            processAppIoNotification(evaluationDTO, startTime);
        } else {
            log.warn("[NOTIFY] Unsupported channel {} for user {}", sanitizedChannel, sanitizedUserId);
        }
        performanceLog(startTime);
    }

    @Override
    public void manualNotify(ManualNotificationDTO manualNotificationDTO) {

        long startTime = System.currentTimeMillis();
        Notification notification = Notification.builder()
                .userId(manualNotificationDTO.getUserId())
                .initiativeId(manualNotificationDTO.getInitiativeId())
                .build();

        Pair<String, String> notificationPreChecks =  processAppIoGeneralNotification(notification, startTime, true);
        if(notificationPreChecks == null){
            return;
        }

        String markdown = (manualNotificationDTO.getBodyValues() != null
                ? replaceMessageItems(
                    manualNotificationDTO.getContent().getMarkdown(), manualNotificationDTO.getBodyValues())
                : manualNotificationDTO.getContent().getMarkdown()).concat(markdownDoubleNewLine);

        NotificationDTO notificationDTO =
                notificationDTOMapper.map(
                        notificationPreChecks.getValue(),
                        timeToLive,
                        manualNotificationDTO.getContent().getSubject(),
                        markdown);

        String notificationId = sendNotification(notificationDTO, notificationPreChecks.getKey());
        logNotificationId(notificationId);

        performanceLog(startTime);
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

    private boolean shouldSkipNotification(EvaluationDTO evaluationDTO) {
        boolean hasFamilyCriteriaKo = evaluationDTO.getOnboardingRejectionReasons() != null &&
                evaluationDTO.getOnboardingRejectionReasons().stream()
                        .anyMatch(r -> r.getType() == OnboardingRejectionReason.OnboardingRejectionReasonType.FAMILY_CRITERIA_KO);

        boolean isDemandedType2 = NotificationConstants.STATUS_ONBOARDING_DEMANDED.equals(evaluationDTO.getStatus());

        return hasFamilyCriteriaKo || isDemandedType2;
    }

    private void processAppIoNotification(EvaluationDTO evaluationDTO, long startTime) {

        Notification notification = notificationMapper.evaluationToNotification(evaluationDTO);
        Pair<String, String> notificationPreChecks = processAppIoGeneralNotification(notification, startTime, false);
        if(notificationPreChecks == null){
            return;
        }

        evaluationDTO.setIoToken(notificationPreChecks.getKey());
        evaluationDTO.setFiscalCode(notificationPreChecks.getValue());

        try {
            String notificationId = onboardingIoNotification.processNotification(evaluationDTO);

            if (notificationId == null) {
                log.error("[NOTIFY][ONBOARDING_STATUS] Failed to send onboarding status notification.");
                notificationKO(notification, startTime);
                return;
            }

            logNotificationId(notificationId);
        } catch (NotFoundNotificationEventTemplate _) {
            log.info(
                    "[NOTIFY][IO] Notification event {} not configured for initiative {}. Notification skipped",
                    sanitizeString(evaluationDTO.getStatus()),
                    sanitizeString(evaluationDTO.getInitiativeId())
            );
        }


    }

    private Pair<String, String> processAppIoGeneralNotification(Notification notification, long startTime, boolean manual) {
        Pair<String, String> result = null;

        String sanitizedUserId = sanitizeString(notification.getUserId());
        String sanitizedInitiativeId = sanitizeString(notification.getInitiativeId());
        InitiativeAdditionalInfoDTO ioTokens;

        try {
            ioTokens = initiativeRestConnector.getIOTokens(notification.getInitiativeId());
        } catch (FeignException e) {
            log.error("[NOTIFY][ONBOARDING_STATUS] Failed to retrieve ioTokens from initiative service.");
            if(!manual) {
                notificationKO(notification, startTime);
            }
            return result;
        }

        if (ioTokens == null) {
            log.error("[NOTIFY][ONBOARDING_STATUS] ioTokens must not be null");
            if(!manual) {
                notificationKO(notification, startTime);
            }
            return result;
        }

        String fiscalCode = decryptUserToken(notification.getUserId());
        if (fiscalCode == null || isNotSenderAllowed(fiscalCode, ioTokens.getPrimaryKey())) {
            log.error("[NOTIFY][ONBOARDING_STATUS] Invalid fiscal code or notifications not allowed for this user.");
            log.error(LOG_NOTIFICATION_KO, sanitizedUserId, sanitizedInitiativeId);
            return result;
        }

        result = Pair.of(ioTokens.getPrimaryKey(), fiscalCode);
        return result;

    }



    @Override
    public boolean notify(Notification notification) {
        String sanitizedUserId = sanitizeString(notification.getUserId());
        String sanitizedInitiativeId = sanitizeString(notification.getInitiativeId());
        long startTime = System.currentTimeMillis();

        notification.setNotificationDate(LocalDateTime.now(ZoneId.of(ZONE_ID_ROME)));

        InitiativeAdditionalInfoDTO ioTokens = null;

        try {
            log.info("[NOTIFY] Sending request to INITIATIVE");
            ioTokens = initiativeRestConnector.getIOTokens(notification.getInitiativeId());
        } catch (FeignException e) {
            log.error(NotificationConstants.FEIGN_KO.formatted(e.status(), e.contentUTF8()));
        }

        if (ioTokens == null) {
            notificationKO(notification, startTime);
            return false;
        }

        String fiscalCode = decryptUserToken(notification.getUserId());

        if (fiscalCode == null) {
            log.error(LOG_NOTIFICATION_KO, sanitizedUserId, sanitizedInitiativeId);
            return false;
        }

        String tokenDecrypt = ioTokens.getPrimaryKey();

        if (isNotSenderAllowed(fiscalCode, tokenDecrypt)) {
            log.error(LOG_NOTIFICATION_KO, sanitizedUserId, sanitizedInitiativeId);
            return false;
        }


        // retrieve initiative configured profile
        InitiativeNotificationDTO initiativeDetailInfo = getInitiativeDetailInfo(notification.getInitiativeId());
        if (initiativeDetailInfo == null) {
            return false;
        }
        String configuredProfile = initiativeDetailInfo.getEmailFlux();

        // resolve notification content based on the notification type and initiative configured profile
        Optional<NotificationContent> content;
        switch (notification.getOperationType()) {
            case ONBOARDING -> {
                Optional<NotificationEvent> optEvent =
                        NotificationEventResolver.resolveOnboardingEvent(notification);

                if (optEvent.isPresent()) {
                    NotificationEvent notificationEventToRender = optEvent.get();

                    // handle the case where the notification event is ONBOARDING_KO
                    // and the notification content is not configured for the given profile and event
                    if (STATUS_ONBOARDING_KO.equals(notification.getOnboardingOutcome())) {
                        Optional<NotificationContent> optContent =
                                ioNotificationContentService.find(configuredProfile, optEvent.get());

                        if (optContent.isEmpty()) {
                            notificationEventToRender = NotificationEvent.ONBOARDING_KO_GENERIC;
                        }
                    }

                    // resolve profile, find event and render notification content with placeholders
                    String assistedLink = recoverAssistedLink(configuredProfile);
                    content = ioNotificationContentService.resolveProfileAndFindAndRender(
                            configuredProfile,
                            notificationEventToRender,
                            OnboardingPlaceholderContext.from(notification, assistedLink),
                            List.of(OnboardingPlaceholder.values()));
                } else {
                    log.error("[NOTIFY][ONBOARDING] Cannot resolve notification event for user {} and initiative {}",
                            sanitizedUserId, sanitizedInitiativeId);
                    content = Optional.empty();
                }
            }
            case CHECKIBAN_KO -> {
                content = ioNotificationContentService.resolveProfileAndFind(
                        configuredProfile,
                        NotificationEvent.CHECK_IBAN_KO);
            }
            case REFUND -> {
                BigDecimal refund = notification.getRefundReward() == null
                        ? null
                        : notification.getRefundReward()
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_DOWN);

                content = ioNotificationContentService.resolveProfileAndFindAndRender(
                        configuredProfile,
                        NotificationEventResolver.resolveRefundEvent(notification.getRefundStatus()),
                        RefundPlaceholderContext.from(refund),
                        List.of(RefundPlaceholder.values()));
            }
            case SUSPENSION -> {
                content = ioNotificationContentService.resolveProfileAndFindAndRender(
                        configuredProfile,
                        NotificationEvent.SUSPENSION,
                        InitiativePlaceholderContext.from(notification),
                        List.of(InitiativePlaceholder.values()));
            }
            case READMISSION -> {
                content = ioNotificationContentService.resolveProfileAndFindAndRender(
                        configuredProfile,
                        NotificationEvent.READMISSION,
                        InitiativePlaceholderContext.from(notification),
                        List.of(InitiativePlaceholder.values()));
            }
            default -> {
                return false;
            }
        }

        // if content is empty, it means that the notification is not configured for the given profile and event
        if (content.isEmpty()) {
            notificationKO(notification, startTime);
            return false;
        }

        // build notification and then send it
        NotificationContent rendered = content.get();
        NotificationDTO notificationDTO = notificationDTOMapper.map(
                fiscalCode,
                timeToLive,
                rendered.subject(),
                rendered.markdown());

        String notificationId = sendNotification(notificationDTO, tokenDecrypt);

        if (notificationId == null) {
            notificationKO(notification, startTime);
            return false;
        }

        logNotificationId(notificationId);

        notificationSent(notification);
        performanceLog(startTime);

        return true;
    }

    private String recoverAssistedLink(String configuredProfile) {
        String assistedLink = null;
        Optional<ProfilesNotificationProperties.ProfileConfigVariables> profileConfigVariables =
                profilesNotificationCatalog.retrieveProfileVariables(configuredProfile);
        if (profileConfigVariables.isPresent()) {
            assistedLink = profileConfigVariables.get().getAssistedLink();
        }
        return assistedLink;
    }

    @Override
    public void recoverKoNotifications() {
        log.debug("[NOTIFY][RECOVER] Searching for notifications to recover");

        final LocalDateTime startTime = LocalDateTime.now(ZoneId.of(ZONE_ID_ROME));
        List<Future<Long>> workers = IntStream.range(0, parallelism)
                .mapToObj(ignored -> executorService.submit(() -> recover(startTime)))
                .toList();

        long recovered = workers.stream().mapToLong(f -> {
            try {
                return f.get();
            } catch (ExecutionException e) {
                log.error(GENERIC_ERROR_LOG, e);
                return 0;
            } catch (InterruptedException e) {
                log.error(GENERIC_ERROR_LOG, e);
                Thread.currentThread().interrupt();
                throw new IllegalStateException(GENERIC_ERROR_LOG, e);
            }
        }).sum();

        if (recovered > 0) {
            log.info("[NOTIFY][RECOVER] Notifications that have been recovered {}", recovered);
        } else {
            log.info("[NOTIFY][RECOVER] Notifications that have been recovered 0");
        }
    }

    @SuppressWarnings("BusyWait")
    @Override
    public void processNotification(CommandOperationQueueDTO commandOperationQueueDTO) {

        log.info("[COMMAND_OPERATION] Starting evaluate payload: {}", commandOperationQueueDTO);
        if (NotificationConstants.OPERATION_TYPE_DELETE_INITIATIVE.equals(commandOperationQueueDTO.getOperationType())) {
            long startTime = System.currentTimeMillis();

            List<Notification> deletedOperation = new ArrayList<>();
            List<Notification> fetchedNotifications;

            do {
                fetchedNotifications = notificationManagerRepositoryExtended.deletePaged(commandOperationQueueDTO.getEntityId(), pageSize);

                deletedOperation.addAll(fetchedNotifications);

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("An error has occurred while waiting {}", e.getMessage());
                }
            } while (fetchedNotifications.size() == pageSize);

            log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection : notification", commandOperationQueueDTO.getEntityId());

            deletedOperation.stream()
                    .map(Notification::getUserId)
                    .distinct()
                    .forEach(userId -> auditUtilities.logDeletedNotification(userId, commandOperationQueueDTO.getEntityId()));
            performanceLog(startTime, "DELETE_INITIATIVE");
        }
    }

    private long recover(LocalDateTime startTime) {

        long count = 0;
        Notification n;
        while ((n = notificationManagerRepository.findKoToRecover(startTime)) != null) {
            log.info("[NOTIFY][RECOVER] Trying to recover notification with id {}", n.getId());

            n.setRetry(n.getRetry() != null ? n.getRetry() + 1 : 1);

            boolean recovered = true;
            try {
                if(n.getChannel() == null || n.getChannel().isAppIo()) {
                    recovered = this.notify(n);
                } else if(n.getChannel().isWeb()) {
                    recovered = onboardingWebNotification.notify(n);
                }
                if (recovered) count++;
            } catch (Exception e) {
                log.error("[NOTIFY][RECOVER] Something went wrong while recovering notification having id {}", n.getId(), e);
            }
        }

        return count;
    }

    @Override
    public void addOutcome(EvaluationDTO evaluationDTO) {
        outcomeProducer.sendOutcome(evaluationDTO);
    }

    @Override
    public void sendNotificationFromOperationType(AnyOfNotificationQueueDTO anyOfNotificationQueueDTO) {
        long startTime = System.currentTimeMillis();

        // check if notification is supported
        if (!(anyOfNotificationQueueDTO instanceof NotificationQueueDTO queueDTO)
                || SUPPORTED_TYPES.stream().noneMatch(type -> type.isInstance(queueDTO))) {
            return;
        }

        // execute common operations first
        // setup notification entity to save in case of any KOs
        Notification notification = notificationMapper.toEntity(queueDTO);

        // retrieve ioTokens from initiativeId
        InitiativeAdditionalInfoDTO ioTokens = getIoTokens(queueDTO.getInitiativeId());
        if (ioTokens == null) {
            notificationKO(notification, startTime);
            return;
        }

        // retrieve fiscalCode from userId
        String fiscalCode = decryptUserToken(queueDTO.getUserId());
        if (fiscalCode == null) {
            notificationKO(notification, startTime);
            return;
        }

        // check if user is not enabled to receive notifications
        String tokenDecrypt = ioTokens.getPrimaryKey();
        if (isNotSenderAllowed(fiscalCode, tokenDecrypt)) {
            notificationKO(notification, startTime);
            return;
        }

        // retrieve initiative configured profile
        InitiativeNotificationDTO initiativeDetailInfo = getInitiativeDetailInfo(queueDTO.getInitiativeId());
        if (initiativeDetailInfo == null) {
            notificationKO(notification, startTime);
            return;
        }
        String configuredProfile = initiativeDetailInfo.getEmailFlux();

        // resolve notification content based on the notification type and initiative configured profile
        Optional<NotificationContent> content;
        switch (anyOfNotificationQueueDTO) {
            case NotificationCitizenOnQueueDTO ignored -> {
                content = ioNotificationContentService.resolveProfileAndFindAndRender(
                        configuredProfile,
                        NotificationEvent.INITIATIVE_PUBLISHING,
                        InitiativePlaceholderContext.from(notification),
                        List.of(InitiativePlaceholder.values()));
            }
            case NotificationIbanQueueDTO ignored -> {
                content = ioNotificationContentService.resolveProfileAndFind(
                        configuredProfile,
                        NotificationEvent.CHECK_IBAN_KO);
            }
            case NotificationRefundQueueDTO notificationRefundOnQueueDTO -> {
                BigDecimal refund = BigDecimal.valueOf(notificationRefundOnQueueDTO.getRefundReward())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_DOWN);
                content = ioNotificationContentService.resolveProfileAndFindAndRender(
                        configuredProfile,
                        NotificationEventResolver.resolveRefundEvent(notificationRefundOnQueueDTO.getStatus()),
                        RefundPlaceholderContext.from(refund),
                        List.of(RefundPlaceholder.values()));
            }
            case NotificationSuspensionQueueDTO ignored -> {
                content = ioNotificationContentService.resolveProfileAndFindAndRender(
                        configuredProfile,
                        NotificationEvent.SUSPENSION,
                        InitiativePlaceholderContext.from(notification),
                        List.of(InitiativePlaceholder.values()));
            }
            case NotificationReadmissionQueueDTO ignored -> {
                content = ioNotificationContentService.resolveProfileAndFindAndRender(
                        configuredProfile,
                        NotificationEvent.READMISSION,
                        InitiativePlaceholderContext.from(notification),
                        List.of(InitiativePlaceholder.values()));
            }
            case NotificationReminderQueueDTO notificationReminderQueueDTO -> {
                content = ioNotificationContentService.resolveProfileAndFindAndRender(
                        configuredProfile,
                        NotificationEvent.VOUCHER_EXPIRING,
                        ReminderPlaceholderContext.from(notificationReminderQueueDTO),
                        List.of(ReminderPlaceholder.values()));
            }
            case NotificationOnboardingQueueDTO ignored -> {
                content = ioNotificationContentService.resolveProfileAndFindAndRender(
                        configuredProfile,
                        NotificationEvent.ONBOARDING_WAITING_LIST,
                        InitiativePlaceholderContext.from(notification),
                        List.of(InitiativePlaceholder.values()));
            }
            default -> {
                return;
            }
        }

        // if content is empty, it means that the notification is not configured for the given profile and event
        if (content.isEmpty()) {
            return;
        }

        // build notification and then send it
        NotificationContent rendered = content.get();
        NotificationDTO notificationDTO = notificationDTOMapper.map(
                fiscalCode,
                timeToLive,
                rendered.subject(),
                rendered.markdown()
        );

        String notificationId = sendNotification(notificationDTO, tokenDecrypt);

        if (notificationId == null) {
            notificationKO(notification, startTime);
            return;
        }

        logNotificationId(notificationId);

        performanceLog(startTime);
    }

    private String sendNotification(NotificationDTO notificationDTO, String primaryKey) {
        try {
            NotificationResource notificationResource =
                    ioBackEndRestConnector.notify(notificationDTO, primaryKey);
            log.info("[NOTIFY] Notification sent");
            return notificationResource.getId();
        } catch (FeignException e) {
            log.error("[NOTIFY] [{}] Cannot send notification: {}", e.status(), e.contentUTF8());
            return null;
        }
    }

    private String decryptUserToken(String token) {
        log.info(NotificationConstants.REQUEST_PDV);
        try {
            return pdvDecryptRestConnector.getPii(token).getPii();
        } catch (FeignException e) {
            return null;
        }
    }

    private boolean isNotSenderAllowed(String fiscalCode, String primaryKey) {
        try {
            FiscalCodeDTO fiscalCodeDTO = new FiscalCodeDTO(fiscalCode);
            ProfileResource profileResource = ioBackEndRestConnector.getProfile(fiscalCodeDTO, primaryKey);
            return !profileResource.isSenderAllowed();
        } catch (FeignException e) {
            log.error("[NOTIFY] The user is not enabled to receive notifications", e);
            return true;
        }
    }

    private InitiativeAdditionalInfoDTO getIoTokens(String initiativeId) {
        log.debug(NotificationConstants.IO_TOKENS);
        try {
            return initiativeRestConnector.getIOTokens(initiativeId);
        } catch (FeignException e) {
            log.error("{}", NotificationConstants.FEIGN_KO.formatted(e.status(), e.contentUTF8()));
            return null;
        }
    }

    private InitiativeNotificationDTO getInitiativeDetailInfo(String initiativeId) {
        log.debug("Getting initiativeDetail");
        InitiativeNotificationDTO initiativeNotificationDTO;
        try {
            initiativeNotificationDTO = initiativeRestConnector.getInitiativeDetailInfo(initiativeId);
        } catch (FeignException e) {
            log.error("{}", NotificationConstants.FEIGN_KO.formatted(e.status(), e.contentUTF8()));
            return null;
        }
        if (initiativeNotificationDTO == null) {
            log.error("[NOTIFY] The initiative detail info couldn't be retrieved | initiativeId={}", initiativeId);
            return null;
        }
        log.info("[NOTIFY] Initiative configured profile retrieved | initiativeId={}, profile={}", initiativeId, initiativeNotificationDTO.getEmailFlux());
        return initiativeNotificationDTO;
    }

    private void notificationKO(Notification notification, long startTime) {
        if (notification == null) {
            return;
        }
        String sanitizedUserId = sanitizeString(notification.getUserId());
        String sanitizedInitiativeId = sanitizeString(notification.getInitiativeId());
        notification.setNotificationStatus(NotificationConstants.NOTIFICATION_STATUS_KO);
        notification.setStatusKoTimestamp(LocalDateTime.now(ZoneId.of(ZONE_ID_ROME)));
        notification.setUpdateDate(LocalDateTime.now(ZoneId.of(ZONE_ID_ROME)));
        notificationManagerRepository.save(notification);
        log.error(LOG_NOTIFICATION_KO, sanitizedUserId, sanitizedInitiativeId);
        performanceLog(startTime);
    }

    private void notificationSent(Notification notification) {
        if(notification.getId() != null) {
            notificationManagerRepository.deleteById(notification.getId());
        }
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

    private static void logNotificationId(String notificationId) {
        log.info("[NOTIFY] Notification ID: {}", notificationId);
    }

    public static String sanitizeString(String str){
        return str == null? null: str.replaceAll("[\\r\\n]", "").replaceAll("[^\\w\\s-]", "");
    }
}
