package it.gov.pagopa.notification.manager.event.consumer;

import it.gov.pagopa.notification.manager.dto.NotificationQueueDTO;
import it.gov.pagopa.notification.manager.service.NotificationManagerService;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationCheckIbanConsumer {

  @Bean
  public Consumer<NotificationQueueDTO> walletConsumer(
      NotificationManagerService notificationManagerService) {
    return notificationManagerService::checkIbanKo;
  }

}
