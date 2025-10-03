package it.gov.pagopa.notification.manager.service.onboarding;

import it.gov.pagopa.notification.manager.config.EmailNotificationProperties;
import it.gov.pagopa.notification.manager.connector.EmailNotificationConnector;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EmailMessageDTO;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.EmailTemplates.EMAIL_OUTCOME_OK;
import static it.gov.pagopa.notification.manager.constants.NotificationConstants.EmailTemplates.EMAIL_OUTCOME_PARTIAL;
import static it.gov.pagopa.notification.manager.enums.Channel.WEB;

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

    //endregion

    //region ONBOARDING_JOINED
    @Test
    void processNotification_onboardingStatusJoined(){
        EvaluationDTO evaluationDTO = getEvaluationDto();
        evaluationDTO.setStatus( NotificationConstants.STATUS_ONBOARDING_JOINED);

        onboardingWebNotification.processNotification(evaluationDTO);

        Mockito.verify(emailNotificationConnectorMock, Mockito.never())
                .sendEmail(Mockito.any());
    }
    //endregion

    //region ONBOARDING_KO
    @Test
    void processNotification_onboardingStatusKo(){
        EvaluationDTO evaluationDTO = getEvaluationDto();
        evaluationDTO.setStatus( NotificationConstants.STATUS_ONBOARDING_KO);

        onboardingWebNotification.processNotification(evaluationDTO);

        Mockito.verify(emailNotificationConnectorMock, Mockito.never())
                .sendEmail(Mockito.any());
    }
    //endregion

}