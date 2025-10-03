package it.gov.pagopa.notification.manager.dto;

import it.gov.pagopa.notification.manager.enums.Channel;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EvaluationDTO {

  @NotEmpty
  private String userId;
  @NotEmpty
  private String initiativeId;
  private String initiativeName;
  private LocalDate initiativeEndDate;
  private String organizationId;
  private String organizationName;
  @NotEmpty
  private String status;
  @NotNull
  private LocalDateTime admissibilityCheckDate;
  private LocalDateTime criteriaConsensusTimestamp;
  private List<OnboardingRejectionReason> onboardingRejectionReasons;
  private Long beneficiaryBudgetCents;
  private Long rankingValue;
  private Boolean verifyIsee;
  private String userMail;
  private Channel channel;

  // Info filled during processing
  private String fiscalCode;
  private String ioToken;
}