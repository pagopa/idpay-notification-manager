package it.gov.pagopa.notification.manager.dto;

import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import jakarta.validation.constraints.NotNull;

import static it.gov.pagopa.notification.manager.enums.NotificationEvent.*;

@Getter
@AllArgsConstructor
@Builder
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
    AUTOMATED_CRITERIA_ISEE_FAIL("ISEE non idoneo", ONBOARDING_KO_REJECTED_NO_RETRY),
    AUTOMATED_CRITERIA_BIRTHDATE_FAIL("Data di nascita non idonea", ONBOARDING_KO_REJECTED_NO_RETRY),
    AUTOMATED_CRITERIA_RESIDENCE_FAIL("Residenza non idonea", ONBOARDING_KO_REJECTED_NO_RETRY),
    CITIZEN_OUT_OF_RANKING("Graduatoria", ONBOARDING_KO_RANKING),
    INITIATIVE_BUDGET_EXHAUSTED("Budget iniziativa", ONBOARDING_KO_BUDGET),
    ISEE_TYPE_FAIL("Tipo ISEE", ONBOARDING_KO_PDND),
    FAMILY_CRITERIA_FAIL("Criteri di famiglia", ONBOARDING_KO_GENERIC),
    REJECTION_REASON_INITIATIVE_ENDED( "L'iniziativa è terminata", ONBOARDING_KO_INITIATIVE_ENDED),

    //region possible onboarding rejection reason added for robust code
    CONSENSUS_CHECK_TC_FAIL("Mancato consenso dei termini e condizioni", ONBOARDING_KO_REJECTED_NO_RETRY),
    CONSENSUS_CHECK_PDND_FAIL("Mancato consenso PDND", ONBOARDING_KO_REJECTED_NO_RETRY),
    CONSENSUS_CHECK_TC_ACCEPT_FAIL("Consenso termini e condizioni fornito fuori dal periodo di validità", ONBOARDING_KO_REJECTED_NO_RETRY),
    CONSENSUS_CHECK_CRITERIA_CONSENSUS_FAIL("Consenso ai criteri fornito fuori dal periodo di validità", ONBOARDING_KO_REJECTED_NO_RETRY),
    INVALID_INITIATIVE_ID ("Iniziativa non trovata", ONBOARDING_KO_REJECTED_NO_RETRY),
    //endregion

    RESIDENCE_FAIL("Residenza non disponibile", ONBOARDING_KO_PDND),
    BIRTHDATE_FAIL("Data di nascita non disponibile", ONBOARDING_KO_PDND),
    FAMILY_FAIL("Nucleo familiare non disponibile", ONBOARDING_KO_PDND),
    RULE_ENGINE_NOT_READY("Errore tecnico", ONBOARDING_KO_GENERIC),

    GENERIC_ERROR("Errore tecnico", ONBOARDING_KO_GENERIC);

    private final String detail;
    private final NotificationEvent notificationEvent;

    OnboardingRejectionReasonCode(String detail, NotificationEvent notificationEvent){
      this.detail = detail;
      this.notificationEvent = notificationEvent;
    }

    public String getDetail() {
      return detail;
    }

    public NotificationEvent getNotificationEvent() {return notificationEvent;}
  }
}
