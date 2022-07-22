package it.gov.pagopa.notification.manager.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document(collection = "notification")
public class Notification {

  @Id
  private String notificationId;

  private String userId;

  private String initiativeId;

  private String notificationStatus;

  private LocalDateTime notificationDate;

}
