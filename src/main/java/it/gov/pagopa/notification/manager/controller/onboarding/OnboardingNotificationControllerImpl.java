package it.gov.pagopa.notification.manager.controller.onboarding;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.service.NotificationManagerService;
import it.gov.pagopa.notification.manager.service.onboarding.BaseOnboardingNotification;
import it.gov.pagopa.notification.manager.service.onboarding.OnboardingWebNotification;
import it.gov.pagopa.notification.manager.service.onboarding.OnboardingWebNotificationImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class OnboardingNotificationControllerImpl implements OnboardingNotificationController {

  @Autowired
  private final OnboardingWebNotificationImpl onboardingWebNotification;

  public OnboardingNotificationControllerImpl(
          OnboardingWebNotificationImpl onboardingWebNotification) {
    this.onboardingWebNotification = onboardingWebNotification;
  }

  @Override
  public ResponseEntity<Void> processNotification(EvaluationDTO body) {
    onboardingWebNotification.processNotification(body);
    return new ResponseEntity<>(HttpStatus.OK);
  }


  
}
