package it.gov.pagopa.notification.manager.model;

import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonType;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Component
@Slf4j
public class NotificationMarkdown {

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
  @Value("${notification.manager.subject.authPayment}")
  private String subjectAuthPayment;
  @Value("${notification.manager.markdown.authPayment}")
  private String markdownAuthPayment;

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
    return evaluationDTO.getStatus().equals(NotificationConstants.STATUS_ONBOARDING_OK)
        ? this.subjectOk
        : getSubjectKo(
            evaluationDTO.getInitiativeName(), evaluationDTO.getOnboardingRejectionReasons());
  }

  public String getSubject(Notification notification) {
    return notification.getOnboardingOutcome().equals(NotificationConstants.STATUS_ONBOARDING_OK)
        ? this.subjectOk
        : getSubjectKo(
            notification.getInitiativeName(), notification.getRejectReasons());
  }

  private String getSubjectKo(
      String initiativeName, List<OnboardingRejectionReason> onboardingRejectionReasons) {
    String reason = onboardingRejectionReasons.get(0).getType().name();
    return reason.startsWith(OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL.name())
            || reason.equals(OnboardingRejectionReasonType.OUT_OF_RANKING.name())
        ? replaceMessageItem(
            this.subjectKo, NotificationConstants.INITIATIVE_NAME_KEY, initiativeName)
        : this.subjectKoTech;
  }

  public String getMarkdown(EvaluationDTO evaluationDTO) {
    return evaluationDTO.getStatus().equals(NotificationConstants.STATUS_ONBOARDING_OK)
        ? replaceMessageItem(
        this.markdownOkCta,
        NotificationConstants.INITIATIVE_ID_KEY,
        evaluationDTO.getInitiativeId())
            .concat(this.markdownDoubleNewLine)
            .concat(replaceMessageItem(
                this.markdownOk,
                NotificationConstants.INITIATIVE_NAME_KEY,
                evaluationDTO.getInitiativeName()))
        : getMarkdownKo(
            evaluationDTO.getInitiativeName(), evaluationDTO.getOnboardingRejectionReasons());
  }

  public String getMarkdown(Notification notification) {
    return notification.getOnboardingOutcome().equals(NotificationConstants.STATUS_ONBOARDING_OK)
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
    String reason = onboardingRejectionReasons.get(0).getType().name();
    if (reason.startsWith(OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL.name())) {
      return getMarkdownKoPdnd(initiativeName, onboardingRejectionReasons);
    }
    if (reason.equals(OnboardingRejectionReasonType.OUT_OF_RANKING.name())) {
      return getMarkdownKoRanking(initiativeName);
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
                reason.getType().equals(OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL))
        .toList()
        .forEach(
            reason ->
                builder
                    .append("* ")
                    .append(reason.getAuthorityLabel())
                    .append(" : ")
                    .append(reason.getCode().getDetail())
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

  private String getMarkdownKoTech(String initiativeName) {
    return replaceMessageItem(this.markdownKoTech, NotificationConstants.INITIATIVE_NAME_KEY, initiativeName)
        .concat(this.markdownDoubleNewLine)
        .concat(this.markdownKoApology);
  }

  public String getSubjectSuspension(String initiativeName) {
    return replaceMessageItem(this.subjectSuspension, NotificationConstants.INITIATIVE_NAME_KEY, initiativeName);
  }

  public String getMarkdownSuspension() {
    return this.markdownSuspension;
  }

  //todo update subject and add test
  public String getSubjectAuthPayment() {
    return subjectAuthPayment;
  }

  //todo update markdown and add test
  public String getMarkdownAuthPayment(Long amountCents) {
    String amountEuro = NumberFormat.getCurrencyInstance(Locale.ITALY).format(amountCents/100.0);

    return replaceMessageItem(markdownAuthPayment, NotificationConstants.AMOUNT_EURO_KEY, amountEuro);
  }
}
