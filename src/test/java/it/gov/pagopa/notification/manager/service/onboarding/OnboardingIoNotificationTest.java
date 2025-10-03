package it.gov.pagopa.notification.manager.service.onboarding;

import feign.FeignException;
import feign.Request;
import feign.Response;
import it.gov.pagopa.notification.manager.config.NotificationProperties;
import it.gov.pagopa.notification.manager.connector.IOBackEndRestConnector;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationResource;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationDTOMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static it.gov.pagopa.notification.manager.enums.Channel.IO;

@ExtendWith(MockitoExtension.class)
class OnboardingIoNotificationTest {
    private static final String TEST_TOKEN = "TEST_TOKEN";
    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    private static final String INITIATIVE_NAME = "INITIATIVE_NAME";
    private static final LocalDateTime TEST_DATE = LocalDateTime.now();
    private static final LocalDate TEST_DATE_ONLY_DATE = LocalDate.now();
    private static final String ORGANIZATION_NAME = "ORGANIZATION_NAME";
    private static final String MARKDOWN_DOUBLE_LINE = "\n\n";
    private static final String MESSAGE_ID = "ID";
    private static final String MARKDOWN_CTA_OK = """
            it:
                        cta_1:
                            text: "Vai al bonus"
                            action: "ioit://idpay/initiative/%initiativeId%"
                    en:
                        cta_1:
                            text: "Go to the bonus page"
                            action: "ioit://idpay/initiative/%initiativeId%"
            """;

    private static final String MARKDOWN_OK_BEL = """
            Buone notizie! Hai ottenuto il %initiativeName% da %rewardAmount%€. sarà valido **per i prossimi 10 giorni**.

            È disponibile ora nella sezione Portafoglio, dove troverai i dettagli dell'importo e la validità.

            **Cosa puoi fare ora?**

            Puoi usarlo per ottenere uno sconto del 30% sul prezzo d'acquisto di un elettrodomestico nuovo ad alta efficienza, fino ad un massimo di %rewardAmount%€.

            **Come fare?**

            Per avere più informazioni su come e dove usare il bonus, [leggi i dettagli](http://www.google.com/).

            **Importante:** ricorda che puoi usare il bonus solo se hai un vecchio elettrodomestico da smaltire. Concorda con il venditore quando e come consegnarlo, ma non smaltirlo autonomamente in discarica.""";

    private static final String MARKDOWN_OK_PARTIAL_BEL = """
            Dopo le verifiche con INPS, il tuo ISEE risulta difforme o diverso da quanto dichiarato. Per questo motivo, non è possibile assegnarti il bonus da 200€ che avevi richiesto, ma hai ottenuto comunque il **bonus di 100€.**

            Se ritiene che ci sia un errore, verifica il tuo ISEE direttamente con INPS.

            **Cosa puoi fare ora?**

            Puoi usarlo per ottenere uno sconto del 30% sul prezzo d'acquisto di un elettrodomestico nuovo ad alta efficienza, fino ad un massimo di 100€,

            **Come fare?**

            Per avere più informazioni su come e dove usare il bonus, [leggi i dettagli](http://www.google.com/).

            **Importante:** ricorda che puoi usare il bonus solo se hai un vecchio elettrodomestico da smaltire. Concorda con il venditore quando e come consegnarlo, ma non smaltirlo autonomamente in discarica.""";

    private static final String SUBJECT_OK = "Hai ottenuto il bonus!";
    private static final String SUBJECT_OK_PARTIAL = "Hai ottenuto il bonus!";

    @Mock
    private NotificationProperties notificationPropertiesMock;

    private NotificationDTOMapper notificationDTOMapper = new NotificationDTOMapper();
    @Mock
    private IOBackEndRestConnector ioBackEndRestConnectorMock;

    private OnboardingIoNotification onboardingIoNotification;

    @BeforeEach
    void setUp() {
        onboardingIoNotification = new OnboardingIoNotificationImpl(notificationPropertiesMock, notificationDTOMapper, ioBackEndRestConnectorMock, 10L);
    }

    private EvaluationDTO getEvaluationDto(){
        return new EvaluationDTO(
                TEST_TOKEN,
                INITIATIVE_ID,
                INITIATIVE_NAME,
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
                null,
                IO,
                "FISCAL_CODE",
                "IO_TOKEN"
        );
    }

    //region ONBOARDING_OK
    @Test
    void processNotification_whenTemplateEsitoOk200() {
        EvaluationDTO evaluationDTO = getEvaluationDto();

        NotificationProperties.Subject subjectMock = Mockito.mock(NotificationProperties.Subject.class);
        Mockito.when(notificationPropertiesMock.getSubject()).thenReturn(subjectMock);
        Mockito.when(subjectMock.getOkBel()).thenReturn(SUBJECT_OK);

        NotificationProperties.Markdown markdownMock = Mockito.mock(NotificationProperties.Markdown.class);
        Mockito.when(notificationPropertiesMock.getMarkdown()).thenReturn(markdownMock);
        Mockito.when(markdownMock.getOkBel()).thenReturn(MARKDOWN_OK_BEL);
        Mockito.when(markdownMock.getDoubleNewLine()).thenReturn(MARKDOWN_DOUBLE_LINE);
        Mockito.when(markdownMock.getOkCta()).thenReturn(MARKDOWN_CTA_OK);

        NotificationResource notificationResource = Mockito.mock(NotificationResource.class);
        Mockito.when(ioBackEndRestConnectorMock.notify(Mockito.any(NotificationDTO.class), Mockito.anyString()))
                .thenReturn(notificationResource);
        Mockito.when(notificationResource.getId()).thenReturn(MESSAGE_ID);

        String result = onboardingIoNotification.processNotification(evaluationDTO);

        String expectedMarkdown = """
                it:
                            cta_1:
                                text: "Vai al bonus"
                                action: "ioit://idpay/initiative/INITIATIVE_ID"
                        en:
                            cta_1:
                                text: "Go to the bonus page"
                                action: "ioit://idpay/initiative/INITIATIVE_ID"


                Buone notizie! Hai ottenuto il INITIATIVE_NAME da 200€. sarà valido **per i prossimi 10 giorni**.

                È disponibile ora nella sezione Portafoglio, dove troverai i dettagli dell'importo e la validità.

                **Cosa puoi fare ora?**

                Puoi usarlo per ottenere uno sconto del 30% sul prezzo d'acquisto di un elettrodomestico nuovo ad alta efficienza, fino ad un massimo di 200€.

                **Come fare?**

                Per avere più informazioni su come e dove usare il bonus, [leggi i dettagli](http://www.google.com/).

                **Importante:** ricorda che puoi usare il bonus solo se hai un vecchio elettrodomestico da smaltire. Concorda con il venditore quando e come consegnarlo, ma non smaltirlo autonomamente in discarica.""";

        Mockito.verify(ioBackEndRestConnectorMock, Mockito.times(1))
                .notify(Mockito.argThat(notificationDTO -> notificationDTO.getContent().getMarkdown().equals(expectedMarkdown)), Mockito.anyString());

        Assertions.assertNotNull(result);
        Assertions.assertEquals(MESSAGE_ID, result);
    }

    @Test
    void processNotification_whenTemplateEsitoOkNoVerifyIsee100() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        evaluationDTO.setVerifyIsee(false);
        evaluationDTO.setBeneficiaryBudgetCents(10000L);

        NotificationProperties.Subject subjectMock = Mockito.mock(NotificationProperties.Subject.class);
        Mockito.when(notificationPropertiesMock.getSubject()).thenReturn(subjectMock);
        Mockito.when(subjectMock.getOkBel()).thenReturn(SUBJECT_OK);

        NotificationProperties.Markdown markdownMock = Mockito.mock(NotificationProperties.Markdown.class);
        Mockito.when(notificationPropertiesMock.getMarkdown()).thenReturn(markdownMock);
        Mockito.when(markdownMock.getOkBel()).thenReturn(MARKDOWN_OK_BEL);
        Mockito.when(markdownMock.getDoubleNewLine()).thenReturn(MARKDOWN_DOUBLE_LINE);
        Mockito.when(markdownMock.getOkCta()).thenReturn(MARKDOWN_CTA_OK);

        NotificationResource notificationResource = Mockito.mock(NotificationResource.class);
        Mockito.when(ioBackEndRestConnectorMock.notify(Mockito.any(NotificationDTO.class), Mockito.anyString()))
                .thenReturn(notificationResource);
        Mockito.when(notificationResource.getId()).thenReturn(MESSAGE_ID);

        String result = onboardingIoNotification.processNotification(evaluationDTO);

        String expectedMarkdown = """
                it:
                            cta_1:
                                text: "Vai al bonus"
                                action: "ioit://idpay/initiative/INITIATIVE_ID"
                        en:
                            cta_1:
                                text: "Go to the bonus page"
                                action: "ioit://idpay/initiative/INITIATIVE_ID"


                Buone notizie! Hai ottenuto il INITIATIVE_NAME da 100€. sarà valido **per i prossimi 10 giorni**.

                È disponibile ora nella sezione Portafoglio, dove troverai i dettagli dell'importo e la validità.

                **Cosa puoi fare ora?**

                Puoi usarlo per ottenere uno sconto del 30% sul prezzo d'acquisto di un elettrodomestico nuovo ad alta efficienza, fino ad un massimo di 100€.

                **Come fare?**

                Per avere più informazioni su come e dove usare il bonus, [leggi i dettagli](http://www.google.com/).

                **Importante:** ricorda che puoi usare il bonus solo se hai un vecchio elettrodomestico da smaltire. Concorda con il venditore quando e come consegnarlo, ma non smaltirlo autonomamente in discarica.""";


        Mockito.verify(ioBackEndRestConnectorMock, Mockito.times(1))
                .notify(Mockito.argThat(notificationDTO -> notificationDTO.getContent().getMarkdown().equals(expectedMarkdown)), Mockito.anyString());

        Assertions.assertNotNull(result);
        Assertions.assertEquals(MESSAGE_ID, result);
    }

    @Test
    void processNotification_withNullBeneficiaryBudget() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        evaluationDTO.setVerifyIsee(false);
        evaluationDTO.setBeneficiaryBudgetCents(null);

        NotificationProperties.Subject subjectMock = Mockito.mock(NotificationProperties.Subject.class);
        Mockito.when(notificationPropertiesMock.getSubject()).thenReturn(subjectMock);
        Mockito.when(subjectMock.getOkBel()).thenReturn(SUBJECT_OK);

        NotificationProperties.Markdown markdownMock = Mockito.mock(NotificationProperties.Markdown.class);
        Mockito.when(notificationPropertiesMock.getMarkdown()).thenReturn(markdownMock);
        Mockito.when(markdownMock.getOkBel()).thenReturn(MARKDOWN_OK_BEL);
        Mockito.when(markdownMock.getDoubleNewLine()).thenReturn(MARKDOWN_DOUBLE_LINE);
        Mockito.when(markdownMock.getOkCta()).thenReturn(MARKDOWN_CTA_OK);

        NotificationResource notificationResource = Mockito.mock(NotificationResource.class);
        Mockito.when(ioBackEndRestConnectorMock.notify(Mockito.any(NotificationDTO.class), Mockito.anyString()))
                .thenReturn(notificationResource);
        Mockito.when(notificationResource.getId()).thenReturn(MESSAGE_ID);

        String result = onboardingIoNotification.processNotification(evaluationDTO);

        String expectedMarkdown = """
                it:
                            cta_1:
                                text: "Vai al bonus"
                                action: "ioit://idpay/initiative/INITIATIVE_ID"
                        en:
                            cta_1:
                                text: "Go to the bonus page"
                                action: "ioit://idpay/initiative/INITIATIVE_ID"


                Buone notizie! Hai ottenuto il INITIATIVE_NAME da N.A.€. sarà valido **per i prossimi 10 giorni**.

                È disponibile ora nella sezione Portafoglio, dove troverai i dettagli dell'importo e la validità.

                **Cosa puoi fare ora?**

                Puoi usarlo per ottenere uno sconto del 30% sul prezzo d'acquisto di un elettrodomestico nuovo ad alta efficienza, fino ad un massimo di N.A.€.

                **Come fare?**

                Per avere più informazioni su come e dove usare il bonus, [leggi i dettagli](http://www.google.com/).

                **Importante:** ricorda che puoi usare il bonus solo se hai un vecchio elettrodomestico da smaltire. Concorda con il venditore quando e come consegnarlo, ma non smaltirlo autonomamente in discarica.""";


        Mockito.verify(ioBackEndRestConnectorMock, Mockito.times(1))
                .notify(Mockito.argThat(notificationDTO -> notificationDTO.getContent().getMarkdown().equals(expectedMarkdown)), Mockito.anyString());

        Assertions.assertNotNull(result);
        Assertions.assertEquals(MESSAGE_ID, result);
    }

    @Test
    void processNotification_templateEsitoOkParziale() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        evaluationDTO.setVerifyIsee(true);
        evaluationDTO.setBeneficiaryBudgetCents(10000L);

        NotificationProperties.Subject subjectMock = Mockito.mock(NotificationProperties.Subject.class);
        Mockito.when(notificationPropertiesMock.getSubject()).thenReturn(subjectMock);
        Mockito.when(subjectMock.getOkPartialBel()).thenReturn(SUBJECT_OK_PARTIAL);

        NotificationProperties.Markdown markdownMock = Mockito.mock(NotificationProperties.Markdown.class);
        Mockito.when(notificationPropertiesMock.getMarkdown()).thenReturn(markdownMock);
        Mockito.when(markdownMock.getOkPartialBel()).thenReturn(MARKDOWN_OK_PARTIAL_BEL);
        Mockito.when(markdownMock.getDoubleNewLine()).thenReturn(MARKDOWN_DOUBLE_LINE);
        Mockito.when(markdownMock.getOkCta()).thenReturn(MARKDOWN_CTA_OK);

        NotificationResource notificationResource = Mockito.mock(NotificationResource.class);
        Mockito.when(ioBackEndRestConnectorMock.notify(Mockito.any(NotificationDTO.class), Mockito.anyString()))
                .thenReturn(notificationResource);
        Mockito.when(notificationResource.getId()).thenReturn(MESSAGE_ID);

        String result = onboardingIoNotification.processNotification(evaluationDTO);

        String expectedMarkdown = """
                it:
                            cta_1:
                                text: "Vai al bonus"
                                action: "ioit://idpay/initiative/INITIATIVE_ID"
                        en:
                            cta_1:
                                text: "Go to the bonus page"
                                action: "ioit://idpay/initiative/INITIATIVE_ID"


                Dopo le verifiche con INPS, il tuo ISEE risulta difforme o diverso da quanto dichiarato. Per questo motivo, non è possibile assegnarti il bonus da 200€ che avevi richiesto, ma hai ottenuto comunque il **bonus di 100€.**

                Se ritiene che ci sia un errore, verifica il tuo ISEE direttamente con INPS.

                **Cosa puoi fare ora?**

                Puoi usarlo per ottenere uno sconto del 30% sul prezzo d'acquisto di un elettrodomestico nuovo ad alta efficienza, fino ad un massimo di 100€,

                **Come fare?**

                Per avere più informazioni su come e dove usare il bonus, [leggi i dettagli](http://www.google.com/).

                **Importante:** ricorda che puoi usare il bonus solo se hai un vecchio elettrodomestico da smaltire. Concorda con il venditore quando e come consegnarlo, ma non smaltirlo autonomamente in discarica.""";

        Mockito.verify(ioBackEndRestConnectorMock, Mockito.times(1))
                .notify(Mockito.argThat(notificationDTO -> notificationDTO.getContent().getMarkdown().equals(expectedMarkdown)), Mockito.anyString());

        Assertions.assertNotNull(result);
        Assertions.assertEquals("ID", result);
    }

    @Test
    void processNotification_callNotificationEmail_Error() {
        EvaluationDTO evaluationDTO = getEvaluationDto();
        evaluationDTO.setVerifyIsee(false);
        evaluationDTO.setBeneficiaryBudgetCents(10000L);

        NotificationProperties.Subject subjectMock = Mockito.mock(NotificationProperties.Subject.class);
        Mockito.when(notificationPropertiesMock.getSubject()).thenReturn(subjectMock);
        Mockito.when(subjectMock.getOkBel()).thenReturn(SUBJECT_OK);

        NotificationProperties.Markdown markdownMock = Mockito.mock(NotificationProperties.Markdown.class);
        Mockito.when(notificationPropertiesMock.getMarkdown()).thenReturn(markdownMock);
        Mockito.when(markdownMock.getOkBel()).thenReturn(MARKDOWN_OK_BEL);
        Mockito.when(markdownMock.getDoubleNewLine()).thenReturn(MARKDOWN_DOUBLE_LINE);
        Mockito.when(markdownMock.getOkCta()).thenReturn(MARKDOWN_CTA_OK);

        Request request = Request.create(Request.HttpMethod.GET, "/dummy", Map.of(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder()
                .status(429)
                .reason("DUMMY_EXCEPTION")
                .request(request)
                .build();
        Mockito.when(ioBackEndRestConnectorMock.notify(Mockito.any(NotificationDTO.class), Mockito.anyString()))
                .thenThrow(FeignException.errorStatus("DUMMY_CLIENT", response));

        String result = onboardingIoNotification.processNotification(evaluationDTO);

        Assertions.assertNull(result);
    }

    //endregion

    //region Onboarding JOINED
    @Test
    void onboardingJoined(){
        EvaluationDTO evaluationDTO = getEvaluationDto();
        evaluationDTO.setStatus( NotificationConstants.STATUS_ONBOARDING_JOINED);

        String result = onboardingIoNotification.processNotification(evaluationDTO);

        Assertions.assertNull(result);
    }
    //endregion

    //region ONBOARDING_KO
    @Test
    void onboardingKO(){
        EvaluationDTO evaluationDTO = getEvaluationDto();
        evaluationDTO.setStatus( NotificationConstants.STATUS_ONBOARDING_KO);

        String result = onboardingIoNotification.processNotification(evaluationDTO);

        Assertions.assertNull(result);
    }
    //endregion
}