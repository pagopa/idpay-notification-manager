package it.gov.pagopa.notification.manager.service.io.placeholder;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;

import java.util.Objects;

public record OnboardingPlaceholderContext(EvaluationDTO evaluation, String assistedLink) {

    public OnboardingPlaceholderContext {
        Objects.requireNonNull(evaluation, "Onboarding evaluation is required");
    }
}
