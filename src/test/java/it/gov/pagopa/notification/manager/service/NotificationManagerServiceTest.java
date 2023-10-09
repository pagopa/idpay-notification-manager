package it.gov.pagopa.notification.manager.service;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
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
import it.gov.pagopa.notification.manager.utils.AuditUtilities;
import org.junit.jupiter.api.Assertions;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = NotificationManagerServiceImpl.class)
@TestPropertySource(properties = {
        "rest-client.notification.backend-io.ttl=3600",
        "notification.manager.recover.parallelism=7"
})
class NotificationManagerServiceTest {
    private static final String TEST_TOKEN = "TEST_TOKEN";
    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    private static final LocalDateTime TEST_DATE = LocalDateTime.now();
    private static final LocalDate TEST_DATE_ONLY_DATE = LocalDate.now();
    private static final FiscalCodeResource FISCAL_CODE_RESOURCE = new FiscalCodeResource();
    private static final List<String> PREFERRED_LANGUAGES = new ArrayList<>();
    private static final ProfileResource PROFILE_RESOURCE = new ProfileResource(true,
            PREFERRED_LANGUAGES);
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
    private static final String OPERATION_TYPE_DELETE_INITIATIVE = "DELETE_INITIATIVE";
    private static final int PAGE_SIZE = 100;
    private static final EvaluationDTO EVALUATION_DTO =
            new EvaluationDTO(
                    TEST_TOKEN,
                    INITIATIVE_ID,
                    INITIATIVE_ID,
                    TEST_DATE_ONLY_DATE,
                    INITIATIVE_ID,
                    NotificationConstants.STATUS_ONBOARDING_OK,
                    TEST_DATE,
                    TEST_DATE,
                    List.of(),
                    new BigDecimal(500), 1L);
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
                    .build();
    private static final ServiceResource SERVICE_RESOURCE = new ServiceResource();
    private static final InitiativeAdditionalInfoDTO INITIATIVE_ADDITIONAL_INFO_DTO = InitiativeAdditionalInfoDTO.builder()
            .primaryTokenIO(TOKEN)
            .secondaryTokenIO(SECONDARY_KEY)
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
            .build();
    private static final Notification NOTIFICATION_READMISSION = Notification.builder()
            .notificationDate(LocalDateTime.now())
            .notificationStatus(NotificationConstants.NOTIFICATION_STATUS_OK)
            .initiativeName(INITIATIVE_NAME)
            .initiativeId(INITIATIVE_ID)
            .operationType(OPERATION_TYPE)
            .userId(TEST_TOKEN)
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
    @MockBean
    OutcomeProducer outcomeProducer;
    @MockBean
    InitiativeRestConnector initiativeRestConnector;
    @MockBean
    IOBackEndRestConnector ioBackEndRestConnector;
    @MockBean
    NotificationManagerRepository notificationManagerRepository;
    @MockBean
    NotificationManagerRepositoryExtended notificationManagerRepositoryExtended;
    @MockBean
    NotificationDTOMapper notificationDTOMapper;
    @MockBean
    PdvDecryptRestConnector pdvDecryptRestConnector;
    @MockBean
    NotificationMapper notificationMapper;
    @MockBean
    NotificationMarkdown notificationMarkdown;
    @MockBean
    AuditUtilities auditUtilities;

    @Test
    void sendToQueue() {
        Mockito.doNothing().when(outcomeProducer).sendOutcome(EVALUATION_DTO);

        notificationManagerService.addOutcome(EVALUATION_DTO);

        Mockito.verify(outcomeProducer, Mockito.times(1)).sendOutcome(EVALUATION_DTO);
    }

    @Test
    void not_to_notify(){
        EvaluationDTO evaluationDTO =
        new EvaluationDTO(
                TEST_TOKEN,
                INITIATIVE_ID,
                INITIATIVE_ID,
                TEST_DATE_ONLY_DATE,
                INITIATIVE_ID,
                NotificationConstants.STATUS_ONBOARDING_OK,
                TEST_DATE,
                TEST_DATE,
                List.of(new OnboardingRejectionReason(OnboardingRejectionReason.OnboardingRejectionReasonType.FAMILY_CRITERIA_KO,
                        OnboardingRejectionReason.OnboardingRejectionReasonCode.FAMILY_CRITERIA_FAIL, null, null, null),
                        new OnboardingRejectionReason(OnboardingRejectionReason.OnboardingRejectionReasonType.ISEE_TYPE_KO,
                                OnboardingRejectionReason.OnboardingRejectionReasonCode.ISEE_TYPE_FAIL, null, null, null)),
                new BigDecimal(500), 1L);

        try {
            notificationManagerService.notify(evaluationDTO);
        } catch (FeignException e) {
            Assertions.fail();
        }
        Mockito.verify(notificationMapper, Mockito.times(0))
                .evaluationToNotification(evaluationDTO);
        Mockito.verify(initiativeRestConnector, Mockito.times(0))
                .getIOTokens(evaluationDTO.getInitiativeId());
        Mockito.verify(notificationManagerRepository, Mockito.times(0))
                .save(NOTIFICATION);
    }
    @Test
    void notify_ok() {
        Mockito.when(notificationMapper.evaluationToNotification(EVALUATION_DTO))
                .thenReturn(NOTIFICATION);

        Mockito.when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);

        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);

        Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE_DTO, TOKEN)).thenReturn(PROFILE_RESOURCE);

        Mockito.when(notificationMarkdown.getSubject(EVALUATION_DTO)).thenReturn(SUBJECT);

        Mockito.when(notificationMarkdown.getMarkdown(EVALUATION_DTO)).thenReturn(MARKDOWN);

        Mockito.when(
                        notificationDTOMapper.map(
                                Mockito.eq(FISCAL_CODE),
                                Mockito.any(Long.class),
                                Mockito.anyString(),
                                Mockito.anyString()))
                .thenReturn(NOTIFICATION_DTO);

        Mockito.when(ioBackEndRestConnector.notify(NOTIFICATION_DTO, TOKEN))
                .thenReturn(NOTIFICATION_RESOURCE);

        try {
            notificationManagerService.notify(EVALUATION_DTO);
        } catch (FeignException e) {
            Assertions.fail();
        }

        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(NOTIFICATION);
    }

    @Test
    void notify_ko() {
        EvaluationDTO evaluationDTO =
                new EvaluationDTO(
                        TEST_TOKEN,
                        INITIATIVE_ID,
                        INITIATIVE_ID,
                        TEST_DATE_ONLY_DATE,
                        INITIATIVE_ID,
                        NotificationConstants.STATUS_ONBOARDING_OK,
                        TEST_DATE,
                        TEST_DATE,
                        null,
                        new BigDecimal(500), 1L);
        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE_DTO, TOKEN))
                .thenReturn(PROFILE_RESOURCE);

        Mockito.when(initiativeRestConnector.getIOTokens(evaluationDTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Mockito.when(notificationMarkdown.getSubject(evaluationDTO)).thenReturn(SUBJECT);
        Mockito.when(notificationMarkdown.getMarkdown(evaluationDTO)).thenReturn(MARKDOWN);
        Mockito.when(
                        notificationDTOMapper.map(
                                Mockito.eq(FISCAL_CODE),
                                Mockito.any(Long.class),
                                Mockito.anyString(),
                                Mockito.anyString()))
                .thenReturn(NOTIFICATION_DTO);
        Notification notification =
                Notification.builder()
                        .notificationDate(LocalDateTime.now())
                        .initiativeId(evaluationDTO.getInitiativeId())
                        .userId(evaluationDTO.getUserId())
                        .onboardingOutcome(evaluationDTO.getStatus())
                        .rejectReasons(evaluationDTO.getOnboardingRejectionReasons())
                        .build();
        Mockito.when(notificationMapper.evaluationToNotification(evaluationDTO))
                .thenReturn(notification);
        Request request =
                Request.create(
                        Request.HttpMethod.POST, "url", new HashMap<>(), null, new RequestTemplate());
        Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
                .when(ioBackEndRestConnector)
                .notify(NOTIFICATION_DTO, TOKEN);

        try {
            notificationManagerService.notify(evaluationDTO);
        } catch (FeignException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.status());
        }

        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(Mockito.any(Notification.class));
    }

    @Test
    void notify_ko_get_io_tokens() {
        EvaluationDTO evaluationDTO =
                new EvaluationDTO(
                        TEST_TOKEN,
                        INITIATIVE_ID,
                        INITIATIVE_ID,
                        TEST_DATE_ONLY_DATE,
                        INITIATIVE_ID,
                        NotificationConstants.STATUS_ONBOARDING_OK,
                        TEST_DATE,
                        TEST_DATE,
                        List.of(new OnboardingRejectionReason(OnboardingRejectionReason.OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL,
                                        OnboardingRejectionReason.OnboardingRejectionReasonCode.AUTOMATED_CRITERIA_ISEE_FAIL, null, null, null),
                                new OnboardingRejectionReason(OnboardingRejectionReason.OnboardingRejectionReasonType.ISEE_TYPE_KO,
                                        OnboardingRejectionReason.OnboardingRejectionReasonCode.ISEE_TYPE_FAIL, null, null, null)),
                        new BigDecimal(500), 1L);

        Request request =
                Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());

        Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(initiativeRestConnector)
                .getIOTokens(evaluationDTO.getInitiativeId());

        Mockito.when(notificationMapper.evaluationToNotification(evaluationDTO))
                .thenReturn(NOTIFICATION);

        notificationManagerService.notify(evaluationDTO);
        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(Mockito.any(Notification.class));
    }

    @Test
    void notify_ko_user_not_allowed_feign() {
        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);

        Mockito.when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Request request =
                Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(ioBackEndRestConnector)
                .getProfile(FISCAL_CODE_DTO, TOKEN);

        Mockito.when(notificationMapper.evaluationToNotification(EVALUATION_DTO))
                .thenReturn(NOTIFICATION);

        notificationManagerService.notify(EVALUATION_DTO);

        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(Mockito.any(Notification.class));
    }

    @Test
    void notify_ko_user_not_allowed() {
        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);

        Mockito.when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE_DTO, TOKEN))
                .thenReturn(PROFILE_RESOURCE_KO);
        Mockito.when(notificationMapper.evaluationToNotification(EVALUATION_DTO))
                .thenReturn(NOTIFICATION);

        notificationManagerService.notify(EVALUATION_DTO);

        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(Mockito.any(Notification.class));
    }

    @Test
    void notify_ko_no_cf() {
        Mockito.when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);

        Request request =
                Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(pdvDecryptRestConnector)
                .getPii(TEST_TOKEN);

        Mockito.when(notificationMapper.evaluationToNotification(EVALUATION_DTO))
                .thenReturn(NOTIFICATION);

        notificationManagerService.notify(EVALUATION_DTO);

        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(Mockito.any(Notification.class));
    }

    @Test
    void sendNotificationFromOperationType_checkiban_ok() {

        Mockito.when(notificationMapper.toEntity(NOTIFICATION_IBAN_QUEUE_DTO))
                .thenReturn(NOTIFICATION);

        Mockito.when(initiativeRestConnector.getIOTokens(NOTIFICATION_IBAN_QUEUE_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);

        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        Mockito.when(notificationMarkdown.getSubjectCheckIbanKo()).thenReturn(SUBJECT);

        Mockito.when(notificationMarkdown.getMarkdownCheckIbanKo()).thenReturn(MARKDOWN);

        Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE_DTO, TOKEN))
                .thenReturn(PROFILE_RESOURCE);

        Mockito.when(
                        notificationDTOMapper.map(
                                Mockito.eq(FISCAL_CODE),
                                Mockito.any(Long.class),
                                Mockito.anyString(),
                                Mockito.anyString()))
                .thenReturn(NOTIFICATION_DTO);

        Mockito.when(ioBackEndRestConnector.notify(NOTIFICATION_DTO, TOKEN))
                .thenReturn(NOTIFICATION_RESOURCE);
        try {
            notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_IBAN_QUEUE_DTO);
        } catch (FeignException e) {
            Assertions.fail();
        }

        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(NOTIFICATION);
    }

    @Test
    void sendNotificationFromOperationType_not_expected() {

        NotificationQueueDTO notificationQueue =
                new NotificationQueueDTO("","","","");

        try {
            notificationManagerService.sendNotificationFromOperationType(notificationQueue);

        } catch (FeignException e) {
            Assertions.fail();
        }
        Mockito.verify(notificationManagerRepository, Mockito.times(0))
                .save(any());
    }

    @Test
    void sendNotificationFromOperationType_checkiban_ko() {
        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE_DTO, TOKEN))
                .thenReturn(PROFILE_RESOURCE);

        Mockito.when(initiativeRestConnector.getIOTokens(NOTIFICATION_IBAN_QUEUE_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Mockito.when(notificationMarkdown.getSubjectCheckIbanKo()).thenReturn(SUBJECT);
        Mockito.when(notificationMarkdown.getMarkdownCheckIbanKo()).thenReturn(MARKDOWN);
        Mockito.when(
                        notificationDTOMapper.map(
                                Mockito.eq(FISCAL_CODE),
                                Mockito.any(Long.class),
                                Mockito.anyString(),
                                Mockito.anyString()))
                .thenReturn(NOTIFICATION_DTO);

        Mockito.when(notificationMapper.toEntity(NOTIFICATION_IBAN_QUEUE_DTO))
                .thenReturn(NOTIFICATION);
        Request request =
                Request.create(
                        Request.HttpMethod.POST, "url", new HashMap<>(), null, new RequestTemplate());
        Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
                .when(ioBackEndRestConnector)
                .notify(NOTIFICATION_DTO, TOKEN);

        try {
            notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_IBAN_QUEUE_DTO);
        } catch (FeignException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.status());
        }

        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(Mockito.any(Notification.class));
    }

    @Test
    void sendNotificationFromOperationType_allowed_citzen_ok() {

        Mockito.when(notificationMapper.toEntity(NOTIFICATION_CITIZEN_ON_QUEUE_DTO))
                .thenReturn(NOTIFICATION);

        Mockito.when(initiativeRestConnector.getIOTokens(INITIATIVE_ID))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);

        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        Mockito.when(notificationMarkdown.getMarkdownInitiativePublishing()).thenReturn(SUBJECT);

        Mockito.when(notificationMarkdown.getSubjectInitiativePublishing()).thenReturn(MARKDOWN);

        Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE_DTO, TOKEN))
                .thenReturn(PROFILE_RESOURCE);

        Mockito.when(
                        notificationDTOMapper.map(
                                Mockito.eq(FISCAL_CODE),
                                Mockito.any(Long.class),
                                Mockito.anyString(),
                                Mockito.anyString()))
                .thenReturn(NOTIFICATION_DTO);

        Mockito.when(ioBackEndRestConnector.notify(NOTIFICATION_DTO, TOKEN))
                .thenReturn(NOTIFICATION_RESOURCE);
        try {
            notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_CITIZEN_ON_QUEUE_DTO);
        } catch (FeignException e) {
            Assertions.fail();
        }

        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(NOTIFICATION);
    }

    @Test
    void checkIbanKo_ko_get_io_tokens() {
        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);

        Request request =
                Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());

        Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(initiativeRestConnector)
                .getIOTokens(NOTIFICATION_IBAN_QUEUE_DTO.getInitiativeId());

        Mockito.when(notificationMapper.toEntity(NOTIFICATION_IBAN_QUEUE_DTO))
                .thenReturn(NOTIFICATION);

        notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_IBAN_QUEUE_DTO);
        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(Mockito.any(Notification.class));
    }

    @Test
    void citizen_ko_get_io_tokens() {
        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);

        Request request =
                Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());

        Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(initiativeRestConnector)
                .getIOTokens(NOTIFICATION_IBAN_QUEUE_DTO.getInitiativeId());

        Mockito.when(notificationMapper.toEntity(NOTIFICATION_CITIZEN_ON_QUEUE_DTO))
                .thenReturn(NOTIFICATION);

        notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_CITIZEN_ON_QUEUE_DTO);
        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(Mockito.any(Notification.class));
    }

    @Test
    void checkIbanKo_ko_user_not_allowed_feign() {
        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);

        Mockito.when(initiativeRestConnector.getIOTokens(NOTIFICATION_IBAN_QUEUE_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Request request =
                Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(ioBackEndRestConnector)
                .getProfile(FISCAL_CODE_DTO, TOKEN);

        Mockito.when(notificationMapper.toEntity(NOTIFICATION_IBAN_QUEUE_DTO))
                .thenReturn(NOTIFICATION);

        notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_IBAN_QUEUE_DTO);

        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(Mockito.any(Notification.class));
    }

    @Test
    void citizen_ko_user_not_allowed_feign() {
        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);

        Mockito.when(initiativeRestConnector.getIOTokens(INITIATIVE_ID))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Request request =
                Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(ioBackEndRestConnector)
                .getProfile(FISCAL_CODE_DTO, TOKEN);

        Mockito.when(notificationMapper.toEntity(NOTIFICATION_CITIZEN_ON_QUEUE_DTO))
                .thenReturn(NOTIFICATION);

        notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_CITIZEN_ON_QUEUE_DTO);

        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(Mockito.any(Notification.class));
    }

    @Test
    void checkIbanKo_ko_user_not_allowed() {
        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);

        Mockito.when(initiativeRestConnector.getIOTokens(NOTIFICATION_IBAN_QUEUE_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE_DTO, TOKEN))
                .thenReturn(PROFILE_RESOURCE_KO);

        Mockito.when(notificationMapper.toEntity(NOTIFICATION_IBAN_QUEUE_DTO))
                .thenReturn(NOTIFICATION);

        notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_IBAN_QUEUE_DTO);

        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(Mockito.any(Notification.class));
    }

    @Test
    void citizen_ko_user_not_allowed() {
        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);

        Mockito.when(initiativeRestConnector.getIOTokens(INITIATIVE_ID))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE_DTO, TOKEN))
                .thenReturn(PROFILE_RESOURCE_KO);

        Mockito.when(notificationMapper.toEntity(NOTIFICATION_CITIZEN_ON_QUEUE_DTO))
                .thenReturn(NOTIFICATION);

        notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_CITIZEN_ON_QUEUE_DTO);

        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(Mockito.any(Notification.class));
    }

    @Test
    void checkIbanKo_ko_no_cf() {
        Mockito.when(initiativeRestConnector.getIOTokens(NOTIFICATION_IBAN_QUEUE_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Request request =
                Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(pdvDecryptRestConnector)
                .getPii(TEST_TOKEN);

        Mockito.when(notificationMapper.toEntity(NOTIFICATION_IBAN_QUEUE_DTO))
                .thenReturn(NOTIFICATION);

        notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_IBAN_QUEUE_DTO);

        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(Mockito.any(Notification.class));
    }

    @Test
    void citizen_ko_no_cf() {
        Mockito.when(initiativeRestConnector.getIOTokens(INITIATIVE_ID))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Request request =
                Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(pdvDecryptRestConnector)
                .getPii(TEST_TOKEN);

        Mockito.when(notificationMapper.toEntity(NOTIFICATION_CITIZEN_ON_QUEUE_DTO))
                .thenReturn(NOTIFICATION);

        notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_CITIZEN_ON_QUEUE_DTO);

        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(Mockito.any(Notification.class));
    }

    @Test
    void sendNotificationFromOperationType_notification_notNull_ioTokens_notNull() {
        Mockito.when(notificationMapper.toEntity(NOTIFICATION_CITIZEN_ON_QUEUE_DTO))
                .thenReturn(NOTIFICATION);

        Mockito.when(initiativeRestConnector.getIOTokens(INITIATIVE_ID))
                .thenReturn(null);

        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        Mockito.when(notificationMarkdown.getMarkdownInitiativePublishing()).thenReturn(SUBJECT);

        Mockito.when(notificationMarkdown.getSubjectInitiativePublishing()).thenReturn(MARKDOWN);
        try {
            notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_CITIZEN_ON_QUEUE_DTO);
        } catch (FeignException e) {
            Assertions.fail();
        }

        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(NOTIFICATION);
    }

    @Test
    void sendNotificationFromOperationType_checkiban_notification_Null_ioTokens_null() {
        Mockito.when(notificationMapper.toEntity(NOTIFICATION_IBAN_QUEUE_DTO))
                .thenReturn(null);

        Mockito.when(initiativeRestConnector.getIOTokens(INITIATIVE_ID))
                .thenReturn(null);

        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        Mockito.when(notificationMarkdown.getMarkdownInitiativePublishing()).thenReturn(SUBJECT);

        Mockito.when(notificationMarkdown.getSubjectInitiativePublishing()).thenReturn(MARKDOWN);
        try {
            notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_IBAN_QUEUE_DTO);
        } catch (FeignException e) {
            Assertions.fail();
        }
    }

    @Test
    void sendNotificationFromOperationType_refund_ok() {

        Mockito.when(notificationMapper.toEntity(NOTIFICATION_REFUND_QUEUE_DTO))
                .thenReturn(NOTIFICATION);

        Mockito.when(initiativeRestConnector.getIOTokens(NOTIFICATION_REFUND_QUEUE_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);

        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        Mockito.when(notificationMarkdown.getSubjectRefund(NOTIFICATION_REFUND_QUEUE_DTO.getStatus())).thenReturn(SUBJECT);

        Mockito.when(notificationMarkdown.getMarkdownRefund(Mockito.eq(NOTIFICATION_REFUND_QUEUE_DTO.getStatus()), Mockito.any())).thenReturn(MARKDOWN);

        Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE_DTO, TOKEN))
                .thenReturn(PROFILE_RESOURCE);

        Mockito.when(
                        notificationDTOMapper.map(
                                Mockito.eq(FISCAL_CODE),
                                Mockito.any(Long.class),
                                Mockito.anyString(),
                                Mockito.anyString()))
                .thenReturn(NOTIFICATION_DTO);

        Mockito.when(ioBackEndRestConnector.notify(NOTIFICATION_DTO, TOKEN))
                .thenReturn(NOTIFICATION_RESOURCE);
        try {
            notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_REFUND_QUEUE_DTO);
        } catch (FeignException e) {
            Assertions.fail();
        }

        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(NOTIFICATION);
    }


    //region recovery
    @Test
    void recoverKoNotifications() {

        Mockito.when(notificationManagerRepository.findKoToRecover(Mockito.any(LocalDateTime.class))).thenReturn(KO_NOTIFICATION_FIRST_RETRY, KO_NOTIFICATION_N_RETRY, null);

        Mockito.when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);

        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);

        Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE_DTO, TOKEN)).thenReturn(PROFILE_RESOURCE);

        Mockito.when(notificationMarkdown.getSubject(Mockito.any(Notification.class))).thenReturn(SUBJECT);

        Mockito.when(notificationMarkdown.getMarkdown(Mockito.any(Notification.class))).thenReturn(MARKDOWN);

        Mockito.when(
                        notificationDTOMapper.map(
                                Mockito.eq(FISCAL_CODE),
                                Mockito.any(Long.class),
                                Mockito.anyString(),
                                Mockito.anyString()))
                .thenReturn(NOTIFICATION_DTO);

        Mockito.when(ioBackEndRestConnector.notify(NOTIFICATION_DTO, TOKEN))
                .thenReturn(NOTIFICATION_RESOURCE);

        try {
            notificationManagerService.schedule();
            checkKoNotifications();
        } catch (FeignException e) {
            Assertions.fail();
        }

        Mockito.verify(notificationManagerRepository, Mockito.times(2))
                .save(Mockito.any(Notification.class));
    }

    @Test
    void recovery_notify_ko() {
        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE_DTO, TOKEN))
                .thenReturn(PROFILE_RESOURCE);

        Mockito.when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Mockito.when(notificationMarkdown.getSubject(Mockito.any(Notification.class))).thenReturn(SUBJECT);
        Mockito.when(notificationMarkdown.getMarkdown(Mockito.any(Notification.class))).thenReturn(MARKDOWN);
        Mockito.when(
                        notificationDTOMapper.map(
                                Mockito.eq(FISCAL_CODE),
                                Mockito.any(Long.class),
                                Mockito.anyString(),
                                Mockito.anyString()))
                .thenReturn(NOTIFICATION_DTO);
        Request request =
                Request.create(
                        Request.HttpMethod.POST, "url", new HashMap<>(), null, new RequestTemplate());
        Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
                .when(ioBackEndRestConnector)
                .notify(NOTIFICATION_DTO, TOKEN);

        try {
            notificationManagerService.notify(KO_NOTIFICATION_FIRST_RETRY);
        } catch (FeignException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.status());
        }

        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(Mockito.any(Notification.class));
    }

    @Test
    void recovery_notify_ko_get_io_tokens() {

        Request request =
                Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());

        Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(initiativeRestConnector)
                .getIOTokens(EVALUATION_DTO.getInitiativeId());

        notificationManagerService.notify(KO_NOTIFICATION_FIRST_RETRY);
        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(Mockito.any(Notification.class));
    }

    @Test
    void recovery_notify_ko_user_not_allowed_feign() {
        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);

        Mockito.when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Request request =
                Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(ioBackEndRestConnector)
                .getProfile(FISCAL_CODE_DTO, TOKEN);

        notificationManagerService.notify(KO_NOTIFICATION_FIRST_RETRY);

        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(Mockito.any(Notification.class));
    }

    @Test
    void recovery_notify_ko_user_not_allowed() {
        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);

        Mockito.when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE_DTO, TOKEN))
                .thenReturn(PROFILE_RESOURCE_KO);

        notificationManagerService.notify(KO_NOTIFICATION_N_RETRY);

        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(Mockito.any(Notification.class));
    }

    @Test
    void recovery_notify_ko_no_cf() {
        Mockito.when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId()))
                .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);

        Request request =
                Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
                .when(pdvDecryptRestConnector)
                .getPii(TEST_TOKEN);

        notificationManagerService.notify(KO_NOTIFICATION_FIRST_RETRY);

        Mockito.verify(notificationManagerRepository, Mockito.times(1))
                .save(Mockito.any(Notification.class));
    }

    private void checkKoNotifications() {
        Assertions.assertEquals(1, KO_NOTIFICATION_FIRST_RETRY.getRetry());
        Assertions.assertTrue(KO_NOTIFICATION_FIRST_RETRY.getNotificationDate().isAfter(TEST_DATE));

        Assertions.assertEquals(3, KO_NOTIFICATION_N_RETRY.getRetry());
        Assertions.assertTrue(KO_NOTIFICATION_N_RETRY.getNotificationDate().isAfter(TEST_DATE));
    }
    //endregion
    @Test
    void sendNotificationFromOperationType_suspension_ok() {
        Mockito.when(notificationMapper.toEntity(NOTIFICATION_SUSPENSION_QUEUE_DTO)).thenReturn(NOTIFICATION_SUSPENSION);
        Mockito.when(initiativeRestConnector.getIOTokens(INITIATIVE_ID)).thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        Mockito.when(notificationMarkdown.getMarkdownSuspension()).thenReturn(MARKDOWN);
        Mockito.when(notificationMarkdown.getSubjectSuspension(INITIATIVE_NAME)).thenReturn(SUBJECT);
        Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE_DTO, TOKEN)).thenReturn(PROFILE_RESOURCE);
        Mockito.when(
                        notificationDTOMapper.map(
                                Mockito.eq(FISCAL_CODE),
                                Mockito.any(Long.class),
                                Mockito.anyString(),
                                Mockito.anyString()))
                .thenReturn(NOTIFICATION_DTO);
        Mockito.when(ioBackEndRestConnector.notify(NOTIFICATION_DTO, TOKEN)).thenReturn(NOTIFICATION_RESOURCE);

        try {
            notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_SUSPENSION_QUEUE_DTO);
        } catch (FeignException e) {
            Assertions.fail();
        }

        Mockito.verify(notificationManagerRepository, Mockito.times(1)).save(NOTIFICATION_SUSPENSION);
    }

    @Test
    void sendNotificationFromOperationType_readmission_ok() {
        Mockito.when(notificationMapper.toEntity(NOTIFICATION_READMISSION_QUEUE_DTO)).thenReturn(NOTIFICATION_READMISSION);
        Mockito.when(initiativeRestConnector.getIOTokens(INITIATIVE_ID)).thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
        Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
        Mockito.when(notificationMarkdown.getMarkdownReadmission()).thenReturn(MARKDOWN);
        Mockito.when(notificationMarkdown.getSubjectReadmission(INITIATIVE_NAME)).thenReturn(SUBJECT);
        Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE_DTO, TOKEN)).thenReturn(PROFILE_RESOURCE);
        Mockito.when(
                        notificationDTOMapper.map(
                                Mockito.eq(FISCAL_CODE),
                                Mockito.any(Long.class),
                                Mockito.anyString(),
                                Mockito.anyString()))
                .thenReturn(NOTIFICATION_DTO);
        Mockito.when(ioBackEndRestConnector.notify(NOTIFICATION_DTO, TOKEN)).thenReturn(NOTIFICATION_RESOURCE);

        try {
            notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_READMISSION_QUEUE_DTO);
        } catch (FeignException e) {
            Assertions.fail();
        }

        Mockito.verify(notificationManagerRepository, Mockito.times(1)).save(NOTIFICATION_READMISSION);
    }

    @ParameterizedTest
    @MethodSource("operationTypeAndInvocationTimes")
    void processCommand(String operationType, int times) {

        CommandOperationQueueDTO queueCommandOperationDTO = CommandOperationQueueDTO.builder()
                .entityId(INITIATIVE_ID)
                .operationType(operationType)
                .operationTime(LocalDateTime.now())
                .build();

        Notification notification = Notification.builder()
                .id("ID_NOTIFICATION")
                .initiativeId(INITIATIVE_ID)
                .build();
        List<Notification> deletedPage = List.of(notification);

        if (times == 2) {
            List<Notification> walletPage = createNotificationPage();
            when(notificationManagerRepositoryExtended.deletePaged(queueCommandOperationDTO.getEntityId(), PAGE_SIZE))
                    .thenReturn(walletPage)
                    .thenReturn(deletedPage);

            Thread.currentThread().interrupt();

        } else {
            when(notificationManagerRepositoryExtended.deletePaged(queueCommandOperationDTO.getEntityId(), PAGE_SIZE))
                    .thenReturn(deletedPage);
        }

        notificationManagerService.processNotification(queueCommandOperationDTO);


        // Then
        Mockito.verify(notificationManagerRepositoryExtended, Mockito.times(times)).deletePaged(queueCommandOperationDTO.getEntityId(), PAGE_SIZE);
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

        for (int i = 0; i < NotificationManagerServiceTest.PAGE_SIZE; i++) {
            notificationPage.add(Notification.builder()
                    .id("ID_NOTIFICATION" + i)
                    .initiativeId(INITIATIVE_ID)
                    .build());
        }

        return notificationPage;
    }
}
