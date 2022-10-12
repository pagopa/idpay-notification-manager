package it.gov.pagopa.notification.manager.event.consumer;

import it.gov.pagopa.notification.manager.dto.event.AnyOfNotificationQueueDTO;
import it.gov.pagopa.notification.manager.service.NotificationManagerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class NotificationConsumer {

  @Bean
  public Consumer<AnyOfNotificationQueueDTO> anyNotificationConsumer(
      NotificationManagerService notificationManagerService) {
    return notificationQueueDTO -> notificationManagerService.sendNotificationFromOperationType(notificationQueueDTO);
  }

}
