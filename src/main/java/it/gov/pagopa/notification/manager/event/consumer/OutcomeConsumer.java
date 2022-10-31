package it.gov.pagopa.notification.manager.event.consumer;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.service.NotificationManagerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class OutcomeConsumer {

  @Bean
  public Consumer<EvaluationDTO> onboardingConsumer(
      NotificationManagerService notificationManagerService) {
    return notificationManagerService::notify;
  }

}
