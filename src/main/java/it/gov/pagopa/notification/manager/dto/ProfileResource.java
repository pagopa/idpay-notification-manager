package it.gov.pagopa.notification.manager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class ProfileResource {

  @JsonProperty("sender_allowed")
  boolean senderAllowed;

  @JsonProperty("preferred_languages")
  List<String> preferredLanguages;

}
