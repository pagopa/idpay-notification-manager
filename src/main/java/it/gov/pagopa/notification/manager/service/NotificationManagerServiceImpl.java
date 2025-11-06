package it.gov.pagopa.notification.manager.service;

import feign.FeignException;
import it.gov.pagopa.notification.manager.connector.IOBackEndRestConnector;
import it.gov.pagopa.notification.manager.connector.PdvDecryptRestConnector;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.*;
import it.gov.pagopa.notification.manager.dto.event.*;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeAdditionalInfoDTO;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationDTOMapper;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationMapper;
import it.gov.pagopa.notification.manager.event.producer.OutcomeProducer;
import it.gov.pagopa.notification.manager.model.Notification;
import it.gov.pagopa.notification.manager.model.NotificationMarkdown;
import it.gov.pagopa.notification.manager.repository.NotificationManagerRepository;
import it.gov.pagopa.notification.manager.repository.NotificationManagerRepositoryExtended;
import it.gov.pagopa.notification.manager.service.onboarding.OnboardingIoNotification;
import it.gov.pagopa.notification.manager.service.onboarding.OnboardingWebNotification;
import it.gov.pagopa.notification.manager.utils.AuditUtilities;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.AnyNotificationConsumer.SubTypes.*;

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
    private final NotificationMarkdown notificationMarkdown;
    private final AuditUtilities auditUtilities;
    private final OnboardingIoNotification onboardingIoNotification;
    private final OnboardingWebNotification onboardingWebNotification;
    @Value("${rest-client.notification.backend-io.ttl}")
    private Long timeToLive;
    @Value("${notification.manager.recover.parallelism}")
    private int parallelism;

    private ExecutorService executorService;

    public NotificationManagerServiceImpl(@Value("${app.delete.paginationSize:100}") int pageSize,
                                          @Value("${app.delete.delayTime:1000}") long delay, OutcomeProducer outcomeProducer, InitiativeRestConnector initiativeRestConnector, IOBackEndRestConnector ioBackEndRestConnector, NotificationManagerRepository notificationManagerRepository, NotificationManagerRepositoryExtended notificationManagerRepositoryExtended, NotificationDTOMapper notificationDTOMapper, PdvDecryptRestConnector pdvDecryptRestConnector, NotificationMapper notificationMapper, NotificationMarkdown notificationMarkdown, AuditUtilities auditUtilities, OnboardingIoNotification onboardingIoNotification, OnboardingWebNotification onboardingWebNotification) {
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
        this.notificationMarkdown = notificationMarkdown;
        this.auditUtilities = auditUtilities;
        this.onboardingIoNotification = onboardingIoNotification;
        this.onboardingWebNotification = onboardingWebNotification;
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
        long startTime = System.currentTimeMillis();

        if (shouldSkipNotification(evaluationDTO)) {
            return;
        }

        if (evaluationDTO.getChannel().isAppIo()) {
            processAppIoNotification(evaluationDTO, startTime);
        } else if (evaluationDTO.getChannel().isWeb()) {
            onboardingWebNotification.processNotification(evaluationDTO);
        } else {
            log.warn("[NOTIFY] Unsupported channel {} for user {}", evaluationDTO.getChannel(), evaluationDTO.getUserId());
        }
        performanceLog(startTime);
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
        InitiativeAdditionalInfoDTO ioTokens;

        try {
            ioTokens = initiativeRestConnector.getIOTokens(evaluationDTO.getInitiativeId());
        } catch (FeignException e) {
            log.error("[NOTIFY][ONBOARDING_STATUS] Failed to retrieve ioTokens from initiative service.");
            notificationKO(notification, startTime);
            return;
        }

        if (ioTokens == null) {
            log.error("[NOTIFY][ONBOARDING_STATUS] ioTokens must not be null");
            notificationKO(notification, startTime);
            return;
        }

        String fiscalCode = decryptUserToken(evaluationDTO.getUserId());
        if (fiscalCode == null || isNotSenderAllowed(fiscalCode, ioTokens.getPrimaryKey())) {
            log.error("[NOTIFY][ONBOARDING_STATUS] Invalid fiscal code or notifications not allowed for this user.");
            notificationKO(notification, startTime);
            return;
        }

        evaluationDTO.setIoToken(ioTokens.getPrimaryKey());
        evaluationDTO.setFiscalCode(fiscalCode);
        String notificationId = onboardingIoNotification.processNotification(evaluationDTO);

        if (notificationId == null) {
            log.error("[NOTIFY][ONBOARDING_STATUS] Failed to send onboarding status notification.");
            notificationKO(notification, startTime);
            return;
        }

        notificationSent(notification, notificationId);
    }

    @Override
    public boolean notify(Notification notification) {
        long startTime = System.currentTimeMillis();

        notification.setNotificationDate(LocalDateTime.now());

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
            notificationKO(notification, startTime);
            return false;
        }

        String tokenDecrypt = ioTokens.getPrimaryKey();

        if (isNotSenderAllowed(fiscalCode, tokenDecrypt)) {
            notificationKO(notification, startTime);
            return false;
        }

        String subject;
        String markdown;

        switch (notification.getOperationType()) {
            case ONBOARDING -> {
                subject = notificationMarkdown.getSubject(notification);
                markdown = notificationMarkdown.getMarkdown(notification);
            }
            case CHECKIBAN_KO -> {
                subject = notificationMarkdown.getSubjectCheckIbanKo();
                markdown = notificationMarkdown.getMarkdownCheckIbanKo();
            }
            case REFUND -> {
                subject = notificationMarkdown.getSubjectRefund(notification.getRefundStatus());
                markdown = notificationMarkdown.getMarkdownRefund(notification.getRefundStatus(), notification.getRefundReward());
            }
            case SUSPENSION -> {
                subject = notificationMarkdown.getSubjectSuspension(notification.getInitiativeName());
                markdown = notificationMarkdown.getMarkdownSuspension();
            }
            case READMISSION -> {
                subject = notificationMarkdown.getSubjectReadmission(notification.getInitiativeName());
                markdown = notificationMarkdown.getMarkdownReadmission();
            }
            default -> {
                return false;
            }
        }

        NotificationDTO notificationDTO =
                notificationDTOMapper.map(fiscalCode, timeToLive, subject, markdown);

        String notificationId = sendNotification(notificationDTO, tokenDecrypt);

        if (notificationId == null) {
            notificationKO(notification, startTime);
            return false;
        }

        logNotificationId(notificationId);

        notificationSent(notification, notificationId);
        performanceLog(startTime);

        return true;
    }

    @Override
    public void recoverKoNotifications() {
        log.debug("[NOTIFY][RECOVER] Searching for notifications to recover");

        final LocalDateTime startTime = LocalDateTime.now();
        List<Future<Long>> workers = IntStream.range(0, parallelism)
                .mapToObj(i -> executorService.submit(() -> recover(startTime)))
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

        String fiscalCode;
        Notification notification;
        String subject;
        String markdown;
        InitiativeAdditionalInfoDTO ioTokens;

        if (anyOfNotificationQueueDTO instanceof NotificationCitizenOnQueueDTO notificationCitizenOnQueueDTO) {

            notification = notificationMapper.toEntity(notificationCitizenOnQueueDTO);

            ioTokens = getIoTokens(notificationCitizenOnQueueDTO.getInitiativeId());

            fiscalCode = decryptUserToken(notificationCitizenOnQueueDTO.getUserId());

            subject = notificationMarkdown.getSubjectInitiativePublishing();
            markdown = notificationMarkdown.getMarkdownInitiativePublishing();

        } else if (anyOfNotificationQueueDTO instanceof NotificationIbanQueueDTO notificationIbanQueueDTO) {

            notification = notificationMapper.toEntity(notificationIbanQueueDTO);

            ioTokens = getIoTokens(notificationIbanQueueDTO.getInitiativeId());

            fiscalCode = decryptUserToken(notificationIbanQueueDTO.getUserId());

            subject = notificationMarkdown.getSubjectCheckIbanKo();
            markdown = notificationMarkdown.getMarkdownCheckIbanKo();

        } else if (anyOfNotificationQueueDTO instanceof NotificationRefundQueueDTO notificationRefundOnQueueDTO) {

            BigDecimal refund = BigDecimal.valueOf(notificationRefundOnQueueDTO.getRefundReward()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_DOWN);

            notification = notificationMapper.toEntity(notificationRefundOnQueueDTO);

            ioTokens = getIoTokens(notificationRefundOnQueueDTO.getInitiativeId());

            fiscalCode = decryptUserToken(notificationRefundOnQueueDTO.getUserId());

            subject = notificationMarkdown.getSubjectRefund(notificationRefundOnQueueDTO.getStatus());
            markdown = notificationMarkdown.getMarkdownRefund(notificationRefundOnQueueDTO.getStatus(), refund);

        } else if (anyOfNotificationQueueDTO instanceof NotificationSuspensionQueueDTO notificationSuspensionQueueDTO) {

            notification = notificationMapper.toEntity(notificationSuspensionQueueDTO);

            ioTokens = getIoTokens(notificationSuspensionQueueDTO.getInitiativeId());

            fiscalCode = decryptUserToken(notificationSuspensionQueueDTO.getUserId());

            subject = notificationMarkdown.getSubjectSuspension(notificationSuspensionQueueDTO.getInitiativeName());
            markdown = notificationMarkdown.getMarkdownSuspension();

        } else if (anyOfNotificationQueueDTO instanceof NotificationReadmissionQueueDTO notificationReadmissionQueueDTO) {

            notification = notificationMapper.toEntity(notificationReadmissionQueueDTO);

            ioTokens = getIoTokens(notificationReadmissionQueueDTO.getInitiativeId());

            fiscalCode = decryptUserToken(notificationReadmissionQueueDTO.getUserId());

            subject = notificationMarkdown.getSubjectReadmission(notificationReadmissionQueueDTO.getInitiativeName());
            markdown = notificationMarkdown.getMarkdownReadmission();

        } else if (anyOfNotificationQueueDTO instanceof NotificationReminderQueueDTO notificationReminderQueueDTO) {

            notification = notificationMapper.toEntity(notificationReminderQueueDTO);

            ioTokens = getIoTokens(notificationReminderQueueDTO.getInitiativeId());

            fiscalCode = decryptUserToken(notificationReminderQueueDTO.getUserId());

            subject = notificationMarkdown.getSubjectReminderBel();
            markdown = notificationMarkdown.getMarkdownReminderBel();

        } else {
            return;
        }

        if (ioTokens == null || fiscalCode == null) {
            notificationKO(notification, startTime);
            return;
        }

        String tokenDecrypt = ioTokens.getPrimaryKey();

        if (isNotSenderAllowed(fiscalCode, tokenDecrypt)) {
            notificationKO(notification, startTime);
            return;
        }

        NotificationDTO notificationDTO =
                notificationDTOMapper.map(fiscalCode, timeToLive, subject, markdown);

        String notificationId = sendNotification(notificationDTO, tokenDecrypt);

        if (notificationId == null) {
            notificationKO(notification, startTime);
            return;
        }

        logNotificationId(notificationId);

        notificationSent(notification, notificationId);

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
            log.error("[NOTIFY] The user is not enabled to receive notifications: {}", e);
            return true;
        }
    }

    private InitiativeAdditionalInfoDTO getIoTokens(String initiativeId) {
        log.debug(NotificationConstants.IO_TOKENS);
        try {
            return initiativeRestConnector.getIOTokens(initiativeId);
        } catch (FeignException e) {
            log.error(NotificationConstants.FEIGN_KO.formatted(e.status(), e.contentUTF8()));
            return null;
        }
    }

    private void notificationKO(Notification notification, long startTime) {
        String sanitizedUserId = sanitizeString(notification.getUserId());
        String sanitizedInitiativeId = sanitizeString(notification.getInitiativeId());
        if (notification == null) {
            return;
        }
        notification.setNotificationStatus(NotificationConstants.NOTIFICATION_STATUS_KO);
        notification.setStatusKoTimestamp(LocalDateTime.now());
        notificationManagerRepository.save(notification);
        log.error("[NOTIFY] [SENT_NOTIFICATION_KO] -  Failed to send notification for user {} and initiative {}",
                sanitizedUserId, sanitizedInitiativeId);
        performanceLog(startTime);
    }

    private void notificationSent(Notification notification, String notificationId) {
        //notification.setNotificationId(notificationId);
        //notification.setNotificationStatus(NotificationConstants.NOTIFICATION_STATUS_OK);
        //notificationManagerRepository.save(notification);
        notificationManagerRepository.deleteById(notification.getId());
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
