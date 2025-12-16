package it.gov.pagopa.notification.manager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
@JsonInclude(Include.NON_NULL)
public class ManualNotificationDTO {



  private String userId;
  private String initiativeId;

  @NotNull
  private MessageContent content;

  private Map<String, String> bodyValues;
}
