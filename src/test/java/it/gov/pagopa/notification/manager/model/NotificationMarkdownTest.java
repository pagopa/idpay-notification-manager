package it.gov.pagopa.notification.manager.model;

import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonType;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@ExtendWith({SpringExtension.class})
@ContextConfiguration(classes = NotificationMarkdown.class)
@TestPropertySource(
    properties = {
      "notification.manager.markdown.double.new.line=\\n\\n",
      "notification.manager.subject.ok=Il tuo Bonus è attivo",
      "notification.manager.subject.ok.refund=Ti è stato accreditato un rimborso!",
      "notification.manager.markdown.ok.cta=---\\nit:\\n    cta_1: \\n        text: \"Vai all'iniziativa\"\\n        action: \"ioit://idpay/initiative/%initiativeId%\"\\nen:\\n    cta_1: \\n        text: \"Go to the bonus page\"\\n        action: \"ioit://idpay/initiative/%initiativeId%\"\\n---",
      "notification.manager.subject.ko=Non è stato possibile attivare %initiativeName%",
      "notification.manager.subject.ko.tech=Abbiamo riscontrato dei problemi",
      "notification.manager.markdown.ok=Buone notizie! Hai ottenuto %initiativeName%. Da questo momento puoi visualizzare il bonus nella sezione Portafoglio dell'app IO.\\n\\nTi ricordiamo che per iniziare ad usufruire del bonus devi configurare almeno un metodo di pagamento.\\n\\nPuoi trovare maggiori informazioni sul [sito](http://example.com/).",
      "notification.manager.markdown.ok.refund=Hai ottenuto un rimborso di %effectiveReward% euro!",
      "notification.manager.markdown.ko.pdnd=Purtroppo non hai i requisiti necessari per aderire a %initiativeName% per i seguenti motivi:",
      "notification.manager.markdown.ko.ranking=Purtroppo non è stato possibile attivare %initiativeName% in quanto i tuoi requisiti non rientrano nella graduatoria.",
      "notification.manager.markdown.ko.mistake=Se ritieni che ci sia stato un errore puoi segnalarlo direttamente all'Ente erogatore dell'iniziativa.",
      "notification.manager.markdown.ko.tech=Si è verificato un errore nel processare la tua richiesta di %initiativeName%.\\nTi chiediamo di riprovare.",
      "notification.manager.markdown.ko.apology=Ci scusiamo per il disagio."
    })
class NotificationMarkdownTest {

  private static final String SUBJECT_OK = "Il tuo Bonus è attivo";
  private static final String SUBJECT_KO = "Non è stato possibile attivare TESTINITIATIVE01";
  private static final String SUBJECT_KO_TECH = "Abbiamo riscontrato dei problemi";
  private static final String USER_ID = "USER_ID";
  private static final String INITIATIVE_ID = "TESTINITIATIVE01";
  private static final String INITIATIVE_NAME = "NAMETESTINITIATIVE01";
  private static final LocalDateTime TEST_DATE = LocalDateTime.now();
  private static final LocalDate TEST_DATE_ONLY_DATE = LocalDate.now();

  private static final EvaluationDTO EVALUATION_DTO =
      new EvaluationDTO(
          USER_ID,
          INITIATIVE_ID,
          INITIATIVE_NAME,
          TEST_DATE_ONLY_DATE,
          INITIATIVE_ID,
          NotificationConstants.STATUS_ONBOARDING_OK,
          TEST_DATE,
          TEST_DATE,
          List.of(),
          new BigDecimal(500), 1L);
  private static final EvaluationDTO EVALUATION_DTO_KO_PDND =
      new EvaluationDTO(
          USER_ID,
          INITIATIVE_ID,
          INITIATIVE_ID,
          TEST_DATE_ONLY_DATE,
          INITIATIVE_ID,
          NotificationConstants.STATUS_ONBOARDING_KO,
          TEST_DATE,
          TEST_DATE,
          List.of(
              new OnboardingRejectionReason(
                  OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL,
                  OnboardingRejectionReasonCode.AUTOMATED_CRITERIA_ISEE_FAIL,
                  "AUTHORITY",
                  "LABEL",
                  "DETAIL")),
          new BigDecimal(500), 1L);

  private static final EvaluationDTO EVALUATION_DTO_KO_RANKING =
      new EvaluationDTO(
          USER_ID,
          INITIATIVE_ID,
          INITIATIVE_ID,
          TEST_DATE_ONLY_DATE,
          INITIATIVE_ID,
          NotificationConstants.STATUS_ONBOARDING_KO,
          TEST_DATE,
          TEST_DATE,
          List.of(
              new OnboardingRejectionReason(
                  OnboardingRejectionReasonType.OUT_OF_RANKING,
                  null,
                  "AUTHORITY",
                  "LABEL",
                  "DETAIL")),
          new BigDecimal(500),1L);

  private static final EvaluationDTO EVALUATION_DTO_KO_TECH =
      new EvaluationDTO(
          USER_ID,
          INITIATIVE_ID,
          INITIATIVE_ID,
          TEST_DATE_ONLY_DATE,
          INITIATIVE_ID,
          NotificationConstants.STATUS_ONBOARDING_KO,
          TEST_DATE,
          TEST_DATE,
          List.of(
              new OnboardingRejectionReason(
                  OnboardingRejectionReasonType.TECHNICAL_ERROR,
                  null,
                  "AUTHORITY",
                  "LABEL",
                  "DETAIL")),
          new BigDecimal(500), 1L);

  @Autowired NotificationMarkdown notificationMarkdown;

  @Test
  void getSubject_status_ok() {
    String actual = notificationMarkdown.getSubject(EVALUATION_DTO);
    assertEquals(SUBJECT_OK, actual);
  }

  @Test
  void getSubject_status_ko_pdnd() {
    String actual = notificationMarkdown.getSubject(EVALUATION_DTO_KO_PDND);
    assertEquals(SUBJECT_KO, actual);
  }

  @Test
  void getSubject_status_ko_ranking() {
    String actual = notificationMarkdown.getSubject(EVALUATION_DTO_KO_RANKING);
    assertEquals(SUBJECT_KO, actual);
  }

  @Test
  void getSubject_status_ko_tech() {
    String actual = notificationMarkdown.getSubject(EVALUATION_DTO_KO_TECH);
    assertEquals(SUBJECT_KO_TECH, actual);
  }

  @Test
  void getMarkdown_status_ok() {
    String actual = notificationMarkdown.getMarkdown(EVALUATION_DTO);
    log.info(actual);
  }

  @Test
  void getMarkdown_status_ko_pdnd() {
    String actual = notificationMarkdown.getMarkdown(EVALUATION_DTO_KO_PDND);
    log.info(actual);
  }

  @Test
  void getMarkdown_status_ko_ranking() {
    String actual = notificationMarkdown.getMarkdown(EVALUATION_DTO_KO_RANKING);
    log.info(actual);
  }

  @Test
  void getMarkdown_status_ko_tech() {
    String actual = notificationMarkdown.getMarkdown(EVALUATION_DTO_KO_TECH);
    log.info(actual);
  }

  @Test
  void getSubjectRefund_ok(){
    String actual = notificationMarkdown.getSubjectRefund("ACCEPTED");
    log.info(actual);
  }

  @Test
  void getSubjectRefund_ko(){
    String actual = notificationMarkdown.getSubjectRefund("REJECTED");
    log.info(actual);
  }

  @Test
  void getMarkdownRefund_ok(){
    String actual = notificationMarkdown.getMarkdownRefund("ACCEPTED", new BigDecimal("500.00"));
    log.info(actual);
  }

  @Test
  void getMarkdownRefund_ko(){
    String actual = notificationMarkdown.getMarkdownRefund("REJECTED", new BigDecimal("500.00"));
    log.info(actual);
  }
}
