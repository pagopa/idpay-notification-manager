package it.gov.pagopa.notification.manager.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationQueueDTO implements AnyOfNotificationQueueDTO{

  @NotNull
  private String operationType;
  private String userId;
  private String initiativeId;
  private String serviceId;

}
