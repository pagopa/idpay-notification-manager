package it.gov.pagopa.notification.manager.controller.onboarding;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/idpay/notifications")
public interface OnboardingNotificationController {

  /**
   * Add a new operation to the Outcomes Queue
   */
  @PutMapping("/processWebNotification")
  ResponseEntity<Void> processWebNotification(@RequestBody EvaluationDTO body);

  @PutMapping("/processIoNotification")
  ResponseEntity<Void> processIoNotification(@RequestBody EvaluationDTO body);

}
