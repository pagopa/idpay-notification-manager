package it.gov.pagopa.notification.manager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
