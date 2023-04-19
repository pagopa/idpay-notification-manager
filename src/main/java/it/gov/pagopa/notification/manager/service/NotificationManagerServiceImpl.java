package it.gov.pagopa.notification.manager.service;

import feign.FeignException;
import it.gov.pagopa.notification.manager.connector.IOBackEndRestConnector;
import it.gov.pagopa.notification.manager.connector.PdvDecryptRestConnector;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationResource;
import it.gov.pagopa.notification.manager.dto.ProfileResource;
import it.gov.pagopa.notification.manager.dto.event.*;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeAdditionalInfoDTO;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationDTOMapper;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationMapper;
import it.gov.pagopa.notification.manager.event.producer.OutcomeProducer;
import it.gov.pagopa.notification.manager.model.Notification;
import it.gov.pagopa.notification.manager.model.NotificationMarkdown;
import it.gov.pagopa.notification.manager.repository.NotificationManagerRepository;
import it.gov.pagopa.notification.manager.utils.AESUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

@Service
@Slf4j
public class NotificationManagerServiceImpl implements NotificationManagerService {
    public static final String GENERIC_ERROR_LOG = "[NOTIFY][RECOVER] Something went wrong while recovering notifications";
    @Autowired
    private AESUtil aesUtil;
    @Autowired
    private OutcomeProducer outcomeProducer;
    @Autowired
    private InitiativeRestConnector initiativeRestConnector;
    @Autowired
    private IOBackEndRestConnector ioBackEndRestConnector;
    @Autowired
    private NotificationManagerRepository notificationManagerRepository;
    @Autowired
    private NotificationDTOMapper notificationDTOMapper;
    @Autowired
    private PdvDecryptRestConnector pdvDecryptRestConnector;
    @Autowired
    private NotificationMapper notificationMapper;
    @Autowired
    private NotificationMarkdown notificationMarkdown;

    @Value("${util.crypto.aes.secret-type.pbe.passphrase}")
    private String passphrase;
    @Value("${rest-client.notification.backend-io.ttl}")
    private Long timeToLive;
    @Value("${notification.manager.recover.parallelism}")
    private int parallelism;

    private ExecutorService executorService;

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

        Notification notification = notificationMapper.evaluationToNotification(evaluationDTO);
        InitiativeAdditionalInfoDTO ioTokens = null;

        try {
            log.info("[NOTIFY] Sending request to INITIATIVE");
            ioTokens = initiativeRestConnector.getIOTokens(evaluationDTO.getInitiativeId());
        } catch (FeignException e) {
            log.error(NotificationConstants.FEIGN_KO.formatted(e.status(), e.contentUTF8()));
        }

        if (ioTokens == null) {
            notificationKO(notification, startTime);
            return;
        }

        log.info(NotificationConstants.REQUEST_PDV);
        String fiscalCode = decryptUserToken(evaluationDTO.getUserId());

        if (fiscalCode == null) {
            notificationKO(notification, startTime);
            return;
        }

        log.info("[NOTIFY] Sending request to DECRYPT_TOKEN");
        String tokenDecrypt = aesUtil.decrypt(passphrase, ioTokens.getPrimaryTokenIO());

        if (isNotSenderAllowed(fiscalCode, tokenDecrypt)) {
            notificationKO(notification, startTime);
            return;
        }

        String subject = notificationMarkdown.getSubject(evaluationDTO);
        String markdown = notificationMarkdown.getMarkdown(evaluationDTO);

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

        log.info(NotificationConstants.REQUEST_PDV);
        String fiscalCode = decryptUserToken(notification.getUserId());

        if (fiscalCode == null) {
            notificationKO(notification, startTime);
            return false;
        }

        log.info("[NOTIFY] Sending request to DECRYPT_TOKEN");
        String tokenDecrypt = aesUtil.decrypt(passphrase, ioTokens.getPrimaryTokenIO());

        if (isNotSenderAllowed(fiscalCode, tokenDecrypt)) {
            notificationKO(notification, startTime);
            return false;
        }

        String subject = notificationMarkdown.getSubject(notification);
        String markdown = notificationMarkdown.getMarkdown(notification);

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

    private long recover(LocalDateTime startTime) {

        long count = 0;
        Notification n;
        while ((n = notificationManagerRepository.findKoToRecover(startTime)) != null) {
            log.info("[NOTIFY][RECOVER] Trying to recover notification with id {}", n.getId());

            n.setRetry(n.getRetry() != null ? n.getRetry() + 1 : 1);

            try {
                boolean recovered = this.notify(n);
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

        String fiscalCode = null;
        Notification notification = null;
        String subject = "";
        String markdown = "";
        InitiativeAdditionalInfoDTO ioTokens = null;

        if (anyOfNotificationQueueDTO instanceof NotificationCitizenOnQueueDTO notificationCitizenOnQueueDTO) {

            notification = notificationMapper.toEntity(notificationCitizenOnQueueDTO);

            ioTokens = getIoTokens(notificationCitizenOnQueueDTO.getInitiativeId());

            fiscalCode = decryptUserToken(notificationCitizenOnQueueDTO.getUserId());

            subject = notificationMarkdown.getSubjectInitiativePublishing();
            markdown = notificationMarkdown.getMarkdownInitiativePublishing();
        }
        if (anyOfNotificationQueueDTO instanceof NotificationIbanQueueDTO notificationIbanQueueDTO) {

            notification = notificationMapper.toEntity(notificationIbanQueueDTO);

            ioTokens = getIoTokens(notificationIbanQueueDTO.getInitiativeId());

            fiscalCode = decryptUserToken(notificationIbanQueueDTO.getUserId());

            subject = notificationMarkdown.getSubjectCheckIbanKo();
            markdown = notificationMarkdown.getMarkdownCheckIbanKo();
        }

        if (anyOfNotificationQueueDTO instanceof NotificationRefundQueueDTO notificationRefundOnQueueDTO) {

            BigDecimal refund = BigDecimal.valueOf(notificationRefundOnQueueDTO.getRefundReward()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_DOWN);

            notification = notificationMapper.toEntity(notificationRefundOnQueueDTO);

            ioTokens = getIoTokens(notificationRefundOnQueueDTO.getInitiativeId());

            fiscalCode = decryptUserToken(notificationRefundOnQueueDTO.getUserId());

            subject = notificationMarkdown.getSubjectRefund(notificationRefundOnQueueDTO.getStatus());
            markdown = notificationMarkdown.getMarkdownRefund(notificationRefundOnQueueDTO.getStatus(), refund);
        }

        if (anyOfNotificationQueueDTO instanceof NotificationSuspensionQueueDTO notificationSuspensionQueueDTO) {

            notification = notificationMapper.toEntity(notificationSuspensionQueueDTO);

            ioTokens = getIoTokens(notificationSuspensionQueueDTO.getInitiativeId());

            fiscalCode = decryptUserToken(notificationSuspensionQueueDTO.getUserId());

            subject = notificationMarkdown.getSubjectSuspension(notificationSuspensionQueueDTO.getInitiativeName());
            markdown = notificationMarkdown.getMarkdownSuspension();
        }

        if (ioTokens == null) {
            if (notification != null) {
                notificationKO(notification, startTime);
            }
            return;
        }

        if (fiscalCode == null) {
            notificationKO(notification, startTime);
            return;
        }

        String tokenDecrypt = aesUtil.decrypt(passphrase, ioTokens.getPrimaryTokenIO());

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
            ProfileResource profileResource = ioBackEndRestConnector.getProfile(fiscalCode, primaryKey);
            return !profileResource.isSenderAllowed();
        } catch (FeignException e) {
            log.error("[NOTIFY] The user is not enabled to receive notifications: {}", e.contentUTF8());
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
        notification.setNotificationStatus(NotificationConstants.NOTIFICATION_STATUS_KO);
        notification.setStatusKoTimestamp(LocalDateTime.now());
        notificationManagerRepository.save(notification);

        performanceLog(startTime);
    }

    private void notificationSent(Notification notification, String notificationId) {
        notification.setNotificationId(notificationId);
        notification.setNotificationStatus(NotificationConstants.NOTIFICATION_STATUS_OK);
        notificationManagerRepository.save(notification);
    }

    private void performanceLog(long startTime) {
        log.info(
                "[PERFORMANCE_LOG] [NOTIFY] Time occurred to perform business logic: {} ms",
                System.currentTimeMillis() - startTime);
    }

    private static void logNotificationId(String notificationId) {
        log.info("[NOTIFY] Notification ID: {}", notificationId);
    }
}
