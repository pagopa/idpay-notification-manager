package it.gov.pagopa.notification.manager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class MessageContent {

  @NotNull
  @NotBlank
  @Length(min = 10, max = 120)
  private String subject;

  @NotNull
  @NotBlank
  @Length(min = 80, max = 10000)
  private String markdown;

}
