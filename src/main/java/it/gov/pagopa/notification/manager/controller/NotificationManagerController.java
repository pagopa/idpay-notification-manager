package it.gov.pagopa.notification.manager.controller;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/idpay/notifications")
public interface NotificationManagerController {

  /**
   * Force the recover process to be executed
   */
  @GetMapping("/recover/start")
  void forceRecoverScheduling();

  /**
   * Add a new operation to the Outcomes Queue
   */
  @PutMapping("/")
  ResponseEntity<Void> addNotification(@RequestBody EvaluationDTO body);

  @PutMapping("/notify")
  ResponseEntity<Void> notify(@RequestBody EvaluationDTO body);

}
