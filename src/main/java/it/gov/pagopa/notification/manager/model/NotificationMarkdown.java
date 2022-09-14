package it.gov.pagopa.notification.manager.model;

import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonType;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
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

  public String getSubjectCheckIbanKo(){
    return this.subjectCheckIbanKo;
  }

  public String getMarkdownCheckIbanKo(){
    return this.markdownCheckIbanKo;
  }

  public String getSubject(EvaluationDTO evaluationDTO) {
    return evaluationDTO.getStatus().equals(NotificationConstants.STATUS_ONBOARDING_OK)
        ? this.subjectOk
        : getSubjectKo(
            evaluationDTO.getInitiativeName(), evaluationDTO.getOnboardingRejectionReasons());
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
            this.markdownOk,
            NotificationConstants.INITIATIVE_NAME_KEY,
            evaluationDTO.getInitiativeName())
        : getMarkdownKo(
            evaluationDTO.getInitiativeName(), evaluationDTO.getOnboardingRejectionReasons());
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
    return replaceMessageItem(this.markdownKoTech, "initiativeName", initiativeName)
        .concat(this.markdownDoubleNewLine)
        .concat(this.markdownKoApology);
  }
}
