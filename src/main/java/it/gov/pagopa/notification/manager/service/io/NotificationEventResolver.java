package it.gov.pagopa.notification.manager.service.io;

import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import it.gov.pagopa.notification.manager.dto.VerifyDTO;
import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.Notification;

import java.util.List;
import java.util.Optional;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.*;

public class NotificationEventResolver {

    private static final String REFUND_STATUS_ACCEPTED = "ACCEPTED";

    public static NotificationEvent resolveRefundEvent(String status) {
        return REFUND_STATUS_ACCEPTED.equals(status)
                ? NotificationEvent.REFUND_OK : NotificationEvent.REFUND_KO;
    }

    public static Optional<NotificationEvent> resolveOnboardingEvent(Notification notification) {
        if (notification == null || notification.getOnboardingOutcome() == null) {
            throw new IllegalArgumentException("An onboarding notification with an outcome is required");
        }

        String outcome = notification.getOnboardingOutcome();
        switch (outcome) {
            case STATUS_ONBOARDING_OK -> {
                return Optional.ofNullable(resolveOnboardingStatusOkEvent(notification));
            }
            case STATUS_ONBOARDING_JOINED -> {
                return Optional.of(NotificationEvent.ONBOARDING_JOINED);
            }
            case STATUS_ONBOARDING_WAITING_LIST -> {
                return Optional.of(NotificationEvent.ONBOARDING_WAITING_LIST);
            }
            case STATUS_ONBOARDING_DEMANDED -> {
                return Optional.of(NotificationEvent.ONBOARDING_DEMANDED);
            }
            case STATUS_ONBOARDING_KO -> {
                return Optional.of(resolveOnboardingStatusKoEvent(notification));
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    private static NotificationEvent resolveOnboardingStatusOkEvent(Notification notification) {
        return isOnboardingOkPartial(notification.getVerifies(), notification.getBeneficiaryBudgetCents())
                ? NotificationEvent.ONBOARDING_OK_PARTIAL : NotificationEvent.ONBOARDING_OK;
    }

    public static boolean isOnboardingOkPartial(List<VerifyDTO> verifies, Long  beneficiaryBudgetCents) {
        boolean isPartial = Boolean.FALSE;
        if(verifies != null && !verifies.isEmpty()){
            for(VerifyDTO verify : verifies){
                // Partial outcome only when an ISEE verification was actually performed
                // and the awarded budget dropped to the lower bracket (ISEE difforme).
                if(verify.isVerify()
                        && beneficiaryBudgetCents != null
                        && verify.getBeneficiaryBudgetCentsMin() == beneficiaryBudgetCents){
                    isPartial = Boolean.TRUE;
                    break;
                }
            }
        }
        return isPartial;
    }

    private static NotificationEvent resolveOnboardingStatusKoEvent(Notification notification) {
        return resolveOnboardingStatusKoEventByRejectionReasons(notification.getRejectReasons());
    }

    public static NotificationEvent resolveOnboardingStatusKoEventByRejectionReasons(List<OnboardingRejectionReason> rejectReasons) {
        if (rejectReasons == null || rejectReasons.isEmpty()
                || rejectReasons.getFirst() == null || rejectReasons.getFirst().getCode() == null) {
            return NotificationEvent.ONBOARDING_KO_GENERIC;
        }

        NotificationEvent event = rejectReasons.getFirst().getCode().getNotificationEvent();

        return event != null ? event : NotificationEvent.ONBOARDING_KO_GENERIC;
    }
}
