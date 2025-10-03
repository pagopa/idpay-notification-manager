package it.gov.pagopa.notification.manager.service.onboarding;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;

public interface OnboardingWebNotification {
    String processNotification(EvaluationDTO evaluationDTO);
}
