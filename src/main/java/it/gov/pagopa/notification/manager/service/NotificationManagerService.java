package it.gov.pagopa.notification.manager.service;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.event.AnyOfNotificationQueueDTO;

public interface NotificationManagerService {
  void notify(EvaluationDTO evaluationDTO);
  void addOutcome(EvaluationDTO evaluationDTO);
  void sendNotificationFromOperationType(AnyOfNotificationQueueDTO notificationQueueDTO);

}
