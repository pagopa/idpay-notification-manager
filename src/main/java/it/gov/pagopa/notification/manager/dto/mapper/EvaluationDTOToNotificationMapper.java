package it.gov.pagopa.notification.manager.dto.mapper;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.model.Notification;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class EvaluationDTOToNotificationMapper {
  public Notification map(EvaluationDTO evaluationDTO) {
    return Notification.builder()
        .notificationDate(LocalDateTime.now())
        .initiativeId(evaluationDTO.getInitiativeId())
        .userId(evaluationDTO.getUserId())
        .onboardingOutcome(evaluationDTO.getStatus())
        .rejectReasons(evaluationDTO.getOnboardingRejectionReasons())
        .build();
  }
}
