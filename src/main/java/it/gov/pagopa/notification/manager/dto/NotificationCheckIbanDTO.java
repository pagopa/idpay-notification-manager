package it.gov.pagopa.notification.manager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class NotificationCheckIbanDTO {
  private String notificationId;
  @JsonProperty("fiscal_code")
  @NotNull
  @NotBlank
  private String fiscalCode;
  @JsonProperty("primary_key")
  @NotNull
  @NotBlank
  private String initiativeToken;
  private MessageContent content;
  private LocalDateTime notificationDate;

}
