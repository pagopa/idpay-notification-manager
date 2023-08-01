package it.gov.pagopa.notification.manager.event.consumer;

import it.gov.pagopa.notification.manager.dto.event.CommandOperationQueueDTO;
import it.gov.pagopa.notification.manager.service.NotificationManagerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class CommandsConsumer {

  @Bean
  public Consumer<CommandOperationQueueDTO> commandsConsumer(
          NotificationManagerService notificationManagerService) {
    return notificationManagerService::processNotification;
  }

}
