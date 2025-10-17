package it.gov.pagopa.notification.manager.service.onboarding;

import it.gov.pagopa.notification.manager.config.EmailNotificationProperties;
import it.gov.pagopa.notification.manager.connector.EmailNotificationConnector;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EmailMessageDTO;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import it.gov.pagopa.notification.manager.dto.event.NotificationReminderQueueDTO;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationMapper;
import it.gov.pagopa.notification.manager.model.Notification;
import it.gov.pagopa.notification.manager.repository.NotificationManagerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.EmailTemplates.*;
import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode.ISEE_TYPE_FAIL;
import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode.REJECTION_REASON_INITIATIVE_ENDED;
import static it.gov.pagopa.notification.manager.enums.Channel.WEB;
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
    private EmailNotificationProperties emailNotificationPropertiesMock;

    private OnboardingWebNotification onboardingWebNotification;

    @Mock
    NotificationManagerRepository notificationManagerRepository;

    @Mock
    NotificationMapper notificationMapper;

    @BeforeEach
    void setUp() {
        onboardingWebNotification = new OnboardingWebNotificationImpl(emailNotificationConnectorMock,
                emailNotificationPropertiesMock,
                notificationManagerRepository,
                notificationMapper);
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
                true,
                "user@email.com",
                WEB,
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

        EmailNotificationProperties.Subject subjectMock = Mockito.mock(EmailNotificationProperties.Subject.class);
        Mockito.when(emailNotificationPropertiesMock.getSubject()).thenReturn(subjectMock);

        Mockito.when(subjectMock.getOk()).thenReturn("TEST_OK");

        Mockito.doAnswer(invocation -> null)
                .when(emailNotificationConnectorMock)
                .sendEmail(Mockito.any(EmailMessageDTO.class));

        when(notificationMapper.createNotificationFromEmailMessageDTO(any(EmailMessageDTO.class),
                any(EvaluationDTO.class))).thenReturn(NOTIFICATION);

        onboardingWebNotification.processNotification(evaluationDTO);


        Mockito.verify(emailNotificationConnectorMock, Mockito.times(1))
                .sendEmail(Mockito.argThat(email ->
                        email.getTemplateValues().containsKey("amount") &&
                                email.getTemplateValues().get("amount").equals("200")
                ));
    }

    @Test
    void processNotification_whenTemplateEsitoOkNoVerifyIsee100() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        evaluationDTO.setVerifyIsee(false);
        evaluationDTO.setBeneficiaryBudgetCents(10000L);

        EmailNotificationProperties.Subject subjectMock = Mockito.mock(EmailNotificationProperties.Subject.class);
        Mockito.when(emailNotificationPropertiesMock.getSubject()).thenReturn(subjectMock);

        Mockito.when(subjectMock.getOk()).thenReturn("TEST_OK");

        Mockito.doAnswer(invocation -> null)
                .when(emailNotificationConnectorMock)
                .sendEmail(Mockito.any(EmailMessageDTO.class));

        when(notificationMapper.createNotificationFromEmailMessageDTO(any(EmailMessageDTO.class),
                any(EvaluationDTO.class))).thenReturn(NOTIFICATION);

        onboardingWebNotification.processNotification(evaluationDTO);

        Mockito.verify(emailNotificationConnectorMock, Mockito.times(1))
                .sendEmail(Mockito.argThat(email ->
                        EMAIL_OUTCOME_OK.equals(email.getTemplateName())
                ));
    }

    @Test
    void processNotification_withNullBeneficiaryBudget() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        evaluationDTO.setVerifyIsee(false);
        evaluationDTO.setBeneficiaryBudgetCents(null);

        EmailNotificationProperties.Subject subjectMock = Mockito.mock(EmailNotificationProperties.Subject.class);
        Mockito.when(emailNotificationPropertiesMock.getSubject()).thenReturn(subjectMock);

        Mockito.when(subjectMock.getOk()).thenReturn("TEST_OK");

        Mockito.doAnswer(invocation -> null)
                .when(emailNotificationConnectorMock)
                .sendEmail(Mockito.any(EmailMessageDTO.class));

        when(notificationMapper.createNotificationFromEmailMessageDTO(any(EmailMessageDTO.class),
                any(EvaluationDTO.class))).thenReturn(NOTIFICATION);

        onboardingWebNotification.processNotification(evaluationDTO);

        Mockito.verify(emailNotificationConnectorMock, Mockito.times(1))
                .sendEmail(Mockito.argThat(email ->
                        !email.getTemplateValues().containsKey("amount")
                ));
    }

    @Test
    void processNotification_templateEsitoOkParziale() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        evaluationDTO.setVerifyIsee(true);
        evaluationDTO.setBeneficiaryBudgetCents(10000L);

        EmailNotificationProperties.Subject subjectMock = Mockito.mock(EmailNotificationProperties.Subject.class);
        Mockito.when(emailNotificationPropertiesMock.getSubject()).thenReturn(subjectMock);

        Mockito.when(subjectMock.getPartial()).thenReturn("TEST_PARTIAL_OK");

        Mockito.doAnswer(invocation -> null)
                .when(emailNotificationConnectorMock)
                .sendEmail(Mockito.any(EmailMessageDTO.class));

        when(notificationMapper.createNotificationFromEmailMessageDTO(any(EmailMessageDTO.class),
                any(EvaluationDTO.class))).thenReturn(NOTIFICATION);

        onboardingWebNotification.processNotification(evaluationDTO);

        Mockito.verify(emailNotificationConnectorMock, Mockito.times(1))
                .sendEmail(Mockito.argThat(email ->
                        EMAIL_OUTCOME_PARTIAL.equals(email.getTemplateName())
                ));
    }

    @Test
    void processNotification_callNotificationEmail_Error() {
        EvaluationDTO evaluationDTO = getEvaluationDto();

        EmailNotificationProperties.Subject subjectMock = Mockito.mock(EmailNotificationProperties.Subject.class);
        Mockito.when(emailNotificationPropertiesMock.getSubject()).thenReturn(subjectMock);

        Mockito.when(subjectMock.getOk()).thenReturn("TEST_OK");

        Mockito.doThrow(new RuntimeException("Boom"))
                .when(emailNotificationConnectorMock)
                .sendEmail(Mockito.any(EmailMessageDTO.class));

        when(notificationMapper.createNotificationFromEmailMessageDTO(any(EmailMessageDTO.class),
                any(EvaluationDTO.class))).thenReturn(NOTIFICATION);

        onboardingWebNotification.processNotification(evaluationDTO);

        Mockito.verify(emailNotificationConnectorMock, Mockito.times(1))
                .sendEmail(Mockito.any(EmailMessageDTO.class));
    }

    @Test
    void processOnboardingJoined_shouldBuildFamilyUnitEmailDto() {
        EvaluationDTO evaluationDTO = getEvaluationDto();

        EmailNotificationProperties.Subject subjectMock = Mockito.mock(EmailNotificationProperties.Subject.class);
        Mockito.when(emailNotificationPropertiesMock.getSubject()).thenReturn(subjectMock);
        Mockito.when(subjectMock.getKoFamilyUnit()).thenReturn("SUBJ_FAMILY_UNIT");

        EmailMessageDTO dto =
                ((OnboardingWebNotificationImpl) onboardingWebNotification).processOnboardingJoined(evaluationDTO);

        assertNotNull(dto);
        assertEquals("SUBJ_FAMILY_UNIT", dto.getSubject());
        assertEquals(EMAIL_OUTCOME_FAMILY_UNIT, dto.getTemplateName());
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

        EmailNotificationProperties.Subject subjectMock = Mockito.mock(EmailNotificationProperties.Subject.class);
        Mockito.when(emailNotificationPropertiesMock.getSubject()).thenReturn(subjectMock);
        Mockito.when(subjectMock.getKoThanks()).thenReturn("SUBJ_KO_THANKS");

        EmailMessageDTO dto =
                ((OnboardingWebNotificationImpl) onboardingWebNotification).processOnboardingKo(evaluationDTO);

        assertNotNull(dto);
        assertEquals("SUBJ_KO_THANKS", dto.getSubject());
        assertEquals(EMAIL_OUTCOME_THANKS, dto.getTemplateName());
        assertTrue(dto.getTemplateValues().containsKey("name"));
        assertEquals(evaluationDTO.getName(), dto.getTemplateValues().get("name"));
        assertFalse(dto.getTemplateValues().containsKey("reason"));
        assertFalse(dto.getTemplateValues().containsKey("managedEntity"));
    }

    @Test
    void processOnboardingKo_shouldBuildGenericErrorDto_whenIseeTypeFail() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        OnboardingRejectionReason rr = OnboardingRejectionReason.builder()
                .code(ISEE_TYPE_FAIL)
                .detail(null)
                .authorityLabel(null)
                .build();
        evaluationDTO.setOnboardingRejectionReasons(List.of(rr));

        EmailNotificationProperties.Subject subjectMock = Mockito.mock(EmailNotificationProperties.Subject.class);
        Mockito.when(emailNotificationPropertiesMock.getSubject()).thenReturn(subjectMock);
        Mockito.when(subjectMock.getKoGenericError()).thenReturn("SUBJ_KO_GENERIC");

        EmailMessageDTO dto =
                ((OnboardingWebNotificationImpl) onboardingWebNotification).processOnboardingKo(evaluationDTO);

        assertNotNull(dto);
        assertEquals("SUBJ_KO_GENERIC", dto.getSubject());
        assertEquals(EMAIL_OUTCOME_GENERIC_ERROR, dto.getTemplateName());
        assertTrue(dto.getTemplateValues().containsKey("name"));
        assertEquals(evaluationDTO.getName(), dto.getTemplateValues().get("name"));
        assertEquals("REASON", dto.getTemplateValues().get("reason"));
        assertEquals("HELPDESK", dto.getTemplateValues().get("managedEntity"));
    }

    @Test
    void processOnboardingKo_shouldBuildGenericErrorDto_withRealValues_whenIseeTypeFail() {

        EvaluationDTO evaluationDTO = getEvaluationDto();
        OnboardingRejectionReason rr = OnboardingRejectionReason.builder()
                .code(ISEE_TYPE_FAIL)
                .detail("ISEE non valido")
                .authorityLabel("INPS")
                .build();
        evaluationDTO.setOnboardingRejectionReasons(List.of(rr));

        EmailNotificationProperties.Subject subjectMock = Mockito.mock(EmailNotificationProperties.Subject.class);
        Mockito.when(emailNotificationPropertiesMock.getSubject()).thenReturn(subjectMock);
        Mockito.when(subjectMock.getKoGenericError()).thenReturn("SUBJ_KO_GENERIC");

        EmailMessageDTO dto =
                ((OnboardingWebNotificationImpl) onboardingWebNotification).processOnboardingKo(evaluationDTO);

        assertNotNull(dto);
        assertEquals("SUBJ_KO_GENERIC", dto.getSubject());
        assertEquals(EMAIL_OUTCOME_GENERIC_ERROR, dto.getTemplateName());
        assertEquals(evaluationDTO.getName(), dto.getTemplateValues().get("name"));
        assertEquals("ISEE non valido", dto.getTemplateValues().get("reason"));
        assertEquals("INPS", dto.getTemplateValues().get("managedEntity"));
    }

    @Test
    void processOnboardingKo_shouldBuildGenericErrorDto_withoutReasonAndManagedEntity_whenReasonsEmpty() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        evaluationDTO.setOnboardingRejectionReasons(List.of());

        EmailNotificationProperties.Subject subjectMock = Mockito.mock(EmailNotificationProperties.Subject.class);
        Mockito.when(emailNotificationPropertiesMock.getSubject()).thenReturn(subjectMock);
        Mockito.when(subjectMock.getKoGenericError()).thenReturn("SUBJ_KO_GENERIC");

        EmailMessageDTO dto =
                ((OnboardingWebNotificationImpl) onboardingWebNotification).processOnboardingKo(evaluationDTO);

        assertNotNull(dto);
        assertEquals("SUBJ_KO_GENERIC", dto.getSubject());
        assertEquals(EMAIL_OUTCOME_GENERIC_ERROR, dto.getTemplateName());
        assertEquals(evaluationDTO.getName(), dto.getTemplateValues().get("name"));
        assertFalse(dto.getTemplateValues().containsKey("reason"));
        assertFalse(dto.getTemplateValues().containsKey("managedEntity"));
    }
    @Test
    void notify_success() {
        when(notificationMapper.notificationToEmailMessageDTO(NOTIFICATION)).thenReturn(EMAIL_MESSAGE_DTO);

        // SendEmail success simulation
        ResponseEntity<Void> successResponse = new ResponseEntity<>(HttpStatus.OK);
        Mockito.when(emailNotificationConnectorMock.sendEmail(EMAIL_MESSAGE_DTO))
                .thenReturn(successResponse);

        when(notificationManagerRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification savedNotification = invocation.getArgument(0);
            assertEquals(NotificationConstants.NOTIFICATION_STATUS_OK, savedNotification.getNotificationStatus());
            return savedNotification;
        });

        // Act
        boolean result = onboardingWebNotification.notify(NOTIFICATION);

        // Verify
        assertTrue(result, "true");
        //check that the mapper has been called 1 time
        verify(notificationMapper, times(1)).notificationToEmailMessageDTO(NOTIFICATION);
        //check that sendEmail has been called 1 time
        verify(emailNotificationConnectorMock, times(1)).sendEmail(EMAIL_MESSAGE_DTO);
        //check that the notificationSent method has called the save method 1 time
        verify(notificationManagerRepository, times(1)).save(any(Notification.class));
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
}