package it.gov.pagopa.notification.manager.service.onboarding;

import feign.FeignException;
import feign.Request;
import it.gov.pagopa.notification.manager.config.ProfilesNotificationProperties;
import it.gov.pagopa.notification.manager.connector.IOBackEndRestConnector;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationResource;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import it.gov.pagopa.notification.manager.dto.VerifyDTO;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeNotificationDTO;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationDTOMapper;
import it.gov.pagopa.notification.manager.enums.Channel;
import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.exception.NotFoundNotificationEventTemplate;
import it.gov.pagopa.notification.manager.model.NotificationContent;
import it.gov.pagopa.notification.manager.service.io.IoNotificationContentService;
import it.gov.pagopa.notification.manager.service.io.ProfilesNotificationCatalog;
import it.gov.pagopa.notification.manager.service.io.placeholder.OnboardingPlaceholderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.STATUS_ONBOARDING_JOINED;
import static it.gov.pagopa.notification.manager.constants.NotificationConstants.STATUS_ONBOARDING_KO;
import static it.gov.pagopa.notification.manager.constants.NotificationConstants.STATUS_ONBOARDING_OK;
import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode.FAMILY_CRITERIA_FAIL;
import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode.REJECTION_REASON_INITIATIVE_ENDED;
import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingIoNotificationTest {

    private static final String PROFILE = "BEL25";

    @Mock
    private IoNotificationContentService notificationContentService;
    @Mock
    private ProfilesNotificationCatalog profilesNotificationCatalog;
    @Mock
    private IOBackEndRestConnector ioBackEndRestConnector;
    @Mock
    private InitiativeRestConnector initiativeRestConnector;

    private OnboardingIoNotification notification;

    @BeforeEach
    void setUp() {
        notification = new OnboardingIoNotificationImpl(
                notificationContentService,
                profilesNotificationCatalog,
                new NotificationDTOMapper(),
                ioBackEndRestConnector,
                initiativeRestConnector,
                3600L
        );
    }

    @Test
    void processNotification_resolvesProfileLoadsEventAndRendersContent() {
        EvaluationDTO evaluation = evaluation(STATUS_ONBOARDING_OK);
        prepareInitiativeAndProfile();
        when(notificationContentService.resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_OK),
                any(OnboardingPlaceholderContext.class),
                any()
        ))
                .thenReturn(Optional.of(new NotificationContent(
                        "Bonus Bonus",
                        "Hai ottenuto Bonus per 250 euro"
                )));
        mockSentNotification();

        assertEquals("notification-id", notification.processNotification(evaluation));

        verify(notificationContentService).resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_OK),
                any(OnboardingPlaceholderContext.class),
                any()
        );
        ArgumentCaptor<NotificationDTO> sent = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(ioBackEndRestConnector).notify(sent.capture(), eq("io-token"));
        assertEquals("Bonus Bonus", sent.getValue().getContent().getSubject());
        assertEquals(
                "Hai ottenuto Bonus per 250 euro",
                sent.getValue().getContent().getMarkdown()
        );
    }

    @Test
    void processNotification_usesInitiativeEndedEvent() {
        EvaluationDTO evaluation = evaluation(STATUS_ONBOARDING_KO);
        evaluation.setOnboardingRejectionReasons(List.of(OnboardingRejectionReason.builder()
                .type(INVALID_REQUEST)
                .code(REJECTION_REASON_INITIATIVE_ENDED)
                .build()));
        prepareInitiativeAndProfile();
        when(notificationContentService.resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_KO_INITIATIVE_ENDED),
                any(OnboardingPlaceholderContext.class),
                any()
        )).thenReturn(Optional.of(new NotificationContent("Esito", "L'iniziativa Bonus è terminata")));
        mockSentNotification();

        assertEquals("notification-id", notification.processNotification(evaluation));

        verify(notificationContentService).resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_KO_INITIATIVE_ENDED),
                any(OnboardingPlaceholderContext.class),
                any()
        );
    }

    @Test
    void processNotification_usesGenericKoEventAndRendersManagedEntity() {
        EvaluationDTO evaluation = evaluation(STATUS_ONBOARDING_KO);
        evaluation.setOnboardingRejectionReasons(List.of(OnboardingRejectionReason.builder()
                .type(INVALID_REQUEST)
                .code(FAMILY_CRITERIA_FAIL)
                .authority("Comune di Test")
                .build()));
        prepareInitiativeAndProfile();
        when(notificationContentService.resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_KO_GENERIC),
                any(OnboardingPlaceholderContext.class),
                any()
        )).thenReturn(Optional.of(new NotificationContent("Esito", "Contatta Comune di Test")));
        mockSentNotification();

        assertEquals("notification-id", notification.processNotification(evaluation));

        ArgumentCaptor<NotificationDTO> sent = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(ioBackEndRestConnector).notify(sent.capture(), eq("io-token"));
        assertEquals("Contatta Comune di Test", sent.getValue().getContent().getMarkdown());
    }

    @Test
    void processNotification_skipsUnknownProfileWithoutLookingUpContent() {
        EvaluationDTO evaluation = evaluation(STATUS_ONBOARDING_OK);
        when(initiativeRestConnector.getInitiativeDetailInfo(anyString())).thenReturn(initiative("UNKNOWN"));
        when(notificationContentService.resolveProfileAndFindAndRender(
                eq("UNKNOWN"),
                eq(NotificationEvent.ONBOARDING_OK),
                any(OnboardingPlaceholderContext.class),
                any()
        )).thenReturn(Optional.empty());

        NotFoundNotificationEventTemplate errorResult = assertThrows(NotFoundNotificationEventTemplate.class, () -> notification.processNotification(evaluation));

        assertEquals("Unable to notify user user-id: notification event ONBOARDING_OK was not found for profile UNKNOWN", errorResult.getMessage());

        verify(notificationContentService).resolveProfileAndFindAndRender(
                eq("UNKNOWN"),
                eq(NotificationEvent.ONBOARDING_OK),
                any(OnboardingPlaceholderContext.class),
                any()
        );
        verifyNoInteractions(ioBackEndRestConnector);
    }

    @Test
    void processNotification_skipsWhenEventContentIsMissing() {
        EvaluationDTO evaluation = evaluation(STATUS_ONBOARDING_OK);
        prepareInitiativeAndProfile();
        when(notificationContentService.resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_OK),
                any(OnboardingPlaceholderContext.class),
                any()
        ))
                .thenReturn(Optional.empty());

        NotFoundNotificationEventTemplate errorResult = assertThrows(NotFoundNotificationEventTemplate.class, () -> notification.processNotification(evaluation));

        assertEquals("Unable to notify user user-id: notification event ONBOARDING_OK was not found for profile BEL25", errorResult.getMessage());


        verifyNoInteractions(ioBackEndRestConnector);
    }

    @Test
    void processNotification_whenStatusJoined_success() {
        EvaluationDTO evaluation = evaluation(STATUS_ONBOARDING_JOINED);
        prepareInitiativeAndProfile();
        when(notificationContentService.resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_JOINED),
                any(OnboardingPlaceholderContext.class),
                any()
        )).thenReturn(Optional.of(new NotificationContent("Adesione", "Adesione effettuata con successo")));
        mockSentNotification();

        assertEquals("notification-id", notification.processNotification(evaluation));

        verify(notificationContentService).resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_JOINED),
                any(OnboardingPlaceholderContext.class),
                any()
        );
        ArgumentCaptor<NotificationDTO> sent = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(ioBackEndRestConnector).notify(sent.capture(), eq("io-token"));
        assertEquals("Adesione", sent.getValue().getContent().getSubject());
        assertEquals("Adesione effettuata con successo", sent.getValue().getContent().getMarkdown());
    }

    @Test
    void processNotification_whenStatusJoined_templateNotFound_throwsException() {
        EvaluationDTO evaluation = evaluation(STATUS_ONBOARDING_JOINED);
        prepareInitiativeAndProfile();
        when(notificationContentService.resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_JOINED),
                any(OnboardingPlaceholderContext.class),
                any()
        )).thenReturn(Optional.empty());

        NotFoundNotificationEventTemplate ex = assertThrows(
                NotFoundNotificationEventTemplate.class,
                () -> notification.processNotification(evaluation)
        );

        assertTrue(ex.getMessage().contains("ONBOARDING_JOINED"));
        verifyNoInteractions(ioBackEndRestConnector);
    }

    @Test
    void processNotification_whenStatusOkPartial_success() {
        EvaluationDTO evaluation = evaluation(STATUS_ONBOARDING_OK);
        VerifyDTO verifyDTO = VerifyDTO.builder()
                .verify(true)
                .beneficiaryBudgetCentsMin(25000L)
                .build();
        evaluation.setVerifies(List.of(verifyDTO));
        evaluation.setBeneficiaryBudgetCents(25000L);

        prepareInitiativeAndProfile();
        when(notificationContentService.resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_OK_PARTIAL),
                any(OnboardingPlaceholderContext.class),
                any()
        )).thenReturn(Optional.of(new NotificationContent("Esito Parziale", "Importo parziale assegnato")));
        mockSentNotification();

        assertEquals("notification-id", notification.processNotification(evaluation));

        verify(notificationContentService).resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_OK_PARTIAL),
                any(OnboardingPlaceholderContext.class),
                any()
        );
        ArgumentCaptor<NotificationDTO> sent = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(ioBackEndRestConnector).notify(sent.capture(), eq("io-token"));
        assertEquals("Esito Parziale", sent.getValue().getContent().getSubject());
    }

    @Test
    void processNotification_whenStatusOkPartial_templateNotFound_throwsException() {
        EvaluationDTO evaluation = evaluation(STATUS_ONBOARDING_OK);
        VerifyDTO verifyDTO = VerifyDTO.builder()
                .verify(true)
                .beneficiaryBudgetCentsMin(25000L)
                .build();
        evaluation.setVerifies(List.of(verifyDTO));
        evaluation.setBeneficiaryBudgetCents(25000L);

        prepareInitiativeAndProfile();
        when(notificationContentService.resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_OK_PARTIAL),
                any(OnboardingPlaceholderContext.class),
                any()
        )).thenReturn(Optional.empty());

        NotFoundNotificationEventTemplate ex = assertThrows(
                NotFoundNotificationEventTemplate.class,
                () -> notification.processNotification(evaluation)
        );

        assertTrue(ex.getMessage().contains("ONBOARDING_OK_PARTIAL"));
        verifyNoInteractions(ioBackEndRestConnector);
    }

    @Test
    void processNotification_whenSpecificKoNotFound_fallbackToGenericKo_success() {
        EvaluationDTO evaluation = evaluation(STATUS_ONBOARDING_KO);
        evaluation.setOnboardingRejectionReasons(List.of(OnboardingRejectionReason.builder()
                .type(INVALID_REQUEST)
                .code(REJECTION_REASON_INITIATIVE_ENDED)
                .build()));
        prepareInitiativeAndProfile();

        when(notificationContentService.resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_KO_INITIATIVE_ENDED),
                any(OnboardingPlaceholderContext.class),
                any()
        )).thenReturn(Optional.empty());

        when(notificationContentService.resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_KO_GENERIC),
                any(OnboardingPlaceholderContext.class),
                any()
        )).thenReturn(Optional.of(new NotificationContent("Generico", "Markdown generico fallback")));
        mockSentNotification();

        assertEquals("notification-id", notification.processNotification(evaluation));

        verify(notificationContentService).resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_KO_INITIATIVE_ENDED),
                any(OnboardingPlaceholderContext.class),
                any()
        );
        verify(notificationContentService).resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_KO_GENERIC),
                any(OnboardingPlaceholderContext.class),
                any()
        );
        ArgumentCaptor<NotificationDTO> sent = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(ioBackEndRestConnector).notify(sent.capture(), eq("io-token"));
        assertEquals("Markdown generico fallback", sent.getValue().getContent().getMarkdown());
    }

    @Test
    void processNotification_whenSpecificKoNotFoundAndGenericKoNotFound_throwsException() {
        EvaluationDTO evaluation = evaluation(STATUS_ONBOARDING_KO);
        evaluation.setOnboardingRejectionReasons(List.of(OnboardingRejectionReason.builder()
                .type(INVALID_REQUEST)
                .code(REJECTION_REASON_INITIATIVE_ENDED)
                .build()));
        prepareInitiativeAndProfile();

        when(notificationContentService.resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_KO_INITIATIVE_ENDED),
                any(OnboardingPlaceholderContext.class),
                any()
        )).thenReturn(Optional.empty());

        when(notificationContentService.resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_KO_GENERIC),
                any(OnboardingPlaceholderContext.class),
                any()
        )).thenReturn(Optional.empty());

        NotFoundNotificationEventTemplate ex = assertThrows(
                NotFoundNotificationEventTemplate.class,
                () -> notification.processNotification(evaluation)
        );

        assertTrue(ex.getMessage().contains("ONBOARDING_KO_INITIATIVE_ENDED"));
        verifyNoInteractions(ioBackEndRestConnector);
    }

    @Test
    void processNotification_whenGenericKoDirectlyNotFound_throwsException() {
        EvaluationDTO evaluation = evaluation(STATUS_ONBOARDING_KO);
        evaluation.setOnboardingRejectionReasons(List.of(OnboardingRejectionReason.builder()
                .type(INVALID_REQUEST)
                .code(FAMILY_CRITERIA_FAIL)
                .build()));
        prepareInitiativeAndProfile();

        when(notificationContentService.resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_KO_GENERIC),
                any(OnboardingPlaceholderContext.class),
                any()
        )).thenReturn(Optional.empty());

        NotFoundNotificationEventTemplate ex = assertThrows(
                NotFoundNotificationEventTemplate.class,
                () -> notification.processNotification(evaluation)
        );

        assertTrue(ex.getMessage().contains("ONBOARDING_KO_GENERIC"));
        verifyNoInteractions(ioBackEndRestConnector);
    }

    @Test
    void processNotification_whenProfileVariablesPresent_passesAssistedLinkToContext() {
        EvaluationDTO evaluation = evaluation(STATUS_ONBOARDING_OK);
        prepareInitiativeAndProfile();

        ProfilesNotificationProperties.ProfileConfigVariables configVars =
                new ProfilesNotificationProperties.ProfileConfigVariables();
        configVars.setAssistedLink("https://assistenza.example.it");
        when(profilesNotificationCatalog.retrieveProfileVariables(PROFILE)).thenReturn(Optional.of(configVars));

        when(notificationContentService.resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_OK),
                any(OnboardingPlaceholderContext.class),
                any()
        )).thenReturn(Optional.of(new NotificationContent("Subject", "Body")));
        mockSentNotification();

        notification.processNotification(evaluation);

        ArgumentCaptor<OnboardingPlaceholderContext> contextCaptor =
                ArgumentCaptor.forClass(OnboardingPlaceholderContext.class);
        verify(notificationContentService).resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_OK),
                contextCaptor.capture(),
                any()
        );
        assertEquals("https://assistenza.example.it", contextCaptor.getValue().assistedLink());
    }

    @Test
    void sendNotification_whenIoBackendThrowsFeignException_logsErrorAndReturnsNull() {
        EvaluationDTO evaluation = evaluation(STATUS_ONBOARDING_OK);
        prepareInitiativeAndProfile();

        when(notificationContentService.resolveProfileAndFindAndRender(
                eq(PROFILE),
                eq(NotificationEvent.ONBOARDING_OK),
                any(OnboardingPlaceholderContext.class),
                any()
        )).thenReturn(Optional.of(new NotificationContent("Subject", "Body")));

        Request request = mock(Request.class);
        when(ioBackEndRestConnector.notify(any(NotificationDTO.class), anyString()))
                .thenThrow(new FeignException.InternalServerError("IO Error", request, new byte[0], null));

        String result = notification.processNotification(evaluation);

        assertNull(result);
    }

    @Test
    void processNotification_whenUnsupportedStatus_returnsNull() {
        EvaluationDTO evaluation = evaluation("UNSUPPORTED_STATUS");
        prepareInitiativeAndProfile();

        String result = notification.processNotification(evaluation);

        assertNull(result);
        verifyNoInteractions(ioBackEndRestConnector);
        verifyNoInteractions(notificationContentService);
    }

    @Test
    void processNotification_whenInitiativeFetchThrowsFeignException_returnsNull() {
        EvaluationDTO evaluation = evaluation(STATUS_ONBOARDING_KO);
        Request request = mock(Request.class);
        when(initiativeRestConnector.getInitiativeDetailInfo(evaluation.getInitiativeId()))
                .thenThrow(new FeignException.BadRequest("Bad Request", request, new byte[0], null));

        String result = notification.processNotification(evaluation);

        assertNull(result);
    }

    private void prepareInitiativeAndProfile() {
        when(initiativeRestConnector.getInitiativeDetailInfo(anyString())).thenReturn(initiative(PROFILE));
    }

    private InitiativeNotificationDTO initiative(String profile) {
        InitiativeNotificationDTO initiative = new InitiativeNotificationDTO();
        initiative.setEmailFlux(profile);
        return initiative;
    }

    private void mockSentNotification() {
        NotificationResource sent = mock(NotificationResource.class);
        when(sent.getId()).thenReturn("notification-id");
        when(ioBackEndRestConnector.notify(any(NotificationDTO.class), anyString())).thenReturn(sent);
    }

    private EvaluationDTO evaluation(String status) {
        EvaluationDTO evaluation = new EvaluationDTO();
        evaluation.setUserId("user-id");
        evaluation.setInitiativeId("initiative-id");
        evaluation.setInitiativeName("Bonus");
        evaluation.setStatus(status);
        evaluation.setAdmissibilityCheckDate(LocalDateTime.now());
        evaluation.setBeneficiaryBudgetCents(25000L);
        evaluation.setChannel(Channel.IO);
        evaluation.setFiscalCode("fiscal-code");
        evaluation.setIoToken("io-token");
        return evaluation;
    }
}
