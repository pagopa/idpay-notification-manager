package it.gov.pagopa.notification.manager.service.io;

import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonType;
import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationEventResolverTest {

    private final NotificationEventResolver resolver = new NotificationEventResolver();

    @Test
    void resolve_usesRefundOkOnlyForAcceptedStatus() {
        assertEquals(NotificationEvent.REFUND_OK, resolver.resolveRefundEvent("ACCEPTED"));
        assertEquals(NotificationEvent.REFUND_KO, resolver.resolveRefundEvent("REJECTED"));
        assertEquals(NotificationEvent.REFUND_KO, resolver.resolveRefundEvent("accepted"));
        assertEquals(NotificationEvent.REFUND_KO, resolver.resolveRefundEvent(null));
    }

    @Test
    void resolveOnboardingEvent_mapsNonKoOutcomesWithoutRejectionReasons() {
        assertEquals(NotificationEvent.ONBOARDING_OK, resolver.resolveOnboardingEvent(
                Notification.builder().onboardingOutcome(STATUS_ONBOARDING_OK).build()));
        assertEquals(NotificationEvent.ONBOARDING_JOINED, resolver.resolveOnboardingEvent(
                Notification.builder().onboardingOutcome(STATUS_ONBOARDING_JOINED).build()));
        assertEquals(NotificationEvent.ONBOARDING_WAITING_LIST, resolver.resolveOnboardingEvent(
                Notification.builder().onboardingOutcome(STATUS_ONBOARDING_WAITING_LIST).build()));
    }

    @ParameterizedTest
    @EnumSource(OnboardingRejectionReasonType.class)
    void resolveOnboardingEvent_mapsAllLegacyRejectionTypes(OnboardingRejectionReasonType type) {
        NotificationEvent expected = switch (type) {
            case ISEE_TYPE_KO, BIRTHDATE_KO, RESIDENCE_KO, FAMILY_KO -> NotificationEvent.ONBOARDING_KO_PDND;
            case OUT_OF_RANKING -> NotificationEvent.ONBOARDING_KO_RANKING;
            case BUDGET_EXHAUSTED -> NotificationEvent.ONBOARDING_KO_BUDGET;
            case AUTOMATED_CRITERIA_FAIL, CONSENSUS_MISSED, INVALID_REQUEST ->
                    NotificationEvent.ONBOARDING_KO_REJECTED_NO_RETRY;
            default -> NotificationEvent.ONBOARDING_KO_TECHNICAL;
        };
        Notification notification = Notification.builder()
                .onboardingOutcome(STATUS_ONBOARDING_KO)
                .rejectReasons(List.of(OnboardingRejectionReason.builder().type(type).build()))
                .build();

        assertEquals(expected, resolver.resolveOnboardingEvent(notification));
    }

    @Test
    void resolveOnboardingEvent_usesOnlyFirstReasonForEventSelection() {
        Notification notification = Notification.builder()
                .onboardingOutcome(STATUS_ONBOARDING_KO)
                .rejectReasons(List.of(
                        OnboardingRejectionReason.builder().type(OnboardingRejectionReasonType.BUDGET_EXHAUSTED).build(),
                        OnboardingRejectionReason.builder().type(OnboardingRejectionReasonType.ISEE_TYPE_KO).build()))
                .build();

        assertEquals(NotificationEvent.ONBOARDING_KO_BUDGET, resolver.resolveOnboardingEvent(notification));
    }

    @Test
    void resolveOnboardingEvent_rejectsMissingInputs() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolveOnboardingEvent(null));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolveOnboardingEvent(Notification.builder().build()));
        Notification notification = Notification.builder().onboardingOutcome(STATUS_ONBOARDING_KO).build();
        assertThrows(IllegalArgumentException.class, () -> resolver.resolveOnboardingEvent(notification));
        notification.setRejectReasons(List.of());
        assertThrows(IllegalArgumentException.class, () -> resolver.resolveOnboardingEvent(notification));
        notification.setRejectReasons(List.of(OnboardingRejectionReason.builder().build()));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolveOnboardingEvent(notification));
    }
}
