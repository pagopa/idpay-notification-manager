package it.gov.pagopa.notification.manager.dto.mapper;

import it.gov.pagopa.notification.manager.dto.NotificationQueueDTO;
import it.gov.pagopa.notification.manager.model.Notification;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class NotificationQueueDTOToNotificationMapper {

  public Notification map(NotificationQueueDTO notificationQueueDTO) {
    // da vedere se servono altri campi come l'iban
    return Notification.builder()
        .notificationCheckIbanDate(LocalDateTime.now())
        .initiativeId(notificationQueueDTO.getInitiativeId())
        .serviceId(notificationQueueDTO.getServiceId())
        .userId(notificationQueueDTO.getUserId())
        .operationType(notificationQueueDTO.getOperationType())
        .build();
  }

}
