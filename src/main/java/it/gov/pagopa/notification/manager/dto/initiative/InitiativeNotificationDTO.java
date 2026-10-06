package it.gov.pagopa.notification.manager.dto.initiative;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * InitiativeNotificationDTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitiativeNotificationDTO {

    @JsonProperty("initiativeId")
    private String initiativeId;
    @JsonProperty("emailFlux")
    private String emailFlux;
}
