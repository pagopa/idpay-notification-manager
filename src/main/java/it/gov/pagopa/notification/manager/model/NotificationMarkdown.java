package it.gov.pagopa.notification.manager.model;

import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class NotificationMarkdown {
  private static final List<OnboardingRejectionReasonType> ONBOARDING_KO_TYPE_GENERIC_MARKDOWN_NO_RETRY =
          List.of(OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL,
                  OnboardingRejectionReasonType.CONSENSUS_MISSED,
                  OnboardingRejectionReasonType.INVALID_REQUEST,
                  OnboardingRejectionReasonType.BUDGET_EXHAUSTED);
  private static final List<OnboardingRejectionReasonType> FAILED_RETRIEVE_PDND_DATA_TYPE =
          List.of(OnboardingRejectionReasonType.ISEE_TYPE_KO,
                  OnboardingRejectionReasonType.BIRTHDATE_KO,
                  OnboardingRejectionReasonType.RESIDENCE_KO,
                  OnboardingRejectionReasonType.FAMILY_KO);

  private static final List<OnboardingRejectionReasonType> ONBOARDING_KO_TYPE_NO_RETRY = new ArrayList<>();
  static {
    ONBOARDING_KO_TYPE_NO_RETRY.addAll(ONBOARDING_KO_TYPE_GENERIC_MARKDOWN_NO_RETRY);
    ONBOARDING_KO_TYPE_NO_RETRY.addAll(FAILED_RETRIEVE_PDND_DATA_TYPE);
    ONBOARDING_KO_TYPE_NO_RETRY.add(OnboardingRejectionReasonType.OUT_OF_RANKING);
  }


  @Value("${notification.manager.markdown.double.new.line}")
  private String markdownDoubleNewLine;

  @Value("${notification.manager.subject.ok}")
  private String subjectOk;

  @Value("${notification.manager.subject.ko}")
  private String subjectKo;

  @Value("${notification.manager.subject.ko.tech}")
  private String subjectKoTech;

  @Value("${notification.manager.markdown.ok}")
  private String markdownOk;

  @Value("${notification.manager.markdown.ok.cta}")
  private String markdownOkCta;

  @Value("${notification.manager.markdown.ko.pdnd}")
  private String markdownKoPdnd;

  @Value("${notification.manager.markdown.ko.ranking}")
  private String markdownKoRanking;

  @Value("${notification.manager.markdown.ko.mistake}")
  private String markdownKoMistake;

  @Value("${notification.manager.markdown.ko.tech}")
  private String markdownKoTech;

  @Value("${notification.manager.markdown.ko.apology}")
  private String markdownKoApology;

  @Value("${notification.manager.subject.ko.checkIban}")
  private String subjectCheckIbanKo;

  @Value("${notification.manager.markdown.ko.checkIban}")
  private String markdownCheckIbanKo;

  @Getter
  @Value("${notification.manager.subject.initiative.publishing}")
  private String subjectInitiativePublishing;

  @Getter
  @Value("${notification.manager.markdown.initiative.publishing}")
  private String markdownInitiativePublishing;

  @Value("${notification.manager.subject.ok.refund}")
  private String subjectRefundOk;

  @Value("${notification.manager.markdown.ok.refund}")
  private String markdownRefundOk;

  @Value("${notification.manager.subject.ko.refund}")
  private String subjectRefundKo;

  @Value("${notification.manager.markdown.ko.refund}")
  private String markdownRefundKo;
  @Value("${notification.manager.subject.suspension}")
  private String subjectSuspension;
  @Value("${notification.manager.markdown.suspension}")
  private String markdownSuspension;
  @Value("${notification.manager.subject.readmission}")
  private String subjectReadmission;
  @Value("${notification.manager.markdown.readmission}")
  private String markdownReadmission;
  @Value("${notification.manager.subject.demanded}")
  private String subjectDemanded;
  @Value("${notification.manager.markdown.demanded}")
  private String markdownDemanded;
  @Value("${notification.manager.markdown.ko.budget}")
  private String markdownKoBudget;
  @Value("${notification.manager.markdown.ko.rejected.noRetry}")
  private String markdownKoRejectedNoRetry;

  @Value("${notification.manager.markdown.ko.generic}")
  private String markdownKoGeneric;

  @Value("${notification.manager.subject.ok.type2}")
  private String subjectOkType2;

  @Value("${notification.manager.markdown.ok.type2}")
  private String markdownOkType2;

  public String getSubjectCheckIbanKo() {
    return this.subjectCheckIbanKo;
  }

  public String getMarkdownCheckIbanKo() {
    return this.markdownCheckIbanKo;
  }

  public String getSubjectRefund(String status) {
    return ("ACCEPTED".equals(status)) ? subjectRefundOk : subjectRefundKo;
  }

  public String getMarkdownRefund(String status, BigDecimal effectiveReward) {
    DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    symbols.setDecimalSeparator(',');

    DecimalFormat df = new DecimalFormat("###.00", symbols);
    df.setGroupingUsed(false);
    return ("ACCEPTED".equals(status))
        ? replaceMessageItem(markdownRefundOk, "effectiveReward", df.format(effectiveReward))
        : markdownRefundKo;
  }

  public String getSubject(EvaluationDTO evaluationDTO) {
    if(NotificationConstants.STATUS_ONBOARDING_OK.equals(evaluationDTO.getStatus())
            && NotificationConstants.ORGANIZATION_NAME_TYPE2.equalsIgnoreCase(evaluationDTO.getOrganizationName())
            && evaluationDTO.getInitiativeName().toLowerCase().contains(NotificationConstants.INITIATIVE_NAME_TYPE2_CHECK.toLowerCase())){
      return this.subjectOkType2;
    }

    if(NotificationConstants.STATUS_ONBOARDING_OK.equals(evaluationDTO.getStatus())  || NotificationConstants.STATUS_ONBOARDING_JOINED.equals(evaluationDTO.getStatus())){
      return this.subjectOk;
    }

    if(NotificationConstants.STATUS_ONBOARDING_DEMANDED.equals(evaluationDTO.getStatus())){
      return replaceMessageItem(subjectDemanded, NotificationConstants.INITIATIVE_NAME_KEY, evaluationDTO.getInitiativeName());
    }

    return getSubjectKo(
            evaluationDTO.getInitiativeName(), evaluationDTO.getOnboardingRejectionReasons());
  }

  public String getSubject(Notification notification) {

    if(NotificationConstants.STATUS_ONBOARDING_OK.equals(notification.getOnboardingOutcome())
            && NotificationConstants.ORGANIZATION_NAME_TYPE2.equalsIgnoreCase(notification.getOrganizationName())
            && notification.getInitiativeName().toLowerCase().contains(NotificationConstants.INITIATIVE_NAME_TYPE2_CHECK.toLowerCase())){
      return this.subjectOkType2;
    }

    return notification.getOnboardingOutcome().equals(NotificationConstants.STATUS_ONBOARDING_OK)||
            notification.getOnboardingOutcome().equals(NotificationConstants.STATUS_ONBOARDING_JOINED)
        ? this.subjectOk
        : getSubjectKo(
            notification.getInitiativeName(), notification.getRejectReasons());
  }

  private String getSubjectKo(
          String initiativeName, List<OnboardingRejectionReason> onboardingRejectionReasons) {

    OnboardingRejectionReasonType reason = onboardingRejectionReasons.get(0).getType();

    return ONBOARDING_KO_TYPE_NO_RETRY.contains(reason)
        ? replaceMessageItem(
            this.subjectKo, NotificationConstants.INITIATIVE_NAME_KEY, initiativeName)
        : this.subjectKoTech;
  }

  public String getMarkdown(EvaluationDTO evaluationDTO) {

    if(NotificationConstants.STATUS_ONBOARDING_OK.equals(evaluationDTO.getStatus())
            && NotificationConstants.ORGANIZATION_NAME_TYPE2.equalsIgnoreCase(evaluationDTO.getOrganizationName())
            && evaluationDTO.getInitiativeName().toLowerCase().contains(NotificationConstants.INITIATIVE_NAME_TYPE2_CHECK.toLowerCase())){
      return replaceMessageItem(markdownOkType2,
              NotificationConstants.INITIATIVE_NAME_KEY,
              evaluationDTO.getInitiativeName());
    }

    if (NotificationConstants.STATUS_ONBOARDING_OK.equals(evaluationDTO.getStatus())
            || NotificationConstants.STATUS_ONBOARDING_JOINED.equals(evaluationDTO.getStatus())){
      return replaceMessageItem(
              this.markdownOkCta,
              NotificationConstants.INITIATIVE_ID_KEY,
              evaluationDTO.getInitiativeId())
              .concat(this.markdownDoubleNewLine)
              .concat(replaceMessageItem(
                      this.markdownOk,
                      NotificationConstants.INITIATIVE_NAME_KEY,
                      evaluationDTO.getInitiativeName()));
    }

    if(NotificationConstants.STATUS_ONBOARDING_DEMANDED.equals(evaluationDTO.getStatus())){
      return replaceMessageItem(markdownDemanded,
              NotificationConstants.INITIATIVE_NAME_KEY,
              evaluationDTO.getInitiativeName());
    }

    return getMarkdownKo(
            evaluationDTO.getInitiativeName(), evaluationDTO.getOnboardingRejectionReasons());
  }

  public String getMarkdown(Notification notification) {
    if(NotificationConstants.STATUS_ONBOARDING_OK.equals(notification.getOnboardingOutcome())
            && NotificationConstants.ORGANIZATION_NAME_TYPE2.equalsIgnoreCase(notification.getOrganizationName())
            && notification.getInitiativeName().toLowerCase().contains(NotificationConstants.INITIATIVE_NAME_TYPE2_CHECK.toLowerCase())){
      return replaceMessageItem(markdownOkType2,
              NotificationConstants.INITIATIVE_NAME_KEY,
              notification.getInitiativeName());
    }

    return notification.getOnboardingOutcome().equals(NotificationConstants.STATUS_ONBOARDING_OK) ||
            notification.getOnboardingOutcome().equals(NotificationConstants.STATUS_ONBOARDING_JOINED)
        ? replaceMessageItem(
        this.markdownOkCta,
        NotificationConstants.INITIATIVE_ID_KEY,
        notification.getInitiativeId())
            .concat(this.markdownDoubleNewLine)
            .concat(replaceMessageItem(
                this.markdownOk,
                NotificationConstants.INITIATIVE_NAME_KEY,
                notification.getInitiativeName()))
        : getMarkdownKo(
            notification.getInitiativeName(), notification.getRejectReasons());
  }

  private String replaceMessageItem(String message, String key, String value) {
    return message.replace(
        NotificationConstants.MARKDOWN_TAG + key + NotificationConstants.MARKDOWN_TAG,
        StringUtils.hasLength(value) ? value : NotificationConstants.MARKDOWN_NA);
  }

  private String getMarkdownKo(
          String initiativeName, List<OnboardingRejectionReason> onboardingRejectionReasons) {

    OnboardingRejectionReasonType reason = onboardingRejectionReasons.get(0).getType();

    if (FAILED_RETRIEVE_PDND_DATA_TYPE.contains(reason)){
      return getMarkdownKoPdnd(initiativeName, onboardingRejectionReasons);
    }

    if (reason.equals(OnboardingRejectionReasonType.OUT_OF_RANKING)) {
      return getMarkdownKoRanking(initiativeName);
    }

    if(reason.equals(OnboardingRejectionReasonType.BUDGET_EXHAUSTED)){
      return getMarkdownKoBudget(initiativeName);
    }

    if(ONBOARDING_KO_TYPE_GENERIC_MARKDOWN_NO_RETRY.contains(reason)){
      return getMarkdownKoNoRetry(initiativeName, onboardingRejectionReasons);
    }
    return getMarkdownKoTech(initiativeName);
  }

  private String getMarkdownKoPdnd(
      String initiativeName, List<OnboardingRejectionReason> onboardingRejectionReasons) {
    return replaceMessageItem(
            this.markdownKoPdnd, NotificationConstants.INITIATIVE_NAME_KEY, initiativeName)
        .concat(this.markdownDoubleNewLine)
        .concat(getPdndRejectReasons(onboardingRejectionReasons))
        .concat(this.markdownKoMistake)
        .concat(this.markdownDoubleNewLine)
        .concat(this.markdownKoApology);
  }

  private String getPdndRejectReasons(List<OnboardingRejectionReason> onboardingRejectionReasons) {
    final StringBuilder builder = new StringBuilder();
    onboardingRejectionReasons.stream()
        .filter(
            reason ->
                    FAILED_RETRIEVE_PDND_DATA_TYPE.contains(reason.getType()))
        .toList()
        .forEach(
            reason ->
                builder
                    .append("* ")
                    .append(reason.getAuthorityLabel())
                    .append(" : ")
                    .append(reason.getDetail() != null ? reason.getDetail() : reason.getCode().getDetail())
                    .append(this.markdownDoubleNewLine));
    return builder.toString();
  }

  private String getMarkdownKoRanking(String initiativeName) {
    return replaceMessageItem(
            this.markdownKoRanking, NotificationConstants.INITIATIVE_NAME_KEY, initiativeName)
        .concat(this.markdownDoubleNewLine)
        .concat(this.markdownKoMistake)
        .concat(this.markdownDoubleNewLine)
        .concat(this.markdownKoApology);
  }

  private String getMarkdownKoBudget(String initiativeName) {
    return replaceMessageItem(
            this.markdownKoBudget, NotificationConstants.INITIATIVE_NAME_KEY, initiativeName)
            .concat(this.markdownDoubleNewLine)
            .concat(this.markdownKoApology);
  }

  private String getMarkdownKoNoRetry(String initiativeName, List<OnboardingRejectionReason> onboardingRejectionReasons) {
    return replaceMessageItem(
            this.markdownKoRejectedNoRetry, NotificationConstants.INITIATIVE_NAME_KEY, initiativeName)
            .concat(this.markdownDoubleNewLine)
            .concat(getRejectedReasons(onboardingRejectionReasons))
            .concat(this.markdownKoMistake)
            .concat(this.markdownDoubleNewLine)
            .concat(this.markdownKoApology);
  }

  private String getRejectedReasons(List<OnboardingRejectionReason> onboardingRejectionReasons) {
    final StringBuilder builder = new StringBuilder();
    onboardingRejectionReasons.stream()
            .filter(
                    reason ->
                            ONBOARDING_KO_TYPE_GENERIC_MARKDOWN_NO_RETRY.contains(reason.getType()))
            .toList()
            .forEach(
                    reason ->
                            builder
                                    .append("* ")
                                    .append(reason.getDetail() != null
                                            ? reason.getDetail()
                                            : reason.getCode().getDetail())
                                    .append(this.markdownDoubleNewLine)

                    );
    return builder.toString();
  }

  private String getMarkdownKoTech(String initiativeName) {
    return replaceMessageItem(
            this.markdownKoGeneric,
            NotificationConstants.INITIATIVE_NAME_KEY,
            initiativeName)
        .concat(this.markdownDoubleNewLine)
        .concat(this.markdownKoApology);
  }

  public String getSubjectSuspension(String initiativeName) {
    return replaceMessageItem(this.subjectSuspension, NotificationConstants.INITIATIVE_NAME_KEY, initiativeName);
  }

  public String getMarkdownSuspension() {
    return this.markdownSuspension;
  }
  public String getSubjectReadmission(String initiativeName) {
    return replaceMessageItem(this.subjectReadmission, NotificationConstants.INITIATIVE_NAME_KEY, initiativeName);
  }

  public String getMarkdownReadmission() {
    return this.markdownReadmission;
  }
}
