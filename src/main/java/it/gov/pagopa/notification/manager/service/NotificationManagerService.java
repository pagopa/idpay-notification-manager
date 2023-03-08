package it.gov.pagopa.notification.manager.service;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.event.AnyOfNotificationQueueDTO;
import it.gov.pagopa.notification.manager.model.Notification;

public interface NotificationManagerService {
  void notify(EvaluationDTO evaluationDTO);
  void notify(Notification notification);
  void addOutcome(EvaluationDTO evaluationDTO);
  void sendNotificationFromOperationType(AnyOfNotificationQueueDTO notificationQueueDTO);
  void recoverKoNotifications();

}
