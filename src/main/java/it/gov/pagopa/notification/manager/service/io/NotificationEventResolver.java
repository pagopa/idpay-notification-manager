package it.gov.pagopa.notification.manager.service.io;

import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.Notification;
import org.springframework.stereotype.Service;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.STATUS_ONBOARDING_JOINED;
import static it.gov.pagopa.notification.manager.constants.NotificationConstants.STATUS_ONBOARDING_OK;
import static it.gov.pagopa.notification.manager.constants.NotificationConstants.STATUS_ONBOARDING_WAITING_LIST;

@Service
public class NotificationEventResolver {

    private static final String REFUND_STATUS_ACCEPTED = "ACCEPTED";

    public NotificationEvent resolveRefundEvent(String status) {
        return REFUND_STATUS_ACCEPTED.equals(status)
                ? NotificationEvent.REFUND_OK : NotificationEvent.REFUND_KO;
    }

    public NotificationEvent resolveOnboardingEvent(Notification notification) {
        if (notification == null || notification.getOnboardingOutcome() == null) {
            throw new IllegalArgumentException("An onboarding notification with an outcome is required");
        }
        String outcome = notification.getOnboardingOutcome();
        if (STATUS_ONBOARDING_OK.equals(outcome)) {
            return NotificationEvent.ONBOARDING_OK;
        }
        if (STATUS_ONBOARDING_JOINED.equals(outcome)) {
            return NotificationEvent.ONBOARDING_JOINED;
        }
        if (STATUS_ONBOARDING_WAITING_LIST.equals(outcome)) {
            return NotificationEvent.ONBOARDING_WAITING_LIST;
        }

        var reasons = notification.getRejectReasons();
        if (reasons == null || reasons.isEmpty()
                || reasons.getFirst() == null || reasons.getFirst().getType() == null) {
            throw new IllegalArgumentException("A first rejection reason with a type is required for onboarding KO");
        }
        return switch (reasons.getFirst().getType()) {
            case ISEE_TYPE_KO, BIRTHDATE_KO, RESIDENCE_KO, FAMILY_KO ->
                    NotificationEvent.ONBOARDING_KO_PDND;
            case OUT_OF_RANKING -> NotificationEvent.ONBOARDING_KO_RANKING;
            case BUDGET_EXHAUSTED -> NotificationEvent.ONBOARDING_KO_BUDGET;
            case AUTOMATED_CRITERIA_FAIL, CONSENSUS_MISSED, INVALID_REQUEST ->
                    NotificationEvent.ONBOARDING_KO_REJECTED_NO_RETRY;
            default -> NotificationEvent.ONBOARDING_KO_TECHNICAL;
        };
    }
}
