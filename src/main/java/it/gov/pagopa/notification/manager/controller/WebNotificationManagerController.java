package it.gov.pagopa.notification.manager.controller;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationReminderQueueDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/idpay/notifications")
public interface WebNotificationManagerController {


  @PutMapping("/sendReminderMail")
  ResponseEntity<Void> sendReminderMail(@RequestBody NotificationReminderQueueDTO body);


}
