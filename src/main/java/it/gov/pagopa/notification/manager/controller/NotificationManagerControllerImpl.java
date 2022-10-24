package it.gov.pagopa.notification.manager.controller;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.service.NotificationManagerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationManagerControllerImpl implements NotificationManagerController {

  private final NotificationManagerService notificationManagerService;

  public NotificationManagerControllerImpl(
      NotificationManagerService notificationManagerService) {
    this.notificationManagerService = notificationManagerService;
  }

  @Override
  public ResponseEntity<Void> addNotification(EvaluationDTO body) {
    notificationManagerService.addOutcome(body);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
