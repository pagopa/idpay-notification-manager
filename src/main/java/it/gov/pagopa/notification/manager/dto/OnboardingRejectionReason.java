package it.gov.pagopa.notification.manager.dto;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OnboardingRejectionReason {

  @NotNull
  private OnboardingRejectionReasonType type;
  @NotNull
  private String code;
  private String authority;
  private String authorityLabel;
  private String detail;

  public enum OnboardingRejectionReasonType {
    TECHNICAL_ERROR,
    CONSENSUS_MISSED,
    INVALID_REQUEST,
    BUDGET_EXHAUSTED,
    AUTOMATED_CRITERIA_FAIL,
    OUT_OF_RANKING
  }
}
