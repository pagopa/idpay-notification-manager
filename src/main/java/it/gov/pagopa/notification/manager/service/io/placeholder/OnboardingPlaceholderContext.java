package it.gov.pagopa.notification.manager.service.io.placeholder;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import it.gov.pagopa.notification.manager.model.Notification;

import java.util.List;

public record OnboardingPlaceholderContext(String initiativeId, String initiativeName, Long beneficiaryBudgetCents,
                                           List<OnboardingRejectionReason> onboardingRejectionReasons, String assistedLink) {

    public static OnboardingPlaceholderContext from(EvaluationDTO evaluation, String assistedLink) {
        return new OnboardingPlaceholderContext(evaluation.getInitiativeId(),
                evaluation.getInitiativeName(),
                evaluation.getBeneficiaryBudgetCents(),
                evaluation.getOnboardingRejectionReasons(),
                assistedLink);
    }

    public static OnboardingPlaceholderContext from(Notification notification, String assistedLink) {
        return new OnboardingPlaceholderContext(notification.getInitiativeId(),
                notification.getInitiativeName(),
                notification.getBeneficiaryBudgetCents(),
                notification.getRejectReasons(),
                assistedLink);
    }
}
