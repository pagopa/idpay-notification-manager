package it.gov.pagopa.notification.manager.service.io;

import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode;
import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Optional;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.*;
import static it.gov.pagopa.notification.manager.service.io.NotificationEventResolver.resolveOnboardingEvent;
import static it.gov.pagopa.notification.manager.service.io.NotificationEventResolver.resolveRefundEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationEventResolverTest {

    @Test
    void resolve_usesRefundOkOnlyForAcceptedStatus() {
        assertEquals(NotificationEvent.REFUND_OK, resolveRefundEvent("ACCEPTED"));
        assertEquals(NotificationEvent.REFUND_KO, resolveRefundEvent("REJECTED"));
        assertEquals(NotificationEvent.REFUND_KO, resolveRefundEvent("accepted"));
        assertEquals(NotificationEvent.REFUND_KO, resolveRefundEvent(null));
    }

    @Test
    void resolveOnboardingEvent_mapsNonKoOutcomesWithoutRejectionReasons() {
        assertEquals(Optional.of(NotificationEvent.ONBOARDING_OK), resolveOnboardingEvent(
                Notification.builder().onboardingOutcome(STATUS_ONBOARDING_OK).build()));
        assertEquals(Optional.of(NotificationEvent.ONBOARDING_JOINED), resolveOnboardingEvent(
                Notification.builder().onboardingOutcome(STATUS_ONBOARDING_JOINED).build()));
        assertEquals(Optional.of(NotificationEvent.ONBOARDING_WAITING_LIST), resolveOnboardingEvent(
                Notification.builder().onboardingOutcome(STATUS_ONBOARDING_WAITING_LIST).build()));
        assertEquals(Optional.of(NotificationEvent.ONBOARDING_DEMANDED), resolveOnboardingEvent(
                Notification.builder().onboardingOutcome(STATUS_ONBOARDING_DEMANDED).build()));
    }

    @Test
    void resolveOnboardingEvent_returnsEmptyForUnsupportedOutcome() {
        assertEquals(Optional.empty(), resolveOnboardingEvent(
                Notification.builder().onboardingOutcome("UNKNOWN").build()));
    }

    @ParameterizedTest
    @EnumSource(OnboardingRejectionReasonCode.class)
    void resolveOnboardingEvent_mapsAllRejectionCodes(OnboardingRejectionReasonCode code) {
        NotificationEvent expected = switch (code) {
            case ISEE_TYPE_FAIL, RESIDENCE_FAIL, BIRTHDATE_FAIL, FAMILY_FAIL ->
                    NotificationEvent.ONBOARDING_KO_PDND;
            case CITIZEN_OUT_OF_RANKING -> NotificationEvent.ONBOARDING_KO_RANKING;
            case INITIATIVE_BUDGET_EXHAUSTED -> NotificationEvent.ONBOARDING_KO_BUDGET;
            case REJECTION_REASON_INITIATIVE_ENDED -> NotificationEvent.ONBOARDING_KO_INITIATIVE_ENDED;
            case AUTOMATED_CRITERIA_ISEE_FAIL, AUTOMATED_CRITERIA_BIRTHDATE_FAIL,
                 AUTOMATED_CRITERIA_RESIDENCE_FAIL, CONSENSUS_CHECK_TC_FAIL,
                 CONSENSUS_CHECK_PDND_FAIL, CONSENSUS_CHECK_TC_ACCEPT_FAIL,
                 CONSENSUS_CHECK_CRITERIA_CONSENSUS_FAIL, INVALID_INITIATIVE_ID ->
                    NotificationEvent.ONBOARDING_KO_REJECTED_NO_RETRY;
            case FAMILY_CRITERIA_FAIL, RULE_ENGINE_NOT_READY, GENERIC_ERROR ->
                    NotificationEvent.ONBOARDING_KO_GENERIC;
        };
        Notification notification = Notification.builder()
                .onboardingOutcome(STATUS_ONBOARDING_KO)
                .rejectReasons(List.of(OnboardingRejectionReason.builder().code(code).build()))
                .build();

        assertEquals(Optional.of(expected), resolveOnboardingEvent(notification));
    }

    @Test
    void resolveOnboardingEvent_usesOnlyFirstReasonForEventSelection() {
        Notification notification = Notification.builder()
                .onboardingOutcome(STATUS_ONBOARDING_KO)
                .rejectReasons(List.of(
                        OnboardingRejectionReason.builder().code(OnboardingRejectionReason.OnboardingRejectionReasonCode.INITIATIVE_BUDGET_EXHAUSTED).build(),
                        OnboardingRejectionReason.builder().code(OnboardingRejectionReason.OnboardingRejectionReasonCode.ISEE_TYPE_FAIL).build()))
                .build();

        assertEquals(Optional.of(NotificationEvent.ONBOARDING_KO_BUDGET), resolveOnboardingEvent(notification));
    }

    @Test
    void resolveOnboardingEvent_rejectsMissingInputs() {
        assertThrows(IllegalArgumentException.class, () -> resolveOnboardingEvent(null));
        assertThrows(IllegalArgumentException.class, () -> resolveOnboardingEvent(Notification.builder().build()));
        Notification notification = Notification.builder().onboardingOutcome(STATUS_ONBOARDING_KO).build();
        assertEquals(Optional.of(NotificationEvent.ONBOARDING_KO_GENERIC), resolveOnboardingEvent(notification));
        notification.setRejectReasons(List.of(OnboardingRejectionReason.builder().build()));
        assertEquals(Optional.of(NotificationEvent.ONBOARDING_KO_GENERIC), resolveOnboardingEvent(notification));
    }
}
