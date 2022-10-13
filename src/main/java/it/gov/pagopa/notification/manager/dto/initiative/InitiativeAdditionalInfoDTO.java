package it.gov.pagopa.notification.manager.dto.initiative;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * InitiativeAdditionalDTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitiativeAdditionalInfoDTO {
    private String primaryTokenIO;
    private String secondaryTokenIO;
}
