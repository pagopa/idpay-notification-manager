package it.gov.pagopa.notification.manager.dto.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRefundQueueDTO extends NotificationQueueDTO {
  private String rewardNotificationId;
  private Long refundReward;
  private String rejectionCode;
  private String rejectionReason;
  private LocalDateTime refundDate;
  private Long refundFeedbackProgressive;
  private String refundCro;
  private String status;
}
