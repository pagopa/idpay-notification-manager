package it.gov.pagopa.notification.manager.service.onboarding;

import it.gov.pagopa.notification.manager.config.EmailNotificationProperties;
import it.gov.pagopa.notification.manager.connector.EmailNotificationConnector;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EmailMessageDTO;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.EmailTemplates.*;
import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode.ISEE_TYPE_FAIL;
import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode.REJECTION_REASON_INITIATIVE_ENDED;
import static it.gov.pagopa.notification.manager.enums.Channel.WEB;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OnboardingWebNotificationTest {
    private static final String TEST_TOKEN = "TEST_TOKEN";
    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    private static final LocalDateTime TEST_DATE = LocalDateTime.now();
    private static final LocalDate TEST_DATE_ONLY_DATE = LocalDate.now();
    private static final String ORGANIZATION_NAME = "ORGANIZATION_NAME";

    @Mock
    private EmailNotificationConnector emailNotificationConnectorMock;

    @Mock
    private EmailNotificationProperties emailNotificationPropertiesMock;

    private OnboardingWebNotification onboardingWebNotification;

    @BeforeEach
    void setUp() {
        onboardingWebNotification = new OnboardingWebNotificationImpl(emailNotificationConnectorMock, emailNotificationPropertiesMock);
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
        assertEquals(getNameSurname(evaluationDTO), dto.getTemplateValues().get("name"));
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
        assertEquals(getNameSurname(evaluationDTO), dto.getTemplateValues().get("name"));
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
        assertEquals(getNameSurname(evaluationDTO), dto.getTemplateValues().get("name"));
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
        assertEquals(getNameSurname(evaluationDTO), dto.getTemplateValues().get("name"));
        assertEquals("ISEE non valido", dto.getTemplateValues().get("reason"));
        assertEquals("INPS", dto.getTemplateValues().get("managedEntity"));
    }

    @NotNull
    private String getNameSurname(EvaluationDTO evaluationDTO) {
        return evaluationDTO.getName() + " " + evaluationDTO.getSurname();
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
        assertEquals(getNameSurname(evaluationDTO), dto.getTemplateValues().get("name"));
        assertFalse(dto.getTemplateValues().containsKey("reason"));
        assertFalse(dto.getTemplateValues().containsKey("managedEntity"));
    }
}