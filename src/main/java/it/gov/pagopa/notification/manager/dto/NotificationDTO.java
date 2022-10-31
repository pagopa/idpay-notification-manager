package it.gov.pagopa.notification.manager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@JsonInclude(Include.NON_NULL)
public class NotificationDTO {

  @Min(value = 3600)
  @Max(value = 604800)
  @JsonProperty("time_to_live")
  private Long timeToLive;

  @JsonProperty("fiscal_code")
  @NotNull
  @NotBlank
  private String fiscalCode;

  @NotNull private MessageContent content;
}
