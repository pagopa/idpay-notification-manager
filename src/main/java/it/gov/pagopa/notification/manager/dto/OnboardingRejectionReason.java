package it.gov.pagopa.notification.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.validation.constraints.NotNull;

@Getter
@AllArgsConstructor
public class OnboardingRejectionReason {

  @NotNull
  private OnboardingRejectionReasonType type;
  @NotNull
  private OnboardingRejectionReasonCode code;
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

  public enum OnboardingRejectionReasonCode {
    AUTOMATED_CRITERIA_ISEE_FAIL("ISEE"),
    AUTOMATED_CRITERIA_BIRTHDATE_FAIL("Data di nascita"),
    AUTOMATED_CRITERIA_RESIDENCE_FAIL("Residenza");

    private final String detail;

    OnboardingRejectionReasonCode(String detail){
      this.detail = detail;
    }

    public String getDetail() {
      return detail;
    }
  }
}
