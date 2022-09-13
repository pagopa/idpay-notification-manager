package it.gov.pagopa.notification.manager.model;

import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document(collection = "notification")
public class Notification {

  @Id
  private String id;

  private String notificationId;

  private String notificationStatus;

  private LocalDateTime notificationDate;

  private String userId;

  private String initiativeId;

  private String authority;

  private String onboardingOutcome;

  private List<OnboardingRejectionReason> rejectReasons;

}
