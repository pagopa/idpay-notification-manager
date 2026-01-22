package it.gov.pagopa.notification.manager.event.consumer;

import it.gov.pagopa.notification.manager.dto.event.AnyOfNotificationQueueDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationReminderQueueDTO;
import it.gov.pagopa.notification.manager.enums.Channel;
import it.gov.pagopa.notification.manager.service.NotificationManagerService;
import it.gov.pagopa.notification.manager.service.WebNotificationManagerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class NotificationConsumer {

    @Bean
    public Consumer<AnyOfNotificationQueueDTO> anyNotificationConsumer(
            NotificationManagerService notificationManagerService,
            WebNotificationManagerService webNotificationManagerService) {
        return dto -> {
            if (dto instanceof NotificationReminderQueueDTO notificationReminderQueueDTO
                    && notificationReminderQueueDTO.getChannel().isWeb()) {
                webNotificationManagerService.sendReminderMail(notificationReminderQueueDTO);
                notificationReminderQueueDTO.setChannel(Channel.IO);
                notificationManagerService.sendNotificationFromOperationType(dto);
            } else {
                notificationManagerService.sendNotificationFromOperationType(dto);
            }
        };
    }
}
