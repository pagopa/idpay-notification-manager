package it.gov.pagopa.notification.manager.service.onboarding;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.model.Notification;

public interface OnboardingWebNotification {
    String processNotification(EvaluationDTO evaluationDTO);

    boolean notify(Notification notification);
}
