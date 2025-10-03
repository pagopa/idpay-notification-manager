package it.gov.pagopa.notification.manager.service.onboarding;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;

public interface OnboardingIoNotification {
    String processNotification(EvaluationDTO evaluationDTO);
}
