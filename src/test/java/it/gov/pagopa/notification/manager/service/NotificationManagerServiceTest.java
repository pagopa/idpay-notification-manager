package it.gov.pagopa.notification.manager.service;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.notification.manager.connector.EmailNotificationConnector;
import it.gov.pagopa.notification.manager.connector.IOBackEndRestConnector;
import it.gov.pagopa.notification.manager.connector.PdvDecryptRestConnector;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.*;
import it.gov.pagopa.notification.manager.dto.event.*;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeAdditionalInfoDTO;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationDTOMapper;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationMapper;
import it.gov.pagopa.notification.manager.enums.Channel;
import it.gov.pagopa.notification.manager.event.producer.OutcomeProducer;
import it.gov.pagopa.notification.manager.model.Notification;
import it.gov.pagopa.notification.manager.model.NotificationMarkdown;
import it.gov.pagopa.notification.manager.repository.NotificationManagerRepository;
import it.gov.pagopa.notification.manager.repository.NotificationManagerRepositoryExtended;
import it.gov.pagopa.notification.manager.service.onboarding.OnboardingIoNotification;
import it.gov.pagopa.notification.manager.service.onboarding.OnboardingWebNotification;
import it.gov.pagopa.notification.manager.utils.AuditUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.AnyNotificationConsumer.SubTypes.*;
import static it.gov.pagopa.notification.manager.enums.Channel.IO;
import static it.gov.pagopa.notification.manager.enums.Channel.WEB;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test “hardened”:
 * - Esecutori fittizi con implementazione completa (niente metodi vuoti).
 * - Helper AutoCloseable per swap/restore dell'executor e del parallelism via try-with-resources.
 * - Nessuna interferenza tra test e niente RejectedExecutionException/InterruptedException spurie.
 */
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = NotificationManagerServiceImpl.class)
@TestPropertySource(properties = {
        "rest-client.notification.backend-io.ttl=3600",
        "notification.manager.recover.parallelism=7"
})
class NotificationManagerServiceTest {

    // ===== Helpers =====

    /** Contesto che sostituisce executor e parallelism e ripristina a fine blocco (try-with-resources). */
    private static final class ExecSwap implements AutoCloseable {
        private final NotificationManagerServiceImpl svc;
        private final ExecutorService originalExec;
        private final int originalPar;

        ExecSwap(NotificationManagerServiceImpl svc, ExecutorService newExec, int newPar) {
            this.svc = svc;
            this.originalExec = (ExecutorService) ReflectionTestUtils.getField(svc, "executorService");
            this.originalPar = (Integer) ReflectionTestUtils.getField(svc, "parallelism");
            ReflectionTestUtils.setField(svc, "executorService", newExec);
            ReflectionTestUtils.setField(svc, "parallelism", newPar);
        }

        @Override
        public void close() {
            ReflectionTestUtils.setField(svc, "executorService", originalExec);
            ReflectionTestUtils.setField(svc, "parallelism", originalPar);
            Thread.interrupted(); // pulizia stato interrupt del thread corrente
        }
    }

    /** Executor testuale che restituisce sempre il Future passato in costruzione (usato per Execution/Interrupted). */
    private static final class FixedFutureExecutor extends AbstractExecutorService {
        private final Future<?> fixedFuture;
        /** stato “shutdown” separato da “terminated” per evitare duplicazioni tra i due metodi */
        private final AtomicBoolean shutdown = new AtomicBoolean(false);
        private final AtomicBoolean terminated = new AtomicBoolean(false);

        FixedFutureExecutor(Future<?> fixedFuture) {
            this.fixedFuture = fixedFuture;
        }

        @Override
        public void shutdown() {
            // implementazione minimale ma non vuota: marca entrambi gli stati
            shutdown.set(true);
            terminated.set(true);
        }

        @Override
        public List<Runnable> shutdownNow() {
            // non ci sono task accodati: restituiamo lista vuota
            shutdown();
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return shutdown.get();
        }

        @Override
        public boolean isTerminated() {
            // non identico a isShutdown per evitare duplicati: vero solo quando abbiamo marcato terminated
            return terminated.get();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            // non ci sono thread attivi: ritorna sempre true
            return true;
        }

        @Override
        public void execute(Runnable command) {
            // Non usato nei test: esplicitiamo chiaramente
            throw new UnsupportedOperationException("execute() not used in test");
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Future<T> submit(Callable<T> task) {
            // ritorna sempre il future fittizio fornito (ExecutionException/InterruptedException simulati a richiesta)
            return (Future<T>) fixedFuture;
        }
    }

    /** Executor reale a thread singolo per eseguire davvero i worker di recover (nessun code smell). */
    private static final class SingleThreadDirectExecutor extends AbstractExecutorService {
        private final ExecutorService delegate = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "test-recovery-thread");
            t.setDaemon(true);
            return t;
        });

        @Override public void shutdown() { delegate.shutdown(); }
        @Override public List<Runnable> shutdownNow() { return delegate.shutdownNow(); }
        @Override public boolean isShutdown() { return delegate.isShutdown(); }
        @Override public boolean isTerminated() { return delegate.isTerminated(); }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }
        @Override public void execute(Runnable command) { delegate.execute(command); }
        @Override public <T> Future<T> submit(Callable<T> task) { return delegate.submit(task); }
    }

    private ExecSwap swapToRealExecutor() {
        // Inizializza l’executor del service con un thread vero e parallelism 1
        SingleThreadDirectExecutor exec = new SingleThreadDirectExecutor();
        ReflectionTestUtils.invokeMethod(notificationManagerService, "init"); // inizializza se necessario
        return new ExecSwap(notificationManagerService, exec, 1);
    }

    private ExecSwap swapToFixedFuture(Future<Long> f) {
        return new ExecSwap(notificationManagerService, new FixedFutureExecutor(f), 1);
    }

    // ===== Dati fissi =====
    private static final String TEST_TOKEN = "TEST_TOKEN";
    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    private static final LocalDateTime TEST_DATE = LocalDateTime.now();
    private static final LocalDate TEST_DATE_ONLY_DATE = LocalDate.now();
    private static final FiscalCodeResource FISCAL_CODE_RESOURCE = new FiscalCodeResource();
    private static final List<String> PREFERRED_LANGUAGES = new ArrayList<>();
    private static final ProfileResource PROFILE_RESOURCE = new ProfileResource(true, PREFERRED_LANGUAGES);
    private static final ProfileResource PROFILE_RESOURCE_KO = new ProfileResource();
    private static final String FISCAL_CODE = "TEST_FISCAL_CODE";
    private static final String PRIMARY_KEY = "PRIMARY_KEY";
    private static final String SECONDARY_KEY = "SECONDARY_KEY";
    private static final String TEST_NOTIFICATION_ID = "NOTIFICATION_ID";
    private static final Long TTL = 3600L;
    private static final String SUBJECT = "SUBJECT";
    private static final String TOKEN = "TOKEN";
    private static final String MARKDOWN = "MARKDOWN";
    private static final String OPERATION_TYPE = "OPERATION_TYPE";
    private static final String SERVICE_ID = "SERVICE_ID";
    private static final String IBAN = "IBAN";
    private static final String INITIATIVE_NAME = "INITIATIVE_NAME";
    private static final String ORGANIZATION_NAME = "ORGANIZATION_NAME";
    private static final String OPERATION_TYPE_DELETE_INITIATIVE = "DELETE_INITIATIVE";
    private static final int PAGE_SIZE = 100;

    private static final EvaluationDTO EVALUATION_DTO =
            new EvaluationDTO(
                    TEST_TOKEN, INITIATIVE_ID, INITIATIVE_ID, TEST_DATE_ONLY_DATE, INITIATIVE_ID, ORGANIZATION_NAME,
                    NotificationConstants.STATUS_ONBOARDING_OK, TEST_DATE, TEST_DATE, List.of(),
                    50000L, 1L, true, null, IO, null, null, null, null
            );

    private static final EvaluationDTO EVALUATION_DTO_WEB = new EvaluationDTO(
            TEST_TOKEN, INITIATIVE_ID, INITIATIVE_ID, TEST_DATE_ONLY_DATE, INITIATIVE_ID, ORGANIZATION_NAME,
            NotificationConstants.STATUS_ONBOARDING_OK, TEST_DATE, TEST_DATE, List.of(),
            50000L, 1L, true, "user@email.com", WEB, null, null, null, null
    );

    private static final FiscalCodeDTO FISCAL_CODE_DTO = new FiscalCodeDTO(FISCAL_CODE);
    private static final NotificationResource NOTIFICATION_RESOURCE = new NotificationResource();
    private static final NotificationDTO NOTIFICATION_DTO = new NotificationDTO();
    private static final Notification NOTIFICATION =
            Notification.builder()
                    .notificationDate(LocalDateTime.now())
                    .initiativeId(EVALUATION_DTO.getInitiativeId())
                    .userId(EVALUATION_DTO.getUserId())
                    .onboardingOutcome(EVALUATION_DTO.getStatus())
                    .rejectReasons(EVALUATION_DTO.getOnboardingRejectionReasons())
                    .channel(Channel.IO)
                    .build();
    private static final ServiceResource SERVICE_RESOURCE = new ServiceResource();
    private static final InitiativeAdditionalInfoDTO INITIATIVE_ADDITIONAL_INFO_DTO = InitiativeAdditionalInfoDTO.builder()
            .primaryKey(TOKEN)
            .secondaryKey(SECONDARY_KEY)
            .build();

    private static final NotificationIbanQueueDTO NOTIFICATION_IBAN_QUEUE_DTO = NotificationIbanQueueDTO.builder()
            .operationType(OPERATION_TYPE)
            .userId(TEST_TOKEN)
            .initiativeId(INITIATIVE_ID)
            .serviceId(SERVICE_ID)
            .iban(IBAN)
            .build();

    private static final NotificationRefundQueueDTO NOTIFICATION_REFUND_QUEUE_DTO = NotificationRefundQueueDTO.builder()
            .operationType(OPERATION_TYPE)
            .refundReward(10000L)
            .userId(TEST_TOKEN)
            .initiativeId(INITIATIVE_ID)
            .serviceId(SERVICE_ID)
            .status("ACCEPTED")
            .build();

    private static final NotificationCitizenOnQueueDTO NOTIFICATION_CITIZEN_ON_QUEUE_DTO = NotificationCitizenOnQueueDTO.builder()
            .initiativeName(INITIATIVE_NAME)
            .initiativeId(INITIATIVE_ID)
            .serviceId(SERVICE_ID)
            .operationType(OPERATION_TYPE)
            .userId(TEST_TOKEN)
            .build();

    private static final Notification KO_NOTIFICATION_FIRST_RETRY = Notification.builder()
            .notificationDate(TEST_DATE)
            .initiativeId(EVALUATION_DTO.getInitiativeId())
            .userId(EVALUATION_DTO.getUserId())
            .onboardingOutcome(EVALUATION_DTO.getStatus())
            .rejectReasons(EVALUATION_DTO.getOnboardingRejectionReasons())
            .notificationStatus(NotificationConstants.NOTIFICATION_STATUS_RECOVER)
            .retryDate(LocalDateTime.now())
            .operationType(ONBOARDING)
            .channel(Channel.IO)
            .build();

    private static final Notification KO_REFUND_NOTIFICATION_FIRST_RETRY = Notification.builder()
            .notificationDate(TEST_DATE)
            .initiativeId(INITIATIVE_ID)
            .userId(TEST_TOKEN)
            .notificationStatus(NotificationConstants.NOTIFICATION_STATUS_RECOVER)
            .retryDate(LocalDateTime.now())
            .operationType(REFUND)
            .refundStatus("ACCEPTED")
            .refundReward(BigDecimal.valueOf(100))
            .channel(Channel.IO)
            .build();

    private static final Notification KO_CHECK_IBAN_NOTIFICATION_FIRST_RETRY = Notification.builder()
            .notificationDate(TEST_DATE)
            .initiativeId(INITIATIVE_ID)
            .userId(TEST_TOKEN)
            .notificationStatus(NotificationConstants.NOTIFICATION_STATUS_RECOVER)
            .retryDate(LocalDateTime.now())
            .operationType(CHECKIBAN_KO)
            .channel(Channel.IO)
            .build();

    private static final Notification KO_SUSPENSION_NOTIFICATION_FIRST_RETRY = Notification.builder()
            .notificationDate(TEST_DATE)
            .initiativeId(INITIATIVE_ID)
            .initiativeName(INITIATIVE_NAME)
            .userId(TEST_TOKEN)
            .notificationStatus(NotificationConstants.NOTIFICATION_STATUS_RECOVER)
            .retryDate(LocalDateTime.now())
            .operationType(SUSPENSION)
            .channel(Channel.IO)
            .build();

    private static final Notification KO_READMISSION_NOTIFICATION_FIRST_RETRY = Notification.builder()
            .notificationDate(TEST_DATE)
            .initiativeId(INITIATIVE_ID)
            .initiativeName(INITIATIVE_NAME)
            .userId(TEST_TOKEN)
            .notificationStatus(NotificationConstants.NOTIFICATION_STATUS_RECOVER)
            .retryDate(LocalDateTime.now())
            .operationType(READMISSION)
            .channel(Channel.IO)
            .build();

    private static final Notification KO_NOTIFICATION_EMAIL_FIRST_RETRY = Notification.builder()
            .notificationDate(TEST_DATE)
            .initiativeId(INITIATIVE_ID)
            .initiativeName(INITIATIVE_NAME)
            .userId(TEST_TOKEN)
            .notificationStatus(NotificationConstants.NOTIFICATION_STATUS_RECOVER)
            .retryDate(LocalDateTime.now())
            .operationType(ONBOARDING)
            .channel(Channel.WEB)
            .build();

    private static final Notification KO_NOTIFICATION_WHITELIST = Notification.builder()
            .notificationDate(TEST_DATE)
            .initiativeId(INITIATIVE_ID)
            .userId(TEST_TOKEN)
            .notificationStatus(NotificationConstants.NOTIFICATION_STATUS_RECOVER)
            .retryDate(LocalDateTime.now())
            .operationType(ALLOWED_CITIZEN_PUBLISH)
            .channel(Channel.IO)
            .build();

    private static final Notification KO_NOTIFICATION_N_RETRY = Notification.builder()
            .notificationDate(TEST_DATE)
            .initiativeId(EVALUATION_DTO.getInitiativeId())
            .userId(EVALUATION_DTO.getUserId())
            .onboardingOutcome(EVALUATION_DTO.getStatus())
            .rejectReasons(EVALUATION_DTO.getOnboardingRejectionReasons())
            .notificationStatus(NotificationConstants.NOTIFICATION_STATUS_RECOVER)
            .retry(2)
            .retryDate(LocalDateTime.now())
            .operationType(ONBOARDING)
            .channel(Channel.IO)
            .build();

    private static final NotificationSuspensionQueueDTO NOTIFICATION_SUSPENSION_QUEUE_DTO = NotificationSuspensionQueueDTO.builder()
            .initiativeName(INITIATIVE_NAME)
            .initiativeId(INITIATIVE_ID)
            .operationType(OPERATION_TYPE)
            .userId(TEST_TOKEN)
            .build();

    private static final NotificationReadmissionQueueDTO NOTIFICATION_READMISSION_QUEUE_DTO = NotificationReadmissionQueueDTO.builder()
            .initiativeName(INITIATIVE_NAME)
            .initiativeId(INITIATIVE_ID)
            .operationType(OPERATION_TYPE)
            .userId(TEST_TOKEN)
            .build();

    private static final Notification NOTIFICATION_SUSPENSION = Notification.builder()
            .notificationDate(LocalDateTime.now())
            .notificationStatus(NotificationConstants.NOTIFICATION_STATUS_OK)
            .initiativeName(INITIATIVE_NAME)
            .initiativeId(INITIATIVE_ID)
            .operationType(OPERATION_TYPE)
            .userId(TEST_TOKEN)
            .channel(Channel.IO)
            .build();

    private static final Notification NOTIFICATION_READMISSION = Notification.builder()
            .notificationDate(LocalDateTime.now())
            .notificationStatus(NotificationConstants.NOTIFICATION_STATUS_OK)
            .initiativeName(INITIATIVE_NAME)
            .initiativeId(INITIATIVE_ID)
            .operationType(OPERATION_TYPE)
            .userId(TEST_TOKEN)
            .channel(Channel.IO)
            .build();

    static {
        FISCAL_CODE_RESOURCE.setPii(FISCAL_CODE);
        PROFILE_RESOURCE.setSenderAllowed(true);
        PROFILE_RESOURCE_KO.setSenderAllowed(false);
        NOTIFICATION_RESOURCE.setId(TEST_NOTIFICATION_ID);

        MessageContent messageContent = new MessageContent();
        messageContent.setSubject(SUBJECT);
        messageContent.setMarkdown(MARKDOWN);
        NOTIFICATION_DTO.setFiscalCode(FISCAL_CODE);
        NOTIFICATION_DTO.setTimeToLive(TTL);
        NOTIFICATION_DTO.setContent(messageContent);
        SERVICE_RESOURCE.setPrimaryKey(PRIMARY_KEY);
    }

    @Autowired
    NotificationManagerServiceImpl notificationManagerService;
    @MockBean OutcomeProducer outcomeProducer;
    @MockBean InitiativeRestConnector initiativeRestConnector;
    @MockBean IOBackEndRestConnector ioBackEndRestConnector;
    @MockBean EmailNotificationConnector emailNotificationConnector;
    @MockBean NotificationManagerRepository notificationManagerRepository;
    @MockBean NotificationManagerRepositoryExtended notificationManagerRepositoryExtended;
    @MockBean NotificationDTOMapper notificationDTOMapper;
    @MockBean PdvDecryptRestConnector pdvDecryptRestConnector;
    @MockBean NotificationMapper notificationMapper;
    @MockBean NotificationMarkdown notificationMarkdown;
    @MockBean AuditUtilities auditUtilities;
    @MockBean OnboardingIoNotification onboardingIoNotification;
    @MockBean OnboardingWebNotification onboardingWebNotification;

    // ===== TESTS =====

    @Test
    void sendToQueue() {
        doNothing().when(outcomeProducer).sendOutcome(EVALUATION_DTO);
        notificationManagerService.addOutcome(EVALUATION_DTO);
        verify(outcomeProducer, times(1)).sendOutcome(EVALUATION_DTO);
    }

    @Test
    void notify_onboardingKo_shouldSkipNotification() {
        EvaluationDTO evaluationDTO = new EvaluationDTO(
                TEST_TOKEN, INITIATIVE_ID, INITIATIVE_ID, TEST_DATE_ONLY_DATE, INITIATIVE_ID, ORGANIZATION_NAME,
                NotificationConstants.STATUS_ONBOARDING_OK, TEST_DATE, TEST_DATE,
                List.of(
                        new OnboardingRejectionReason(
                                OnboardingRejectionReason.OnboardingRejectionReasonType.FAMILY_CRITERIA_KO,
                                OnboardingRejectionReason.OnboardingRejectionReasonCode.FAMILY_CRITERIA_FAIL,
                                null, null, null),
                        new OnboardingRejectionReason(
                                OnboardingRejectionReason.OnboardingRejectionReasonType.ISEE_TYPE_KO,
                                OnboardingRejectionReason.OnboardingRejectionReasonCode.ISEE_TYPE_FAIL,
                                null, null, null)
                ),
                50000L, 1L, true, "user@mail.com", IO, null, null, null, null
        );

        notificationManagerService.notify(evaluationDTO);

        verify(notificationMapper, times(0)).evaluationToNotification(evaluationDTO);
        verify(initiativeRestConnector, times(0)).getIOTokens(evaluationDTO.getInitiativeId());
        verify(notificationManagerRepository, times(0)).save(any(Notification.class));
    }

    @Test
    void notify_ok() {
        when(notificationMapper.evaluationToNotification(EVALUATION_DTO)).thenReturn(NOTIFICATION);
        when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId())).thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(ioBackEndRestConnector.getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN)))
                .thenReturn(PROFILE_RESOURCE);
        when(ioBackEndRestConnector.notify(NOTIFICATION_DTO, TOKEN)).thenReturn(NOTIFICATION_RESOURCE);
        when(onboardingIoNotification.processNotification(any())).thenReturn("ID");

        assertDoesNotThrow(() -> notificationManagerService.notify(EVALUATION_DTO));
        verifyNoMoreInteractions(notificationManagerRepository);
    }

    @Test
    void notify_appIo_ioTokensNull_triggersNotificationKO() {
        EvaluationDTO evaluationDTO = new EvaluationDTO(
                TEST_TOKEN, INITIATIVE_ID, INITIATIVE_ID, TEST_DATE_ONLY_DATE, INITIATIVE_ID, ORGANIZATION_NAME,
                NotificationConstants.STATUS_ONBOARDING_OK, TEST_DATE, TEST_DATE, List.of(), 50000L, 1L,
                true, null, IO, null, null, null, null
        );

        Notification notification = Notification.builder()
                .notificationDate(LocalDateTime.now())
                .initiativeId(evaluationDTO.getInitiativeId())
                .userId(evaluationDTO.getUserId())
                .onboardingOutcome(evaluationDTO.getStatus())
                .rejectReasons(evaluationDTO.getOnboardingRejectionReasons())
                .build();
        when(notificationMapper.evaluationToNotification(evaluationDTO)).thenReturn(notification);
        when(initiativeRestConnector.getIOTokens(evaluationDTO.getInitiativeId())).thenReturn(null);

        notificationManagerService.notify(evaluationDTO);

        verify(notificationManagerRepository, times(1)).save(argThat(n ->
                NotificationConstants.NOTIFICATION_STATUS_KO.equals(n.getNotificationStatus())));
    }

    @Test
    void notify_unsupportedChannel_logsWarning() {
        EvaluationDTO evaluationDTO = mock(EvaluationDTO.class);
        when(evaluationDTO.getUserId()).thenReturn("user@email.com");

        var fakeChannel = mock(it.gov.pagopa.notification.manager.enums.Channel.class);
        when(fakeChannel.isAppIo()).thenReturn(false);
        when(fakeChannel.isWeb()).thenReturn(false);
        when(evaluationDTO.getChannel()).thenReturn(fakeChannel);

        assertDoesNotThrow(() -> notificationManagerService.notify(evaluationDTO));
    }

    @Test
    void notify_webNotification() {
        EvaluationDTO evaluationDTO = new EvaluationDTO(
                TEST_TOKEN, INITIATIVE_ID, INITIATIVE_ID, TEST_DATE_ONLY_DATE, INITIATIVE_ID, ORGANIZATION_NAME,
                NotificationConstants.STATUS_ONBOARDING_OK, TEST_DATE, TEST_DATE, List.of(),
                50000L, 1L, true, null, WEB, null, null, null, null
        );

        when(onboardingWebNotification.processNotification(evaluationDTO)).thenReturn(null);
        when(notificationMapper.evaluationToNotification(evaluationDTO)).thenReturn(NOTIFICATION);
        notificationManagerService.notify(evaluationDTO);
        verify(onboardingWebNotification, times(1)).processNotification(any());
    }

    @Test
    void notify_webNotification_scapeNotification() {
        EvaluationDTO evaluationDTO = new EvaluationDTO(
                TEST_TOKEN, INITIATIVE_ID, INITIATIVE_ID, TEST_DATE_ONLY_DATE, INITIATIVE_ID, ORGANIZATION_NAME,
                NotificationConstants.STATUS_ONBOARDING_DEMANDED, TEST_DATE, TEST_DATE, List.of(),
                50000L, 1L, true, null, WEB, null, null, null, null
        );

        notificationManagerService.notify(evaluationDTO);
        verify(onboardingWebNotification, never()).processNotification(any());
    }

    @Test
    void notify_ko() {
        EvaluationDTO evaluationDTO =
                new EvaluationDTO(
                        TEST_TOKEN, INITIATIVE_ID, INITIATIVE_ID, TEST_DATE_ONLY_DATE, INITIATIVE_ID, ORGANIZATION_NAME,
                        NotificationConstants.STATUS_ONBOARDING_OK, TEST_DATE, TEST_DATE, null,
                        50000L, 1L, true, null, IO, null, null, null, null
                );

        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(ioBackEndRestConnector.getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN)))
                .thenReturn(PROFILE_RESOURCE);
        when(initiativeRestConnector.getIOTokens(evaluationDTO.getInitiativeId())).thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        when(notificationMarkdown.getSubject(evaluationDTO)).thenReturn(SUBJECT);
        when(notificationMarkdown.getMarkdown(evaluationDTO)).thenReturn(MARKDOWN);
        when(notificationDTOMapper.map(eq(FISCAL_CODE), anyLong(), anyString(), anyString()))
                .thenReturn(NOTIFICATION_DTO);
        Notification notification =
                Notification.builder()
                        .notificationDate(LocalDateTime.now())
                        .initiativeId(evaluationDTO.getInitiativeId())
                        .userId(evaluationDTO.getUserId())
                        .onboardingOutcome(evaluationDTO.getStatus())
                        .rejectReasons(evaluationDTO.getOnboardingRejectionReasons())
                        .build();
        when(notificationMapper.evaluationToNotification(evaluationDTO)).thenReturn(notification);
        Request request = Request.create(Request.HttpMethod.POST, "url", new HashMap<>(), null, new RequestTemplate());
        doThrow(new FeignException.BadRequest("", request, new byte[0], null))
                .when(ioBackEndRestConnector).notify(NOTIFICATION_DTO, TOKEN);

        try {
            notificationManagerService.notify(evaluationDTO);
        } catch (FeignException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.status());
        }
        verify(notificationManagerRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void notify_ko_get_io_tokens() {
        EvaluationDTO evaluationDTO =
                new EvaluationDTO(
                        TEST_TOKEN, INITIATIVE_ID, INITIATIVE_ID, TEST_DATE_ONLY_DATE, INITIATIVE_ID, ORGANIZATION_NAME,
                        NotificationConstants.STATUS_ONBOARDING_OK, TEST_DATE, TEST_DATE,
                        List.of(
                                new OnboardingRejectionReason(
                                        OnboardingRejectionReason.OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL,
                                        OnboardingRejectionReason.OnboardingRejectionReasonCode.AUTOMATED_CRITERIA_ISEE_FAIL, null, null, null),
                                new OnboardingRejectionReason(
                                        OnboardingRejectionReason.OnboardingRejectionReasonType.ISEE_TYPE_KO,
                                        OnboardingRejectionReason.OnboardingRejectionReasonCode.ISEE_TYPE_FAIL, null, null, null)
                        ),
                        50000L, 1L, true, null, IO, null, null, null, null
                );

        Request request = Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(initiativeRestConnector).getIOTokens(evaluationDTO.getInitiativeId());
        when(notificationMapper.evaluationToNotification(evaluationDTO)).thenReturn(NOTIFICATION);

        notificationManagerService.notify(evaluationDTO);
        verify(notificationManagerRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void notify_ko_user_not_allowed_feign() {
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId())).thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Request request = Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(ioBackEndRestConnector)
                .getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN));
        when(notificationMapper.evaluationToNotification(EVALUATION_DTO)).thenReturn(NOTIFICATION);

        notificationManagerService.notify(EVALUATION_DTO);
        verify(notificationManagerRepository, times(0)).save(any(Notification.class));
    }

    @Test
    void notify_ko_user_not_allowed() {
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId())).thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        when(ioBackEndRestConnector.getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN)))
                .thenReturn(PROFILE_RESOURCE_KO);
        when(notificationMapper.evaluationToNotification(EVALUATION_DTO)).thenReturn(NOTIFICATION);

        notificationManagerService.notify(EVALUATION_DTO);
        verify(notificationManagerRepository, times(0)).save(any(Notification.class));
    }

    @Test
    void notify_ko_no_cf() {
        when(initiativeRestConnector.getIOTokens(EVALUATION_DTO_WEB.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Request request = Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(pdvDecryptRestConnector).getPii(TEST_TOKEN);
        when(notificationMapper.evaluationToNotification(EVALUATION_DTO)).thenReturn(NOTIFICATION);

        notificationManagerService.notify(EVALUATION_DTO);
        verify(notificationManagerRepository, times(0)).save(any(Notification.class));
    }

    @Test
    void sendNotificationFromOperationType_checkiban_ok() {
        when(notificationMapper.toEntity(NOTIFICATION_IBAN_QUEUE_DTO)).thenReturn(NOTIFICATION);
        when(initiativeRestConnector.getIOTokens(NOTIFICATION_IBAN_QUEUE_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(notificationMarkdown.getSubjectCheckIbanKo()).thenReturn(SUBJECT);
        when(notificationMarkdown.getMarkdownCheckIbanKo()).thenReturn(MARKDOWN);
        when(ioBackEndRestConnector.getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN)))
                .thenReturn(PROFILE_RESOURCE);
        when(notificationDTOMapper.map(eq(FISCAL_CODE), anyLong(), anyString(), anyString()))
                .thenReturn(NOTIFICATION_DTO);
        when(ioBackEndRestConnector.notify(NOTIFICATION_DTO, TOKEN)).thenReturn(NOTIFICATION_RESOURCE);

        assertDoesNotThrow(() -> notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_IBAN_QUEUE_DTO));
    }

    @Test
    void sendNotificationFromOperationType_not_expected() {
        NotificationQueueDTO notificationQueue = new NotificationQueueDTO("", "", "", "");
        assertDoesNotThrow(() -> notificationManagerService.sendNotificationFromOperationType(notificationQueue));
        verify(notificationManagerRepository, times(0)).save(any());
    }

    @Test
    void sendNotificationFromOperationType_checkiban_ko() {
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(ioBackEndRestConnector.getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN)))
                .thenReturn(PROFILE_RESOURCE);
        when(initiativeRestConnector.getIOTokens(NOTIFICATION_IBAN_QUEUE_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        when(notificationMarkdown.getSubjectCheckIbanKo()).thenReturn(SUBJECT);
        when(notificationMarkdown.getMarkdownCheckIbanKo()).thenReturn(MARKDOWN);
        when(notificationDTOMapper.map(eq(FISCAL_CODE), anyLong(), anyString(), anyString()))
                .thenReturn(NOTIFICATION_DTO);
        when(notificationMapper.toEntity(NOTIFICATION_IBAN_QUEUE_DTO)).thenReturn(NOTIFICATION);

        Request request = Request.create(Request.HttpMethod.POST, "url", new HashMap<>(), null, new RequestTemplate());
        doThrow(new FeignException.BadRequest("", request, new byte[0], null))
                .when(ioBackEndRestConnector).notify(NOTIFICATION_DTO, TOKEN);

        try {
            notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_IBAN_QUEUE_DTO);
        } catch (FeignException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.status());
        }
        verify(notificationManagerRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void sendNotificationFromOperationType_allowed_citzen_ok() {
        when(notificationMapper.toEntity(NOTIFICATION_CITIZEN_ON_QUEUE_DTO)).thenReturn(NOTIFICATION);
        when(initiativeRestConnector.getIOTokens(INITIATIVE_ID)).thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(notificationMarkdown.getMarkdownInitiativePublishing()).thenReturn(SUBJECT);
        when(notificationMarkdown.getSubjectInitiativePublishing()).thenReturn(MARKDOWN);
        when(ioBackEndRestConnector.getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN)))
                .thenReturn(PROFILE_RESOURCE);
        when(notificationDTOMapper.map(eq(FISCAL_CODE), anyLong(), anyString(), anyString()))
                .thenReturn(NOTIFICATION_DTO);
        when(ioBackEndRestConnector.notify(NOTIFICATION_DTO, TOKEN)).thenReturn(NOTIFICATION_RESOURCE);

        assertDoesNotThrow(() -> notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_CITIZEN_ON_QUEUE_DTO));
    }

    @Test
    void checkIbanKo_ko_get_io_tokens() {
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        Request request = Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(initiativeRestConnector).getIOTokens(NOTIFICATION_IBAN_QUEUE_DTO.getInitiativeId());
        when(notificationMapper.toEntity(NOTIFICATION_IBAN_QUEUE_DTO)).thenReturn(NOTIFICATION);

        notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_IBAN_QUEUE_DTO);
        verify(notificationManagerRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void citizen_ko_get_io_tokens() {
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        Request request = Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(initiativeRestConnector).getIOTokens(NOTIFICATION_IBAN_QUEUE_DTO.getInitiativeId());
        when(notificationMapper.toEntity(NOTIFICATION_CITIZEN_ON_QUEUE_DTO)).thenReturn(NOTIFICATION);

        notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_CITIZEN_ON_QUEUE_DTO);
        verify(notificationManagerRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void checkIbanKo_ko_user_not_allowed_feign() {
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(initiativeRestConnector.getIOTokens(NOTIFICATION_IBAN_QUEUE_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Request request = Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(ioBackEndRestConnector)
                .getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN));
        when(notificationMapper.toEntity(NOTIFICATION_IBAN_QUEUE_DTO)).thenReturn(NOTIFICATION);

        notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_IBAN_QUEUE_DTO);
        verify(notificationManagerRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void citizen_ko_user_not_allowed_feign() {
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(initiativeRestConnector.getIOTokens(INITIATIVE_ID)).thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Request request = Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(ioBackEndRestConnector)
                .getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN));
        when(notificationMapper.toEntity(NOTIFICATION_CITIZEN_ON_QUEUE_DTO)).thenReturn(NOTIFICATION);

        notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_CITIZEN_ON_QUEUE_DTO);
        verify(notificationManagerRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void checkIbanKo_ko_user_not_allowed() {
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(initiativeRestConnector.getIOTokens(NOTIFICATION_IBAN_QUEUE_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        when(ioBackEndRestConnector.getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN)))
                .thenReturn(PROFILE_RESOURCE_KO);
        when(notificationMapper.toEntity(NOTIFICATION_IBAN_QUEUE_DTO)).thenReturn(NOTIFICATION);

        notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_IBAN_QUEUE_DTO);
        verify(notificationManagerRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void citizen_ko_user_not_allowed() {
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(initiativeRestConnector.getIOTokens(INITIATIVE_ID)).thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        when(ioBackEndRestConnector.getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN)))
                .thenReturn(PROFILE_RESOURCE_KO);
        when(notificationMapper.toEntity(NOTIFICATION_CITIZEN_ON_QUEUE_DTO)).thenReturn(NOTIFICATION);

        notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_CITIZEN_ON_QUEUE_DTO);
        verify(notificationManagerRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void checkIbanKo_ko_no_cf() {
        when(initiativeRestConnector.getIOTokens(NOTIFICATION_IBAN_QUEUE_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Request request = Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(pdvDecryptRestConnector).getPii(TEST_TOKEN);
        when(notificationMapper.toEntity(NOTIFICATION_IBAN_QUEUE_DTO)).thenReturn(NOTIFICATION);

        notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_IBAN_QUEUE_DTO);
        verify(notificationManagerRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void citizen_ko_no_cf() {
        when(initiativeRestConnector.getIOTokens(INITIATIVE_ID)).thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Request request = Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(pdvDecryptRestConnector).getPii(TEST_TOKEN);
        when(notificationMapper.toEntity(NOTIFICATION_CITIZEN_ON_QUEUE_DTO)).thenReturn(NOTIFICATION);

        notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_CITIZEN_ON_QUEUE_DTO);
        verify(notificationManagerRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void sendNotificationFromOperationType_notification_notNull_ioTokens_notNull() {
        when(notificationMapper.toEntity(NOTIFICATION_CITIZEN_ON_QUEUE_DTO)).thenReturn(NOTIFICATION);
        when(initiativeRestConnector.getIOTokens(INITIATIVE_ID)).thenReturn(null);
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(notificationMarkdown.getMarkdownInitiativePublishing()).thenReturn(SUBJECT);
        when(notificationMarkdown.getSubjectInitiativePublishing()).thenReturn(MARKDOWN);

        assertDoesNotThrow(() -> notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_CITIZEN_ON_QUEUE_DTO));
        verify(notificationManagerRepository, times(1)).save(NOTIFICATION);
    }

    @Test
    void sendNotificationFromOperationType_checkiban_notification_Null_ioTokens_null() {
        Notification dto = Mockito.mock(Notification.class);

        when(dto.getUserId()).thenReturn(null);
        when(dto.getInitiativeId()).thenReturn(null);
        when(notificationMapper.toEntity(NOTIFICATION_IBAN_QUEUE_DTO)).thenReturn(dto);
        when(initiativeRestConnector.getIOTokens(INITIATIVE_ID)).thenReturn(null);
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(notificationMarkdown.getMarkdownInitiativePublishing()).thenReturn(SUBJECT);
        when(notificationMarkdown.getSubjectInitiativePublishing()).thenReturn(MARKDOWN);

        assertDoesNotThrow(() -> notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_IBAN_QUEUE_DTO));
    }

    @Test
    void sendNotificationFromOperationType_refund_ok() {
        when(notificationMapper.toEntity(NOTIFICATION_REFUND_QUEUE_DTO)).thenReturn(NOTIFICATION);
        when(initiativeRestConnector.getIOTokens(NOTIFICATION_REFUND_QUEUE_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(notificationMarkdown.getSubjectRefund(NOTIFICATION_REFUND_QUEUE_DTO.getStatus())).thenReturn(SUBJECT);
        when(notificationMarkdown.getMarkdownRefund(eq(NOTIFICATION_REFUND_QUEUE_DTO.getStatus()), any()))
                .thenReturn(MARKDOWN);
        when(ioBackEndRestConnector.getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN)))
                .thenReturn(PROFILE_RESOURCE);
        when(notificationDTOMapper.map(eq(FISCAL_CODE), anyLong(), anyString(), anyString()))
                .thenReturn(NOTIFICATION_DTO);
        when(ioBackEndRestConnector.notify(NOTIFICATION_DTO, TOKEN)).thenReturn(NOTIFICATION_RESOURCE);

        assertDoesNotThrow(() -> notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_REFUND_QUEUE_DTO));
    }

    @Test
    void recoverKoNotifications() {
        when(notificationManagerRepository.findKoToRecover(any(LocalDateTime.class)))
                .thenReturn(KO_NOTIFICATION_FIRST_RETRY, KO_REFUND_NOTIFICATION_FIRST_RETRY,
                        KO_CHECK_IBAN_NOTIFICATION_FIRST_RETRY, KO_SUSPENSION_NOTIFICATION_FIRST_RETRY,
                        KO_READMISSION_NOTIFICATION_FIRST_RETRY, KO_NOTIFICATION_N_RETRY, KO_NOTIFICATION_EMAIL_FIRST_RETRY, null);

        when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(ioBackEndRestConnector.getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN)))
                .thenReturn(PROFILE_RESOURCE);

        when(notificationMarkdown.getSubject(any(Notification.class))).thenReturn(SUBJECT);
        when(notificationMarkdown.getSubjectCheckIbanKo()).thenReturn(SUBJECT);
        when(notificationMarkdown.getSubjectRefund(anyString())).thenReturn(SUBJECT);
        when(notificationMarkdown.getSubjectSuspension(anyString())).thenReturn(SUBJECT);
        when(notificationMarkdown.getSubjectReadmission(anyString())).thenReturn(SUBJECT);

        when(notificationMarkdown.getMarkdown(any(Notification.class))).thenReturn(MARKDOWN);
        when(notificationMarkdown.getMarkdownCheckIbanKo()).thenReturn(MARKDOWN);
        when(notificationMarkdown.getMarkdownRefund(anyString(), any())).thenReturn(MARKDOWN);
        when(notificationMarkdown.getMarkdownSuspension()).thenReturn(MARKDOWN);
        when(notificationMarkdown.getMarkdownReadmission()).thenReturn(MARKDOWN);

        when(notificationDTOMapper.map(eq(FISCAL_CODE), anyLong(), anyString(), anyString()))
                .thenReturn(NOTIFICATION_DTO);
        when(ioBackEndRestConnector.notify(NOTIFICATION_DTO, TOKEN)).thenReturn(NOTIFICATION_RESOURCE);

        try (ExecSwap ignored = swapToRealExecutor()) {
            assertDoesNotThrow(() -> notificationManagerService.schedule());
        }

        checkKoNotifications();
    }

    @Test
    void recoverKoNotification_ko_for_whitelist() {
        when(notificationManagerRepository.findKoToRecover(any(LocalDateTime.class)))
                .thenReturn(KO_NOTIFICATION_WHITELIST, KO_NOTIFICATION_WHITELIST, null);
        when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(ioBackEndRestConnector.getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN)))
                .thenReturn(PROFILE_RESOURCE);

        // executor che non esegue nulla ma non fallisce
        Future<Long> ok = CompletableFuture.completedFuture(0L);
        try (ExecSwap ignored = swapToFixedFuture(ok)) {
            assertDoesNotThrow(() -> notificationManagerService.schedule());
        }

        verify(notificationManagerRepository, never()).save(any(Notification.class));
    }

    @Test
    void recovery_notify_ko() {
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(ioBackEndRestConnector.getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN)))
                .thenReturn(PROFILE_RESOURCE);
        when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        when(notificationMarkdown.getSubject(any(Notification.class))).thenReturn(SUBJECT);
        when(notificationMarkdown.getMarkdown(any(Notification.class))).thenReturn(MARKDOWN);
        when(notificationDTOMapper.map(eq(FISCAL_CODE), anyLong(), anyString(), anyString()))
                .thenReturn(NOTIFICATION_DTO);

        Request request = Request.create(Request.HttpMethod.POST, "url", new HashMap<>(), null, new RequestTemplate());
        doThrow(new FeignException.BadRequest("", request, new byte[0], null))
                .when(ioBackEndRestConnector).notify(NOTIFICATION_DTO, TOKEN);

        try {
            notificationManagerService.notify(KO_NOTIFICATION_FIRST_RETRY);
        } catch (FeignException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.status());
        }
        verify(notificationManagerRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void recovery_notify_ko_get_io_tokens() {
        Request request = Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(initiativeRestConnector).getIOTokens(EVALUATION_DTO.getInitiativeId());

        notificationManagerService.notify(KO_NOTIFICATION_FIRST_RETRY);
        verify(notificationManagerRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void recovery_notify_ko_user_not_allowed_feign() {
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Request request = Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(ioBackEndRestConnector)
                .getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN));

        notificationManagerService.notify(KO_NOTIFICATION_FIRST_RETRY);
        verify(notificationManagerRepository, times(0)).save(any(Notification.class));
    }

    @Test
    void recovery_notify_ko_user_not_allowed() {
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        when(ioBackEndRestConnector.getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN)))
                .thenReturn(PROFILE_RESOURCE_KO);

        notificationManagerService.notify(KO_NOTIFICATION_N_RETRY);
        verify(notificationManagerRepository, times(0)).save(any(Notification.class));
    }

    @Test
    void recovery_notify_ko_no_cf() {
        when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Request request = Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(pdvDecryptRestConnector).getPii(TEST_TOKEN);

        notificationManagerService.notify(KO_NOTIFICATION_FIRST_RETRY);
        verify(notificationManagerRepository, times(0)).save(any(Notification.class));
    }

    private void checkKoNotifications() {
        assertEquals(1, KO_NOTIFICATION_FIRST_RETRY.getRetry());
        assertTrue(KO_NOTIFICATION_FIRST_RETRY.getNotificationDate().isAfter(TEST_DATE));

        assertEquals(1, KO_CHECK_IBAN_NOTIFICATION_FIRST_RETRY.getRetry());
        assertTrue(KO_NOTIFICATION_FIRST_RETRY.getNotificationDate().isAfter(TEST_DATE));

        assertEquals(1, KO_REFUND_NOTIFICATION_FIRST_RETRY.getRetry());
        assertTrue(KO_NOTIFICATION_FIRST_RETRY.getNotificationDate().isAfter(TEST_DATE));

        assertEquals(1, KO_SUSPENSION_NOTIFICATION_FIRST_RETRY.getRetry());
        assertTrue(KO_NOTIFICATION_FIRST_RETRY.getNotificationDate().isAfter(TEST_DATE));

        assertEquals(1, KO_READMISSION_NOTIFICATION_FIRST_RETRY.getRetry());
        assertTrue(KO_NOTIFICATION_FIRST_RETRY.getNotificationDate().isAfter(TEST_DATE));

        assertEquals(3, KO_NOTIFICATION_N_RETRY.getRetry());
        assertTrue(KO_NOTIFICATION_N_RETRY.getNotificationDate().isAfter(TEST_DATE));
    }

    @Test
    void sendNotificationFromOperationType_suspension_ok() {
        when(notificationMapper.toEntity(NOTIFICATION_SUSPENSION_QUEUE_DTO)).thenReturn(NOTIFICATION_SUSPENSION);
        when(initiativeRestConnector.getIOTokens(INITIATIVE_ID)).thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(notificationMarkdown.getMarkdownSuspension()).thenReturn(MARKDOWN);
        when(notificationMarkdown.getSubjectSuspension(INITIATIVE_NAME)).thenReturn(SUBJECT);
        when(ioBackEndRestConnector.getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN)))
                .thenReturn(PROFILE_RESOURCE);
        when(notificationDTOMapper.map(eq(FISCAL_CODE), anyLong(), anyString(), anyString()))
                .thenReturn(NOTIFICATION_DTO);
        when(ioBackEndRestConnector.notify(NOTIFICATION_DTO, TOKEN)).thenReturn(NOTIFICATION_RESOURCE);

        assertDoesNotThrow(() -> notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_SUSPENSION_QUEUE_DTO));
    }

    @Test
    void sendNotificationFromOperationType_readmission_ok() {
        when(notificationMapper.toEntity(NOTIFICATION_READMISSION_QUEUE_DTO)).thenReturn(NOTIFICATION_READMISSION);
        when(initiativeRestConnector.getIOTokens(INITIATIVE_ID)).thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(notificationMarkdown.getMarkdownReadmission()).thenReturn(MARKDOWN);
        when(notificationMarkdown.getSubjectReadmission(INITIATIVE_NAME)).thenReturn(SUBJECT);
        when(ioBackEndRestConnector.getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN)))
                .thenReturn(PROFILE_RESOURCE);
        when(notificationDTOMapper.map(eq(FISCAL_CODE), anyLong(), anyString(), anyString()))
                .thenReturn(NOTIFICATION_DTO);
        when(ioBackEndRestConnector.notify(NOTIFICATION_DTO, TOKEN)).thenReturn(NOTIFICATION_RESOURCE);

        assertDoesNotThrow(() -> notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_READMISSION_QUEUE_DTO));
    }

    @ParameterizedTest
    @MethodSource("operationTypeAndInvocationTimes")
    void processCommand(String operationType, int times) {
        CommandOperationQueueDTO queueCommandOperationDTO = CommandOperationQueueDTO.builder()
                .entityId(INITIATIVE_ID)
                .operationType(operationType)
                .operationTime(LocalDateTime.now())
                .build();

        Notification notification = Notification.builder().id("ID_NOTIFICATION").initiativeId(INITIATIVE_ID).build();
        List<Notification> deletedPage = List.of(notification);

        if (times == 2) {
            List<Notification> walletPage = createNotificationPage();
            when(notificationManagerRepositoryExtended.deletePaged(queueCommandOperationDTO.getEntityId(), PAGE_SIZE))
                    .thenReturn(walletPage).thenReturn(deletedPage);
            Thread.currentThread().interrupt();
        } else {
            when(notificationManagerRepositoryExtended.deletePaged(queueCommandOperationDTO.getEntityId(), PAGE_SIZE))
                    .thenReturn(deletedPage);
        }

        notificationManagerService.processNotification(queueCommandOperationDTO);
        verify(notificationManagerRepositoryExtended, times(times))
                .deletePaged(queueCommandOperationDTO.getEntityId(), PAGE_SIZE);
    }

    private static Stream<Arguments> operationTypeAndInvocationTimes() {
        return Stream.of(
                Arguments.of(OPERATION_TYPE_DELETE_INITIATIVE, 1),
                Arguments.of(OPERATION_TYPE_DELETE_INITIATIVE, 2),
                Arguments.of("OPERATION_TYPE_TEST", 0)
        );
    }

    private List<Notification> createNotificationPage() {
        List<Notification> notificationPage = new ArrayList<>();
        for (int i = 0; i < PAGE_SIZE; i++) {
            notificationPage.add(Notification.builder()
                    .id("ID_NOTIFICATION" + i)
                    .initiativeId(INITIATIVE_ID)
                    .build());
        }
        return notificationPage;
    }

    @Test
    void notify_appIo_processReturnsNull_marksKo() {
        EvaluationDTO evaluationDTO = new EvaluationDTO(
                TEST_TOKEN, INITIATIVE_ID, INITIATIVE_ID, TEST_DATE_ONLY_DATE, INITIATIVE_ID, ORGANIZATION_NAME,
                NotificationConstants.STATUS_ONBOARDING_OK, TEST_DATE, TEST_DATE, List.of(),
                50000L, 1L, true, null, IO, null, null, null, null
        );

        Notification notification = Notification.builder()
                .initiativeId(evaluationDTO.getInitiativeId())
                .userId(evaluationDTO.getUserId())
                .build();

        when(notificationMapper.evaluationToNotification(evaluationDTO)).thenReturn(notification);
        when(initiativeRestConnector.getIOTokens(evaluationDTO.getInitiativeId())).thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(ioBackEndRestConnector.getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN)))
                .thenReturn(PROFILE_RESOURCE);
        when(onboardingIoNotification.processNotification(evaluationDTO)).thenReturn(null);

        notificationManagerService.notify(evaluationDTO);

        verify(notificationManagerRepository, times(1)).save(argThat(n ->
                NotificationConstants.NOTIFICATION_STATUS_KO.equals(n.getNotificationStatus())
                        && n.getStatusKoTimestamp() != null
        ));
    }

    @Test
    void notify_withUnknownOperationType_returnsFalse_andNoSave() {
        Notification unknown = Notification.builder()
                .initiativeId(INITIATIVE_ID)
                .userId(TEST_TOKEN)
                .operationType("SOMETHING_ELSE")
                .build();

        when(initiativeRestConnector.getIOTokens(INITIATIVE_ID)).thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(ioBackEndRestConnector.getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN)))
                .thenReturn(PROFILE_RESOURCE);

        boolean result = notificationManagerService.notify(unknown);

        assertFalse(result);
        verify(notificationManagerRepository, never()).save(any());
    }

    @Test
    void sendNotificationFromOperationType_reminder_ok() {
        NotificationReminderQueueDTO dto = NotificationReminderQueueDTO.builder()
                .operationType("REMINDER")
                .userId(TEST_TOKEN)
                .initiativeId(INITIATIVE_ID)
                .build();

        Notification notification = Notification.builder()
                .initiativeId(INITIATIVE_ID)
                .userId(TEST_TOKEN)
                .build();

        when(notificationMapper.toEntity(dto)).thenReturn(notification);
        when(initiativeRestConnector.getIOTokens(INITIATIVE_ID)).thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        when(ioBackEndRestConnector.getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN)))
                .thenReturn(PROFILE_RESOURCE);
        when(notificationMarkdown.getSubjectReminderBel()).thenReturn(SUBJECT);
        when(notificationMarkdown.getMarkdownReminder(dto.getInitiativeId(), dto.getVoucherEndDate())).thenReturn(MARKDOWN);
        when(notificationDTOMapper.map(eq(FISCAL_CODE), anyLong(), anyString(), anyString()))
                .thenReturn(NOTIFICATION_DTO);
        when(ioBackEndRestConnector.notify(NOTIFICATION_DTO, TOKEN)).thenReturn(NOTIFICATION_RESOURCE);

        notificationManagerService.sendNotificationFromOperationType(dto);
        verify(notificationManagerRepository, never()).save(any());
    }

    @Test
    void schedule_invokesRecoverKoNotifications() {
        NotificationManagerServiceImpl spy = Mockito.spy(notificationManagerService);
        doNothing().when(spy).recoverKoNotifications();
        spy.schedule();
        verify(spy, times(1)).recoverKoNotifications();
    }

    @Test
    void recoverKoNotifications_noneFound_logsZero_noSaves() {
        when(notificationManagerRepository.findKoToRecover(any(LocalDateTime.class))).thenReturn(null);
        try (ExecSwap ignored = swapToRealExecutor()) {
            assertDoesNotThrow(() -> notificationManagerService.recoverKoNotifications());
        }
        verify(notificationManagerRepository, never()).save(any());
    }

    @Test
    void recoverKoNotifications_handlesExecutionException() {
        Future<Long> bad = new Future<>() {
            @Override public boolean cancel(boolean mayInterruptIfRunning) { return false; }
            @Override public boolean isCancelled() { return false; }
            @Override public boolean isDone() { return true; }
            @Override public Long get() throws ExecutionException { throw new ExecutionException(new RuntimeException("boom")); }
            @Override public Long get(long timeout, TimeUnit unit) throws ExecutionException { throw new ExecutionException(new RuntimeException("boom")); }
        };

        try (ExecSwap ignored = swapToFixedFuture(bad)) {
            when(notificationManagerRepository.findKoToRecover(any())).thenReturn(null);
            assertDoesNotThrow(() -> notificationManagerService.recoverKoNotifications());
        }
    }

    @Test
    void recoverKoNotifications_handlesInterruptedException_rethrowsIllegalState() {
        Future<Long> interrupted = new Future<>() {
            @Override public boolean cancel(boolean mayInterruptIfRunning) { return false; }
            @Override public boolean isCancelled() { return false; }
            @Override public boolean isDone() { return true; }
            @Override public Long get() throws InterruptedException { throw new InterruptedException("interrupted"); }
            @Override public Long get(long timeout, TimeUnit unit) throws InterruptedException { throw new InterruptedException("interrupted"); }
        };

        try (ExecSwap ignored = swapToFixedFuture(interrupted)) {
            when(notificationManagerRepository.findKoToRecover(any())).thenReturn(null);
            assertThrows(IllegalStateException.class, () -> notificationManagerService.recoverKoNotifications());
        }
    }

    @Test
    void close_shutsDownExecutor() {
        try (ExecSwap ignored = swapToRealExecutor()) {
            notificationManagerService.close();
            ExecutorService exec = (ExecutorService) ReflectionTestUtils
                    .getField(notificationManagerService, "executorService");
            assertTrue(exec.isShutdown(), "ExecutorService should be shut down after close()");
        }
    }

    @Test
    void manualNotify_ok() {
        String subject = "Subject Test!";
        String markdown = "Ciao %name%, test.\n**Dummy** %libTest%";
        MessageContent message = new MessageContent();
        message.setSubject(subject);
        message.setMarkdown(markdown);

        String initiativeId = "INITIATIVE_ID";
        String userId = "USER_ID";

        ManualNotificationDTO request = ManualNotificationDTO.builder().userId(userId)
                .initiativeId(initiativeId)
                .content(message)
                .bodyValues(Map.of("name", "NAME_FAKE", "libTest", "TESTO_LIBERO_FAKE")).build();
        when(initiativeRestConnector.getIOTokens(initiativeId)).thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        when(pdvDecryptRestConnector.getPii(userId)).thenReturn(FISCAL_CODE_RESOURCE);
        when(ioBackEndRestConnector.getProfile(argThat(fc -> FISCAL_CODE.equals(fc.getFiscalCode())), eq(TOKEN)))
                .thenReturn(PROFILE_RESOURCE);
        NotificationResource notificationResource = new NotificationResource();
        notificationResource.setId("ID");

        when(ioBackEndRestConnector.notify(any(), eq(TOKEN))).thenReturn(notificationResource);

        assertDoesNotThrow(() -> notificationManagerService.manualNotify(request));
        verifyNoMoreInteractions(notificationManagerRepository);
    }
}
