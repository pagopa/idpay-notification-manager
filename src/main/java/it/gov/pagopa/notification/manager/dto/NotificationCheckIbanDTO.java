package it.gov.pagopa.notification.manager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class NotificationCheckIbanDTO {
  private String initiativeId;
  private String notificationId;
  @JsonProperty("fiscal_code")
  @NotNull
  @NotBlank
  private String fiscalCode;
  private String initiativeToken;
  private MessageContent content;

}
