package it.gov.pagopa.notification.manager.controller;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/idpay/notifications")
public interface NotificationManagerController {


  /**
   * Add a new operation to the Outcomes Queue
   *
   * @param body
   * @return
   */
  @PutMapping("/")
  ResponseEntity<Void> addNotification(@RequestBody EvaluationDTO body);

}
