package it.gov.pagopa.notification.manager.service;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;

public interface NotificationManagerService {
  void notify(EvaluationDTO evaluationDTO);
  void addOutcome(EvaluationDTO evaluationDTO);
}
