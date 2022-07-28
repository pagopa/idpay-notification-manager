package it.gov.pagopa.notification.manager.model;

import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
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

  public String getSubject(EvaluationDTO evaluationDTO) {
    return evaluationDTO.getStatus().equals(NotificationConstants.STATUS_ONBOARDING_OK)
        ? this.subjectOk
        : getSubjectKo(
            evaluationDTO.getInitiativeId(), evaluationDTO.getOnboardingRejectionReasons());
  }

  private String getSubjectKo(String initiativeId, List<String> onboardingRejectionReasons) {
    String reason = onboardingRejectionReasons.get(0);
    return reason.startsWith(NotificationConstants.AUTOMATED_CRITERIA)
            || reason.equals(NotificationConstants.RANKING_FAIL)
        ? replaceMessageItem(this.subjectKo, NotificationConstants.INITIATIVE_NAME_KEY, initiativeId)
        : this.subjectKoTech;
  }

  public String getMarkdown(EvaluationDTO evaluationDTO) {
    return evaluationDTO.getStatus().equals(NotificationConstants.STATUS_ONBOARDING_OK)
        ? replaceMessageItem(
            this.markdownOk,
            NotificationConstants.INITIATIVE_NAME_KEY,
            evaluationDTO.getInitiativeId())
        : getMarkdownKo(
            evaluationDTO.getInitiativeId(), evaluationDTO.getOnboardingRejectionReasons());
  }

  private String replaceMessageItem(String message, String key, String value) {
    return message.replace(
        NotificationConstants.MARKDOWN_TAG + key + NotificationConstants.MARKDOWN_TAG,
        StringUtils.hasLength(value) ? value : NotificationConstants.MARKDOWN_NA);
  }

  private String getMarkdownKo(String initiativeId, List<String> onboardingRejectionReasons) {
    String reason = onboardingRejectionReasons.get(0);
    if (reason.startsWith(NotificationConstants.AUTOMATED_CRITERIA)) {
      return getMarkdownKoPdnd(initiativeId);
    }
    if (reason.equals(NotificationConstants.RANKING_FAIL)) {
      return getMarkdownKoRanking(initiativeId);
    }
    return getMarkdownKoTech(initiativeId);
  }

  private String getMarkdownKoPdnd(String initiativeId) {
    return replaceMessageItem(this.markdownKoPdnd, NotificationConstants.INITIATIVE_NAME_KEY, initiativeId)
        .concat(this.markdownDoubleNewLine)
        .concat(this.markdownKoMistake)
        .concat(this.markdownDoubleNewLine)
        .concat(this.markdownKoApology);
  }

  private String getMarkdownKoRanking(String initiativeId) {
    return replaceMessageItem(this.markdownKoRanking, NotificationConstants.INITIATIVE_NAME_KEY, initiativeId)
        .concat(this.markdownDoubleNewLine)
        .concat(this.markdownKoMistake)
        .concat(this.markdownDoubleNewLine)
        .concat(this.markdownKoApology);
  }

  private String getMarkdownKoTech(String initiativeId) {
    return replaceMessageItem(this.markdownKoTech, "initiativeName", initiativeId)
        .concat(this.markdownDoubleNewLine)
        .concat(this.markdownKoApology);
  }
}
