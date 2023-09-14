package it.gov.pagopa.notification.manager.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
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
  @NotEmpty
  private String status;
  @NotNull
  private LocalDateTime admissibilityCheckDate;
  private LocalDateTime criteriaConsensusTimestamp;
  private List<OnboardingRejectionReason> onboardingRejectionReasons;
  private BigDecimal beneficiaryBudget;
  private Long rankingValue;
}