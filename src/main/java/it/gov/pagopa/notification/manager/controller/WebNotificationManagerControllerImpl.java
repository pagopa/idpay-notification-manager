package it.gov.pagopa.notification.manager.controller;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationReminderQueueDTO;
import it.gov.pagopa.notification.manager.service.NotificationManagerService;
import it.gov.pagopa.notification.manager.service.WebNotificationManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class WebNotificationManagerControllerImpl implements WebNotificationManagerController {

  private final WebNotificationManagerService webNotificationManagerService;

  public WebNotificationManagerControllerImpl(
          WebNotificationManagerService webNotificationManagerService) {
    this.webNotificationManagerService = webNotificationManagerService;
  }

  @Override
  public ResponseEntity<Void> sendReminderMail(NotificationReminderQueueDTO body) {
    webNotificationManagerService.sendReminderMail(body);
    return new ResponseEntity<>(HttpStatus.OK);
  }


  
}
