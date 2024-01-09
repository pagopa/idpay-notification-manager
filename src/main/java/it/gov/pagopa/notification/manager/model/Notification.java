package it.gov.pagopa.notification.manager.model;

import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder(toBuilder = true)
@FieldNameConstants
@Document(collection = "notification")
public class Notification {

  @Id
  private String id;

  private String notificationId;

  private String notificationStatus;

  private LocalDateTime notificationDate;

  private LocalDateTime statusKoTimestamp;

  private String operationType;

  private String userId;

  private String initiativeId;

  private String initiativeName;

  private String authority;

  private String onboardingOutcome;

  private List<OnboardingRejectionReason> rejectReasons;

  private LocalDateTime retryDate;

  private Integer retry;

  private BigDecimal refundReward;

  private String refundStatus;
}
