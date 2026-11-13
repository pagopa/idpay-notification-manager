package it.gov.pagopa.notification.manager.service.onboarding;

import it.gov.pagopa.notification.manager.config.ProfilesNotificationProperties;
import it.gov.pagopa.notification.manager.connector.EmailNotificationConnector;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EmailMessageDTO;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import it.gov.pagopa.notification.manager.dto.VerifyDTO;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeNotificationDTO;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationMapper;
import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.Notification;
import it.gov.pagopa.notification.manager.model.EmailNotificationContent;
import it.gov.pagopa.notification.manager.repository.NotificationManagerRepository;
import it.gov.pagopa.notification.manager.service.io.EmailNotificationContentService;
import it.gov.pagopa.notification.manager.service.io.ProfilesNotificationCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode.ISEE_TYPE_FAIL;
import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode.AUTOMATED_CRITERIA_ISEE_FAIL;
import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode.FAMILY_BONUS_ALREADY_USED;
import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode.REJECTION_REASON_INITIATIVE_ENDED;
import static it.gov.pagopa.notification.manager.enums.Channel.WEB;
import static it.gov.pagopa.notification.manager.enums.NotificationEvent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingWebNotificationTest {
    private static final String TEST_TOKEN = "TEST_TOKEN";
    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    private static final LocalDateTime TEST_DATE = LocalDateTime.now();
    private static final LocalDate TEST_DATE_ONLY_DATE = LocalDate.now();
    private static final String ORGANIZATION_NAME = "ORGANIZATION_NAME";

    private static final String TEMPLATE_NAME = "TEMPLATE_NAME";
    private static final Map<String, String> TEMPLATE_VALUES = new HashMap<String, String>();
    private static final String SUBJECT = "SUBJECT";
    private static final String CONTENT = "CONTENT";
    private static final String SENDER_EMAIL = "SENDER_EMAIL";
    private static final String RECIPIENT_EMAIL = "RECIPIENT_EMAIL";

    private static final Notification NOTIFICATION = Notification.builder().build();

    private static final EmailMessageDTO EMAIL_MESSAGE_DTO = EmailMessageDTO.builder()
            .templateName(TEMPLATE_NAME)
            .templateValues(TEMPLATE_VALUES)
            .subject(SUBJECT)
            .content(CONTENT)
            .senderEmail(SENDER_EMAIL)
            .recipientEmail(RECIPIENT_EMAIL)
            .build();


    @Mock
    private EmailNotificationConnector emailNotificationConnectorMock;

    @Mock
    private EmailNotificationContentService emailNotificationContentService;

    private OnboardingWebNotification onboardingWebNotification;

    @Mock
    NotificationManagerRepository notificationManagerRepository;

    @Mock
    NotificationMapper notificationMapper;

    @Mock
    InitiativeRestConnector initiativeRestConnector;

    @Mock
    private ProfilesNotificationCatalog profilesNotificationCatalog;

    @BeforeEach
    void setUp() {
        onboardingWebNotification = new OnboardingWebNotificationImpl(emailNotificationConnectorMock,
                notificationManagerRepository,
                notificationMapper,
                initiativeRestConnector,
                emailNotificationContentService,
                profilesNotificationCatalog);
    }

    private void mockContent(String profile, NotificationEvent event,
                             String subject, String templateName) {
        when(emailNotificationContentService.resolveProfileAndFindWithDefaultFallback(profile, event))
                .thenReturn(Optional.of(new EmailNotificationContent(subject, templateName)));
    }

    private EvaluationDTO getEvaluationDto(){
        return new EvaluationDTO(
                TEST_TOKEN,
                INITIATIVE_ID,
                INITIATIVE_ID,
                TEST_DATE_ONLY_DATE,
                INITIATIVE_ID,
                ORGANIZATION_NAME,
                NotificationConstants.STATUS_ONBOARDING_OK,
                TEST_DATE,
                TEST_DATE,
                List.of(),
                20000L,
                1L,
                null,
                "user@email.com",
                WEB,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }





    //region ONBOARDING_OK
    @Test
    void processNotification_whenTemplateEsitoOk200() {
        EvaluationDTO evaluationDTO = getEvaluationDto();

        mockContent("DEC26", ONBOARDING_OK, "TEST_OK", "Email_DEC26/EsitoOk");

        Mockito.doAnswer(invocation -> null)
                .when(emailNotificationConnectorMock)
                .sendEmail(Mockito.any(EmailMessageDTO.class));

        InitiativeNotificationDTO mockInitiative = new InitiativeNotificationDTO();
        mockInitiative.setEmailFlux("DEC26");
        when(initiativeRestConnector.getInitiativeDetailInfo(anyString())).thenReturn(mockInitiative);


        onboardingWebNotification.processNotification(evaluationDTO);

        verify(emailNotificationConnectorMock, times(1))
                .sendEmail(Mockito.argThat(email ->
                        email.getTemplateValues().containsKey("amount") &&
                                email.getTemplateValues().get("amount").equals("200")
                ));
    }

    @Test
    void processNotification_whenTemplateEsitoOkNoVerifyIsee100() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        evaluationDTO.setBeneficiaryBudgetCents(10000L);

        mockContent("DEC26", ONBOARDING_OK, "TEST_OK", "Email_DEC26/EsitoOk");

        Mockito.doAnswer(invocation -> null)
                .when(emailNotificationConnectorMock)
                .sendEmail(Mockito.any(EmailMessageDTO.class));

        InitiativeNotificationDTO mockInitiative = new InitiativeNotificationDTO();
        mockInitiative.setEmailFlux("DEC26");
        when(initiativeRestConnector.getInitiativeDetailInfo(anyString())).thenReturn(mockInitiative);

        onboardingWebNotification.processNotification(evaluationDTO);

        verify(emailNotificationConnectorMock, times(1))
                .sendEmail(Mockito.argThat(email ->
                        "Email_DEC26/EsitoOk".equals(email.getTemplateName())
                ));
    }

    @Test
    void processNotification_withNullBeneficiaryBudget() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        evaluationDTO.setBeneficiaryBudgetCents(null);

        mockContent("DEC26", ONBOARDING_OK, "TEST_OK", "Email_DEC26/EsitoOk");

        Mockito.doAnswer(invocation -> null)
                .when(emailNotificationConnectorMock)
                .sendEmail(Mockito.any(EmailMessageDTO.class));

        InitiativeNotificationDTO mockInitiative = new InitiativeNotificationDTO();
        mockInitiative.setEmailFlux("DEC26");
        when(initiativeRestConnector.getInitiativeDetailInfo(anyString())).thenReturn(mockInitiative);


        onboardingWebNotification.processNotification(evaluationDTO);

        verify(emailNotificationConnectorMock, times(1))
                .sendEmail(Mockito.argThat(email ->
                        !email.getTemplateValues().containsKey("amount")
                ));
    }

    @Test
    void processNotification_templateEsitoOkParziale() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        VerifyDTO verifyDTO = VerifyDTO.builder()
                .verify(true)
                .beneficiaryBudgetCentsMin(10000L)
                .build();
        List<VerifyDTO> verifies = List.of(verifyDTO);
        evaluationDTO.setVerifies(verifies);
        evaluationDTO.setBeneficiaryBudgetCents(10000L);

        mockContent("DEC26", ONBOARDING_OK_PARTIAL, "TEST_PARTIAL_OK", "Email_DEC26/EsitoParziale");

        Mockito.doAnswer(invocation -> null)
                .when(emailNotificationConnectorMock)
                .sendEmail(Mockito.any(EmailMessageDTO.class));

        InitiativeNotificationDTO mockInitiative = new InitiativeNotificationDTO();
        mockInitiative.setEmailFlux("DEC26");
        when(initiativeRestConnector.getInitiativeDetailInfo(anyString())).thenReturn(mockInitiative);

        onboardingWebNotification.processNotification(evaluationDTO);

        verify(emailNotificationConnectorMock, times(1))
                .sendEmail(Mockito.argThat(email ->
                        "Email_DEC26/EsitoParziale".equals(email.getTemplateName())
                ));
    }

    @Test
    void processNotification_whenNoIseeVerify_shouldBuildEsitoOk() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        // User opted NOT to use ISEE: verification is not performed even if the option's
        // min budget matches the awarded budget. Must resolve to EsitoOk, not EsitoParziale.
        VerifyDTO verifyDTO = VerifyDTO.builder()
                .verify(false)
                .beneficiaryBudgetCentsMin(10000L)
                .build();
        evaluationDTO.setVerifies(List.of(verifyDTO));
        evaluationDTO.setBeneficiaryBudgetCents(10000L);

        mockContent("DEC26", ONBOARDING_OK, "TEST_OK", "Email_DEC26/EsitoOk");

        Mockito.doAnswer(invocation -> null)
                .when(emailNotificationConnectorMock)
                .sendEmail(Mockito.any(EmailMessageDTO.class));

        InitiativeNotificationDTO mockInitiative = new InitiativeNotificationDTO();
        mockInitiative.setEmailFlux("DEC26");
        when(initiativeRestConnector.getInitiativeDetailInfo(anyString())).thenReturn(mockInitiative);

        onboardingWebNotification.processNotification(evaluationDTO);

        verify(emailNotificationConnectorMock, times(1))
                .sendEmail(Mockito.argThat(email ->
                        "Email_DEC26/EsitoOk".equals(email.getTemplateName())
                ));
    }

    @Test
    void processNotification_whenIseeVerifiedFullBudget_shouldBuildEsitoOk() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        // ISEE verified and conform: awarded budget equals the option MAX (200€),
        // so min != budget and the outcome must be full EsitoOk.
        VerifyDTO verifyDTO = VerifyDTO.builder()
                .verify(true)
                .beneficiaryBudgetCentsMin(10000L)
                .beneficiaryBudgetCentsMax(20000L)
                .build();
        evaluationDTO.setVerifies(List.of(verifyDTO));
        evaluationDTO.setBeneficiaryBudgetCents(20000L);

        mockContent("DEC26", ONBOARDING_OK, "TEST_OK", "Email_DEC26/EsitoOk");

        Mockito.doAnswer(invocation -> null)
                .when(emailNotificationConnectorMock)
                .sendEmail(Mockito.any(EmailMessageDTO.class));

        InitiativeNotificationDTO mockInitiative = new InitiativeNotificationDTO();
        mockInitiative.setEmailFlux("DEC26");
        when(initiativeRestConnector.getInitiativeDetailInfo(anyString())).thenReturn(mockInitiative);

        onboardingWebNotification.processNotification(evaluationDTO);

        verify(emailNotificationConnectorMock, times(1))
                .sendEmail(Mockito.argThat(email ->
                        "Email_DEC26/EsitoOk".equals(email.getTemplateName())
                ));
    }

    @Test
    void processNotification_whenVerifyTrueButBudgetNull_shouldBuildEsitoOk() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        // verify=true but budget is null: the null-check short-circuits, so it must not be partial.
        VerifyDTO verifyDTO = VerifyDTO.builder()
                .verify(true)
                .beneficiaryBudgetCentsMin(10000L)
                .build();
        evaluationDTO.setVerifies(List.of(verifyDTO));
        evaluationDTO.setBeneficiaryBudgetCents(null);

        mockContent("DEC26", ONBOARDING_OK, "TEST_OK", "Email_DEC26/EsitoOk");

        Mockito.doAnswer(invocation -> null)
                .when(emailNotificationConnectorMock)
                .sendEmail(Mockito.any(EmailMessageDTO.class));

        InitiativeNotificationDTO mockInitiative = new InitiativeNotificationDTO();
        mockInitiative.setEmailFlux("DEC26");
        when(initiativeRestConnector.getInitiativeDetailInfo(anyString())).thenReturn(mockInitiative);

        onboardingWebNotification.processNotification(evaluationDTO);

        verify(emailNotificationConnectorMock, times(1))
                .sendEmail(Mockito.argThat(email ->
                        "Email_DEC26/EsitoOk".equals(email.getTemplateName())
                                && !email.getTemplateValues().containsKey("amount")
                ));
    }

    @Test
    void processNotification_whenVerifiesEmptyList_shouldBuildEsitoOk() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        // Non-null but empty verifies list: the loop is skipped, outcome must be full EsitoOk.
        evaluationDTO.setVerifies(List.of());
        evaluationDTO.setBeneficiaryBudgetCents(10000L);

        mockContent("DEC26", ONBOARDING_OK, "TEST_OK", "Email_DEC26/EsitoOk");

        Mockito.doAnswer(invocation -> null)
                .when(emailNotificationConnectorMock)
                .sendEmail(Mockito.any(EmailMessageDTO.class));

        InitiativeNotificationDTO mockInitiative = new InitiativeNotificationDTO();
        mockInitiative.setEmailFlux("DEC26");
        when(initiativeRestConnector.getInitiativeDetailInfo(anyString())).thenReturn(mockInitiative);

        onboardingWebNotification.processNotification(evaluationDTO);

        verify(emailNotificationConnectorMock, times(1))
                .sendEmail(Mockito.argThat(email ->
                        "Email_DEC26/EsitoOk".equals(email.getTemplateName())
                ));
    }

    @Test
    void processNotification_whenStatusKo_shouldSendGenericErrorEmail() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        // Exercise the STATUS_ONBOARDING_KO branch of the processNotification switch.
        evaluationDTO.setStatus(NotificationConstants.STATUS_ONBOARDING_KO);
        evaluationDTO.setOnboardingRejectionReasons(List.of());

        mockContent("DEC26", ONBOARDING_KO_GENERIC, "SUBJ_KO_GENERIC", "Email_DEC26/GenericError");

        Mockito.doAnswer(invocation -> null)
                .when(emailNotificationConnectorMock)
                .sendEmail(Mockito.any(EmailMessageDTO.class));

        InitiativeNotificationDTO mockInitiative = new InitiativeNotificationDTO();
        mockInitiative.setEmailFlux("DEC26");
        when(initiativeRestConnector.getInitiativeDetailInfo(anyString())).thenReturn(mockInitiative);

        onboardingWebNotification.processNotification(evaluationDTO);

        verify(emailNotificationConnectorMock, times(1))
                .sendEmail(Mockito.argThat(email ->
                        "Email_DEC26/GenericError".equals(email.getTemplateName())
                ));
    }

    @Test
    void processNotification_whenMultipleVerifies_firstNonMatching_shouldBuildEsitoParziale() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        // The first verify does not match (min != budget); the loop must continue and
        // detect the second matching verify (verify=true, min == budget) -> partial.
        VerifyDTO nonMatching = VerifyDTO.builder()
                .verify(true)
                .beneficiaryBudgetCentsMin(99999L)
                .build();
        VerifyDTO matching = VerifyDTO.builder()
                .verify(true)
                .beneficiaryBudgetCentsMin(10000L)
                .build();
        evaluationDTO.setVerifies(List.of(nonMatching, matching));
        evaluationDTO.setBeneficiaryBudgetCents(10000L);

        mockContent("DEC26", ONBOARDING_OK_PARTIAL, "TEST_PARTIAL_OK", "Email_DEC26/EsitoParziale");

        Mockito.doAnswer(invocation -> null)
                .when(emailNotificationConnectorMock)
                .sendEmail(Mockito.any(EmailMessageDTO.class));

        InitiativeNotificationDTO mockInitiative = new InitiativeNotificationDTO();
        mockInitiative.setEmailFlux("DEC26");
        when(initiativeRestConnector.getInitiativeDetailInfo(anyString())).thenReturn(mockInitiative);

        onboardingWebNotification.processNotification(evaluationDTO);

        verify(emailNotificationConnectorMock, times(1))
                .sendEmail(Mockito.argThat(email ->
                        "Email_DEC26/EsitoParziale".equals(email.getTemplateName())
                ));
    }

    @Test
    void processNotification_callNotificationEmail_Error() {
        EvaluationDTO evaluationDTO = getEvaluationDto();

        mockContent("DEC26", ONBOARDING_OK, "TEST_OK", "Email_DEC26/EsitoOk");

        doThrow(new RuntimeException("Boom"))
                .when(emailNotificationConnectorMock)
                .sendEmail(Mockito.any(EmailMessageDTO.class));

        when(notificationMapper.createNotificationFromEmailMessageDTO(any(EmailMessageDTO.class),
                any(EvaluationDTO.class))).thenReturn(NOTIFICATION);

        InitiativeNotificationDTO mockInitiative = new InitiativeNotificationDTO();
        mockInitiative.setEmailFlux("DEC26");
        when(initiativeRestConnector.getInitiativeDetailInfo(anyString())).thenReturn(mockInitiative);


        onboardingWebNotification.processNotification(evaluationDTO);

        verify(emailNotificationConnectorMock, times(1))
                .sendEmail(Mockito.any(EmailMessageDTO.class));
    }

    @Test
    void processOnboardingJoined_shouldBuildFamilyUnitEmailDto() {
        EvaluationDTO evaluationDTO = getEvaluationDto();

        mockContent("DEC26", ONBOARDING_JOINED, "SUBJ_FAMILY_UNIT", "Email_DEC26/FamilyUnit");
        evaluationDTO.setEmailFlux("DEC26");

        EmailMessageDTO dto =
                ((OnboardingWebNotificationImpl) onboardingWebNotification).processOnboardingJoined(evaluationDTO);

        assertNotNull(dto);
        assertEquals("SUBJ_FAMILY_UNIT", dto.getSubject());
        assertEquals("Email_DEC26/FamilyUnit", dto.getTemplateName());
        assertTrue(dto.getTemplateValues().containsKey("name"));
        assertEquals(evaluationDTO.getName(), dto.getTemplateValues().get("name"));
    }

    @Test
    void processOnboardingKo_shouldBuildThanksDto_whenInitiativeEnded() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        OnboardingRejectionReason rr = OnboardingRejectionReason.builder()
                .code(REJECTION_REASON_INITIATIVE_ENDED)
                .detail("ignored")
                .authorityLabel("ignored")
                .build();
        evaluationDTO.setOnboardingRejectionReasons(List.of(rr));
        evaluationDTO.setEmailFlux("DEC26");

        mockContent("DEC26", ONBOARDING_KO_INITIATIVE_ENDED, "SUBJ_KO_THANKS", "Email_DEC26/Thanks");

        EmailMessageDTO dto =
                ((OnboardingWebNotificationImpl) onboardingWebNotification).processOnboardingKo(evaluationDTO);

        assertNotNull(dto);
        assertEquals("SUBJ_KO_THANKS", dto.getSubject());
        assertEquals("Email_DEC26/Thanks", dto.getTemplateName());
        assertTrue(dto.getTemplateValues().containsKey("name"));
        assertEquals(evaluationDTO.getName(), dto.getTemplateValues().get("name"));
        assertFalse(dto.getTemplateValues().containsKey("reason"));
        assertFalse(dto.getTemplateValues().containsKey("managedEntity"));
    }

    @Test
    void processOnboardingKo_shouldBuildAlreadyUsedDto_whenFamilyBonusAlreadyUsed() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        OnboardingRejectionReason rr = OnboardingRejectionReason.builder()
                .code(FAMILY_BONUS_ALREADY_USED)
                .authority("Comune di Test")
                .detail("Bonus già utilizzato nel nucleo familiare")
                .build();
        evaluationDTO.setOnboardingRejectionReasons(List.of(rr));
        evaluationDTO.setEmailFlux("DEC26");

        mockContent("DEC26", ONBOARDING_KO_ALREADY_USED, "SUBJ_KO_ALREADY_USED", "Email_DEC26/AlreadyUsedKo");

        EmailMessageDTO dto =
                ((OnboardingWebNotificationImpl) onboardingWebNotification).processOnboardingKo(evaluationDTO);

        assertNotNull(dto);
        assertEquals("SUBJ_KO_ALREADY_USED", dto.getSubject());
        assertEquals("Email_DEC26/AlreadyUsedKo", dto.getTemplateName());
        assertEquals(evaluationDTO.getName(), dto.getTemplateValues().get("name"));
        assertEquals("Comune di Test", dto.getTemplateValues().get("managedEntity"));
        assertEquals("Bonus già utilizzato nel nucleo familiare", dto.getTemplateValues().get("reason"));
    }

    @Test
    void processOnboardingKo_shouldBuildGenericErrorDto_whenIseeTypeFail() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        OnboardingRejectionReason rr = OnboardingRejectionReason.builder()
                .code(ISEE_TYPE_FAIL)
                .authority(null)
                .build();
        evaluationDTO.setOnboardingRejectionReasons(List.of(rr));
        evaluationDTO.setEmailFlux("DEC26");

        mockContent("DEC26", ONBOARDING_KO_GENERIC, "SUBJ_KO_GENERIC", "Email_DEC26/GenericError");

        EmailMessageDTO dto =
                ((OnboardingWebNotificationImpl) onboardingWebNotification).processOnboardingKo(evaluationDTO);

        assertNotNull(dto);
        assertEquals("SUBJ_KO_GENERIC", dto.getSubject());
        assertEquals("Email_DEC26/GenericError", dto.getTemplateName());
        assertTrue(dto.getTemplateValues().containsKey("name"));
        assertEquals(evaluationDTO.getName(), dto.getTemplateValues().get("name"));
        assertEquals("Assistenza", dto.getTemplateValues().get("managedEntity"));
    }

    @Test
    void processOnboardingKo_shouldBuildGenericErrorDto_withRealValues_whenIseeTypeFail() {

        EvaluationDTO evaluationDTO = getEvaluationDto();
        OnboardingRejectionReason rr = OnboardingRejectionReason.builder()
                .code(ISEE_TYPE_FAIL)
                .authority("INPS")
                .build();
        evaluationDTO.setOnboardingRejectionReasons(List.of(rr));

        mockContent("DEC26", ONBOARDING_KO_GENERIC, "SUBJ_KO_GENERIC", "Email_DEC26/GenericError");

        evaluationDTO.setEmailFlux("DEC26");
        EmailMessageDTO dto =
                ((OnboardingWebNotificationImpl) onboardingWebNotification).processOnboardingKo(evaluationDTO);

        assertNotNull(dto);
        assertEquals("SUBJ_KO_GENERIC", dto.getSubject());
        assertEquals("Email_DEC26/GenericError", dto.getTemplateName());
        assertEquals(evaluationDTO.getName(), dto.getTemplateValues().get("name"));
        assertEquals("INPS", dto.getTemplateValues().get("managedEntity"));
    }

    @Test
    void processOnboardingKo_shouldBuildGenericErrorDto_withoutReasonAndManagedEntity_whenReasonsEmpty() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        evaluationDTO.setOnboardingRejectionReasons(List.of());
        evaluationDTO.setEmailFlux("DEC26");

        mockContent("DEC26", ONBOARDING_KO_GENERIC, "SUBJ_KO_GENERIC", "Email_DEC26/GenericError");

        EmailMessageDTO dto =
                ((OnboardingWebNotificationImpl) onboardingWebNotification).processOnboardingKo(evaluationDTO);

        assertNotNull(dto);
        assertEquals("SUBJ_KO_GENERIC", dto.getSubject());
        assertEquals("Email_DEC26/GenericError", dto.getTemplateName());
        assertEquals(evaluationDTO.getName(), dto.getTemplateValues().get("name"));
        assertFalse(dto.getTemplateValues().containsKey("reason"));
        assertFalse(dto.getTemplateValues().containsKey("managedEntity"));
    }
    @Test
    void notify_success() {
        when(notificationMapper.notificationToEmailMessageDTO(NOTIFICATION)).thenReturn(EMAIL_MESSAGE_DTO);

        // SendEmail success simulation
        ResponseEntity<Void> successResponse = new ResponseEntity<>(HttpStatus.OK);
        when(emailNotificationConnectorMock.sendEmail(EMAIL_MESSAGE_DTO))
                .thenReturn(successResponse);

        // Act
        boolean result = onboardingWebNotification.notify(NOTIFICATION);

        // Verify
        assertTrue(result, "true");
        //check that the mapper has been called 1 time
        verify(notificationMapper, times(1)).notificationToEmailMessageDTO(NOTIFICATION);
        //check that sendEmail has been called 1 time
        verify(emailNotificationConnectorMock, times(1)).sendEmail(EMAIL_MESSAGE_DTO);
    }

    @Test
    void notify_failure() {

        when(notificationMapper.notificationToEmailMessageDTO(NOTIFICATION)).thenReturn(EMAIL_MESSAGE_DTO);

        //sendEmail failure simulation
        doThrow(new RuntimeException("Email service down")).when(emailNotificationConnectorMock).sendEmail(EMAIL_MESSAGE_DTO);

        when(notificationManagerRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification savedNotification = invocation.getArgument(0);
            assertEquals(NotificationConstants.NOTIFICATION_STATUS_KO, savedNotification.getNotificationStatus());
            assertNotNull(savedNotification.getStatusKoTimestamp());
            return savedNotification;
        });

        // Act
        boolean result = onboardingWebNotification.notify(NOTIFICATION);

        // Verify
        assertFalse(result, "false");
        //check that the mapper has been called 1 time
        verify(notificationMapper, times(1)).notificationToEmailMessageDTO(NOTIFICATION);
        //check that sendEmail has been called 1 time
        verify(emailNotificationConnectorMock, times(1)).sendEmail(EMAIL_MESSAGE_DTO);
        //check that the notificationKo method has called the save method 1 time
        verify(notificationManagerRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void processNotification_whenFeignException_handlesError() {
        EvaluationDTO evaluationDTO = getEvaluationDto();

        when(notificationMapper.createNotificationFromEmailMessageDTO(
                any(EmailMessageDTO.class),
                eq(evaluationDTO)
        )).thenReturn(NOTIFICATION);

        when(initiativeRestConnector.getInitiativeDetailInfo(anyString()))
                .thenThrow(new feign.FeignException.InternalServerError(
                        "Internal Server Error",
                        mock(feign.Request.class),
                        new byte[0],
                        null
                ));

        mockContent(null, ONBOARDING_OK, "Fallback subject", "Email_null/ONBOARDING_OK");

        assertDoesNotThrow(
                () -> onboardingWebNotification.processNotification(evaluationDTO)
        );
    }

    @Test
    void processNotification_whenStatusJoined() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        evaluationDTO.setStatus(NotificationConstants.STATUS_ONBOARDING_JOINED);

        mockContent("DEC26", ONBOARDING_JOINED, "SUBJ_FAMILY_UNIT", "Email_DEC26/FamilyUnit");

        InitiativeNotificationDTO mockInitiative = new InitiativeNotificationDTO();
        mockInitiative.setEmailFlux("DEC26");
        when(initiativeRestConnector.getInitiativeDetailInfo(anyString())).thenReturn(mockInitiative);

        assertDoesNotThrow(() -> onboardingWebNotification.processNotification(evaluationDTO));
    }

    //region setReasonDetail tests
    @ParameterizedTest(name = "{2}")
    @MethodSource("provideSetReasonDetailScenarios")
    void setReasonDetail_shouldUseCorrectReasonValue(String detail, String expectedReason, String description) {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        OnboardingRejectionReason rr = OnboardingRejectionReason.builder()
                .code(AUTOMATED_CRITERIA_ISEE_FAIL)
                .detail(detail)
                .authority("INPS")
                .build();
        evaluationDTO.setOnboardingRejectionReasons(List.of(rr));
        evaluationDTO.setEmailFlux("DEC26");

        mockContent("DEC26", ONBOARDING_KO_GENERIC, "SUBJ_KO_GENERIC", "Email_DEC26/GenericError");

        EmailMessageDTO dto =
                ((OnboardingWebNotificationImpl) onboardingWebNotification).processOnboardingKo(evaluationDTO);

        assertNotNull(dto);
        assertTrue(dto.getTemplateValues().containsKey("reason"));
        assertEquals(expectedReason, dto.getTemplateValues().get("reason"), description);
    }

    private static Stream<Arguments> provideSetReasonDetailScenarios() {
        return Stream.of(
                Arguments.of(
                        "Detail Message",
                        "Detail Message",
                        "When detail is present and not blank, should use detail"
                ),
                Arguments.of(
                        null,
                        AUTOMATED_CRITERIA_ISEE_FAIL.getDetail(),
                        "When detail is null, should use codeDetail"
                ),
                Arguments.of(
                        "   ",
                        AUTOMATED_CRITERIA_ISEE_FAIL.getDetail(),
                        "When detail is blank (spaces), should use codeDetail"
                ),
                Arguments.of(
                        "",
                        AUTOMATED_CRITERIA_ISEE_FAIL.getDetail(),
                        "When detail is empty, should use codeDetail"
                )
        );
    }

    //endregion

    @Test
    void processOnboardingKo_shouldBuildGenericErrorDto_whenReasonsNull() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        evaluationDTO.setOnboardingRejectionReasons(null);
        evaluationDTO.setEmailFlux("DEC26");

        mockContent("DEC26", ONBOARDING_KO_GENERIC, "SUBJ_KO_GENERIC", "Email_DEC26/GenericError");

        EmailMessageDTO dto =
                ((OnboardingWebNotificationImpl) onboardingWebNotification).processOnboardingKo(evaluationDTO);

        assertNotNull(dto);
        assertEquals("SUBJ_KO_GENERIC", dto.getSubject());
        assertEquals("Email_DEC26/GenericError", dto.getTemplateName());
        assertEquals(evaluationDTO.getName(), dto.getTemplateValues().get("name"));
        assertFalse(dto.getTemplateValues().containsKey("reason"));
        assertFalse(dto.getTemplateValues().containsKey("managedEntity"));
    }

    @Test
    void processOnboardingKo_whenAssistedLinkConfigured_shouldIncludeAssistedLinkInTemplateValues() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        OnboardingRejectionReason rr = OnboardingRejectionReason.builder()
                .code(ISEE_TYPE_FAIL)
                .authority(null)
                .build();
        evaluationDTO.setOnboardingRejectionReasons(List.of(rr));
        evaluationDTO.setEmailFlux("DEC26");

        mockContent("DEC26", ONBOARDING_KO_GENERIC, "SUBJ_KO_GENERIC", "Email_DEC26/GenericError");

        ProfilesNotificationProperties.ProfileConfigVariables configVars =
                new ProfilesNotificationProperties.ProfileConfigVariables();
        configVars.setAssistedLink("https://assistance.example.com");
        when(profilesNotificationCatalog.retrieveProfileVariables("DEC26")).thenReturn(java.util.Optional.of(configVars));

        EmailMessageDTO dto =
                ((OnboardingWebNotificationImpl) onboardingWebNotification).processOnboardingKo(evaluationDTO);

        assertNotNull(dto);
        assertEquals("Assistenza", dto.getTemplateValues().get("managedEntity"));
        assertEquals("https://assistance.example.com", dto.getTemplateValues().get("assistedLink"));
    }

    @Test
    void processOnboardingKo_whenAuthorityIsExplicitlyAssistenza_shouldLookUpAssistedLink() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        OnboardingRejectionReason rr = OnboardingRejectionReason.builder()
                .code(ISEE_TYPE_FAIL)
                .authority("assistenza")
                .build();
        evaluationDTO.setOnboardingRejectionReasons(List.of(rr));
        evaluationDTO.setEmailFlux("DEC26");

        mockContent("DEC26", ONBOARDING_KO_GENERIC, "SUBJ_KO_GENERIC", "Email_DEC26/GenericError");

        ProfilesNotificationProperties.ProfileConfigVariables configVars =
                new ProfilesNotificationProperties.ProfileConfigVariables();
        configVars.setAssistedLink("https://assistenza.custom.it");
        when(profilesNotificationCatalog.retrieveProfileVariables("DEC26")).thenReturn(java.util.Optional.of(configVars));

        EmailMessageDTO dto =
                ((OnboardingWebNotificationImpl) onboardingWebNotification).processOnboardingKo(evaluationDTO);

        assertNotNull(dto);
        assertEquals("assistenza", dto.getTemplateValues().get("managedEntity"));
        assertEquals("https://assistenza.custom.it", dto.getTemplateValues().get("assistedLink"));
    }

    @Test
    void handleInitiativeFetchError_shouldMapAndSaveNotificationKo() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        Notification mockNotification = Notification.builder()
                .id("NOTIF_FETCH_ERROR")
                .build();

        when(notificationMapper.createNotificationFromEmailMessageDTO(
                any(EmailMessageDTO.class),
                eq(evaluationDTO)
        )).thenReturn(mockNotification);

        mockContent(null, ONBOARDING_OK, "Fallback subject", "Email_null/ONBOARDING_OK");

        ((OnboardingWebNotificationImpl) onboardingWebNotification)
                .handleInitiativeFetchError(evaluationDTO);

        verify(notificationMapper, times(1))
                .createNotificationFromEmailMessageDTO(
                        any(EmailMessageDTO.class),
                        eq(evaluationDTO)
                );

        verify(notificationManagerRepository, times(1))
                .save(argThat(notif ->
                        NotificationConstants.NOTIFICATION_STATUS_KO
                                .equals(notif.getNotificationStatus())
                                && notif.getStatusKoTimestamp() != null
                ));
    }

    @Test
    void processNotification_whenContentMissing_shouldNotSendOrSave() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        InitiativeNotificationDTO initiative = new InitiativeNotificationDTO();
        initiative.setEmailFlux("DEC26");
        when(initiativeRestConnector.getInitiativeDetailInfo(INITIATIVE_ID)).thenReturn(initiative);

        onboardingWebNotification.processNotification(evaluationDTO);

        verify(emailNotificationConnectorMock, never()).sendEmail(any());
        verify(notificationMapper, never()).createNotificationFromEmailMessageDTO(any(), any());
        verify(notificationManagerRepository, never()).save(any());
    }

    @Test
    void notify_success_withNotificationId_shouldDeleteById() {
        Notification notificationWithId = Notification.builder()
                .id("NOTIF_TO_DELETE_123")
                .userId("USER_123")
                .initiativeId(INITIATIVE_ID)
                .build();
        when(notificationMapper.notificationToEmailMessageDTO(notificationWithId)).thenReturn(EMAIL_MESSAGE_DTO);
        when(emailNotificationConnectorMock.sendEmail(EMAIL_MESSAGE_DTO)).thenReturn(new ResponseEntity<>(HttpStatus.OK));

        boolean result = onboardingWebNotification.notify(notificationWithId);

        assertTrue(result);
        verify(notificationManagerRepository, times(1)).deleteById("NOTIF_TO_DELETE_123");
    }

    @Test
    void processNotification_whenUnsupportedStatus_shouldNotSendNotification() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        evaluationDTO.setStatus("UNSUPPORTED_STATUS");

        InitiativeNotificationDTO mockInitiative = new InitiativeNotificationDTO();
        mockInitiative.setEmailFlux("DEC26");
        when(initiativeRestConnector.getInitiativeDetailInfo(anyString())).thenReturn(mockInitiative);

        String result = onboardingWebNotification.processNotification(evaluationDTO);

        assertNull(result);
        verify(emailNotificationConnectorMock, never()).sendEmail(any());
    }

    @Test
    void notify_whenBadTemplateName_shouldReplaceTemplateName() {
        Notification notification = Notification.builder()
                .id("NOTIFICATION_ID")
                .userId("USER_ID")
                .initiativeId(INITIATIVE_ID)
                .templateName("Email_null/EsitoOk")
                .event(ONBOARDING_OK)
                .build();

        InitiativeNotificationDTO initiative = new InitiativeNotificationDTO();
        initiative.setEmailFlux("DEC26");

        when(initiativeRestConnector.getInitiativeDetailInfo(INITIATIVE_ID))
                .thenReturn(initiative);
        mockContent("DEC26", ONBOARDING_OK, "TEST_OK", "Email_DEC26/EsitoOk");

        when(notificationMapper.notificationToEmailMessageDTO(notification))
                .thenReturn(EMAIL_MESSAGE_DTO);

        when(emailNotificationConnectorMock.sendEmail(EMAIL_MESSAGE_DTO))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        boolean result = onboardingWebNotification.notify(notification);

        assertTrue(result);
        assertEquals("Email_DEC26/EsitoOk", notification.getTemplateName());

        verify(initiativeRestConnector, times(1))
                .getInitiativeDetailInfo(INITIATIVE_ID);

        verify(notificationMapper, times(1))
                .notificationToEmailMessageDTO(notification);

        verify(emailNotificationConnectorMock, times(1))
                .sendEmail(EMAIL_MESSAGE_DTO);
    }

    @Test
    void notify_whenTemplateNameIsValid_shouldNotReplaceTemplateName() {
        Notification notification = Notification.builder()
                .id("NOTIFICATION_ID")
                .userId("USER_ID")
                .initiativeId(INITIATIVE_ID)
                .templateName("Email_DEC26/EsitoOk")
                .build();

        when(notificationMapper.notificationToEmailMessageDTO(notification))
                .thenReturn(EMAIL_MESSAGE_DTO);

        when(emailNotificationConnectorMock.sendEmail(EMAIL_MESSAGE_DTO))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        boolean result = onboardingWebNotification.notify(notification);

        assertTrue(result);
        assertEquals("Email_DEC26/EsitoOk", notification.getTemplateName());

        verify(initiativeRestConnector, never())
                .getInitiativeDetailInfo(anyString());

        verify(notificationMapper, times(1))
                .notificationToEmailMessageDTO(notification);
    }

    @Test
    void notify_whenTemplateNameIsNull_shouldNotReplaceTemplateName() {
        Notification notification = Notification.builder()
                .id("NOTIFICATION_ID")
                .userId("USER_ID")
                .initiativeId(INITIATIVE_ID)
                .templateName(null)
                .build();

        when(notificationMapper.notificationToEmailMessageDTO(notification))
                .thenReturn(EMAIL_MESSAGE_DTO);

        when(emailNotificationConnectorMock.sendEmail(EMAIL_MESSAGE_DTO))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        boolean result = onboardingWebNotification.notify(notification);

        assertTrue(result);
        assertNull(notification.getTemplateName());

        verify(initiativeRestConnector, never())
                .getInitiativeDetailInfo(anyString());
    }

    @Test
    void notify_whenTemplateNameIsBlank_shouldNotReplaceTemplateName() {
        Notification notification = Notification.builder()
                .id("NOTIFICATION_ID")
                .userId("USER_ID")
                .initiativeId(INITIATIVE_ID)
                .templateName(" ")
                .build();

        when(notificationMapper.notificationToEmailMessageDTO(notification))
                .thenReturn(EMAIL_MESSAGE_DTO);

        when(emailNotificationConnectorMock.sendEmail(EMAIL_MESSAGE_DTO))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        boolean result = onboardingWebNotification.notify(notification);

        assertTrue(result);
        assertEquals(" ", notification.getTemplateName());

        verify(initiativeRestConnector, never())
                .getInitiativeDetailInfo(anyString());
    }

}