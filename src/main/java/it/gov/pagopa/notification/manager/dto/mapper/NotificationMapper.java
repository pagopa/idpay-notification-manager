package it.gov.pagopa.notification.manager.dto.mapper;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationQueueDTO;
import it.gov.pagopa.notification.manager.model.Notification;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

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
        .notificationCheckIbanDate(LocalDateTime.now())
        .initiativeId(notificationQueueDTO.getInitiativeId())
        .serviceId(notificationQueueDTO.getServiceId())
        .userId(notificationQueueDTO.getUserId())
        .operationType(notificationQueueDTO.getOperationType())
        .build();
  }
}
