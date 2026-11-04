package it.gov.pagopa.notification.manager.service.onboarding;

import it.gov.pagopa.notification.manager.connector.IOBackEndRestConnector;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationResource;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeNotificationDTO;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationDTOMapper;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationMapper;
import it.gov.pagopa.notification.manager.enums.Channel;
import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.NotificationContent;
import it.gov.pagopa.notification.manager.repository.NotificationManagerRepository;
import it.gov.pagopa.notification.manager.service.io.IoNotificationContentService;
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

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.STATUS_ONBOARDING_KO;
import static it.gov.pagopa.notification.manager.constants.NotificationConstants.STATUS_ONBOARDING_OK;
import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode.FAMILY_CRITERIA_FAIL;
import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode.REJECTION_REASON_INITIATIVE_ENDED;
import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private IOBackEndRestConnector ioBackEndRestConnector;
    @Mock
    private NotificationManagerRepository notificationManagerRepository;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private InitiativeRestConnector initiativeRestConnector;

    private OnboardingIoNotification notification;

    @BeforeEach
    void setUp() {
        notification = new OnboardingIoNotificationImpl(
                notificationContentService,
                new NotificationDTOMapper(),
                ioBackEndRestConnector,
                notificationManagerRepository,
                notificationMapper,
                initiativeRestConnector,
                3600L,
                "https://assistance.example"
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

        assertNull(notification.processNotification(evaluation));

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

        assertNull(notification.processNotification(evaluation));

        verifyNoInteractions(ioBackEndRestConnector);
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
