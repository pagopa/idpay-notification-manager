package it.gov.pagopa.notification.manager.dto.mapper;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.event.AnyOfNotificationQueueDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationQueueDTO;
import it.gov.pagopa.notification.manager.model.Notification;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationMapper {
  public Notification evaluationToNotification(EvaluationDTO evaluationDTO) {
    return Notification.builder()
        .notificationDate(LocalDateTime.now())
        .initiativeId(evaluationDTO.getInitiativeId())
        .userId(evaluationDTO.getUserId())
        .onboardingOutcome(evaluationDTO.getStatus())
        .operationType("ONBOARDING")
        .rejectReasons(evaluationDTO.getOnboardingRejectionReasons())
        .build();
  }

  public Notification queueToNotification(NotificationQueueDTO notificationQueueDTO) {
    return Notification.builder()
        .notificationDate(LocalDateTime.now())
        .initiativeId(notificationQueueDTO.getInitiativeId())
        .serviceId(notificationQueueDTO.getServiceId())
        .userId(notificationQueueDTO.getUserId())
        .operationType(notificationQueueDTO.getOperationType())
        .build();
  }

  public Notification toEntity(AnyOfNotificationQueueDTO anyOfNotificationQueueDTO){
    Notification notification = Notification.builder().build();
    BeanUtils.copyProperties(anyOfNotificationQueueDTO, notification);
    return notification;
  }

}
