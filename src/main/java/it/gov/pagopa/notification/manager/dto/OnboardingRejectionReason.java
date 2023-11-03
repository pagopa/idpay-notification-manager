package it.gov.pagopa.notification.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import jakarta.validation.constraints.NotNull;

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
    ISEE_TYPE_KO,
    FAMILY_KO,
    BIRTHDATE_KO,
    RESIDENCE_KO,
    OUT_OF_RANKING,
    FAMILY_CRITERIA_KO
  }

  public enum OnboardingRejectionReasonCode {
    AUTOMATED_CRITERIA_ISEE_FAIL("ISEE non idoneo"),
    AUTOMATED_CRITERIA_BIRTHDATE_FAIL("Data di nascita non idonea"),
    AUTOMATED_CRITERIA_RESIDENCE_FAIL("Residenza non idonea"),
    CITIZEN_OUT_OF_RANKING("Graduatoria"),
    INITIATIVE_BUDGET_EXHAUSTED("Budget iniziativa"),
    ISEE_TYPE_FAIL("Tipo ISEE"),
    FAMILY_CRITERIA_FAIL("Criteri di famiglia"),

    //region possible onboarding rejection reason added for robust code
    CONSENSUS_CHECK_TC_FAIL("Mancato consenso dei termini e condizioni"),
    CONSENSUS_CHECK_PDND_FAIL("Mancato consenso PDND"),
    CONSENSUS_CHECK_TC_ACCEPT_FAIL("Consenso termini e condizioni fornito fuori dal periodo di validità"),
    CONSENSUS_CHECK_CRITERIA_CONSENSUS_FAIL("Consenso ai criteri fornito fuori dal periodo di validità"),
    INVALID_INITIATIVE_ID ("Iniziativa non trovata"),
    //endregion

    RESIDENCE_FAIL("Residenza non disponibile"),
    BIRTHDATE_FAIL("Data di nascita non disponibile"),
    FAMILY_FAIL("Nucleo familiare non disponibile");

    private final String detail;

    OnboardingRejectionReasonCode(String detail){
      this.detail = detail;
    }

    public String getDetail() {
      return detail;
    }
  }
}
