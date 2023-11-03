package it.gov.pagopa.notification.manager.model;

import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonType;
import java.time.LocalDate;

import it.gov.pagopa.notification.manager.dto.mapper.NotificationMapper;
import it.gov.pagopa.notification.manager.event.producer.OutcomeProducer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
@ContextConfiguration(classes = NotificationMarkdown.class)
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
  @Test
  void getMarkdownSuspension_ok(){
    String actual = notificationMarkdown.getMarkdownSuspension();
    log.info(actual);
  }
  @Test
  void getSubjectSuspension_ok(){
    String actual = notificationMarkdown.getSubjectSuspension(INITIATIVE_NAME);
    log.info(actual);
  }
  @Test
  void getMarkdownReadmission_ok(){
    String actual = notificationMarkdown.getMarkdownReadmission();
    log.info(actual);
  }
  @Test
  void getSubjectReadmission_ok(){
    String actual = notificationMarkdown.getSubjectReadmission(INITIATIVE_NAME);
    log.info(actual);
  }

  @Test
  void getSubjectDemanded(){
    String subjectDemanded = notificationMarkdown.getSubject(getEvaluationDto(NotificationConstants.STATUS_ONBOARDING_DEMANDED, null));
    log.info(subjectDemanded);
  }

  @Test
  void getMarkdownDemanded(){
    String subjectDemanded = notificationMarkdown.getMarkdown(getEvaluationDto(NotificationConstants.STATUS_ONBOARDING_DEMANDED, null));
    log.info(subjectDemanded);
  }

  @Test
  void getSubjectJoined(){
    String subjectDemanded = notificationMarkdown.getSubject(getEvaluationDto(NotificationConstants.STATUS_ONBOARDING_JOINED, null));
    log.info(subjectDemanded);
  }

  @Test
  void getMarkdownJoined(){
    String subjectDemanded = notificationMarkdown.getMarkdown(getEvaluationDto(NotificationConstants.STATUS_ONBOARDING_JOINED, null));
    log.info(subjectDemanded);
  }

  private EvaluationDTO getEvaluationDto(String status, List<OnboardingRejectionReason> rejectionReasons){
    return new EvaluationDTO(
            USER_ID,
            INITIATIVE_ID,
            INITIATIVE_NAME,
            TEST_DATE_ONLY_DATE,
            "ORGANIZATIONID",
            status,
            TEST_DATE,
            TEST_DATE,
            rejectionReasons,
            new BigDecimal(500),
            1L);
  }

  @Test
  void markdownTest(){ //TODO remove
    List<OnboardingRejectionReason> list =
            List.of(
//                    new OnboardingRejectionReason(
//                            OnboardingRejectionReasonType.BUDGET_EXHAUSTED,
//                            OnboardingRejectionReasonCode.INITIATIVE_BUDGET_EXHAUSTED,
//                            null,
//                            null,
//                            null),
//                    new OnboardingRejectionReason(
//                            OnboardingRejectionReasonType.OUT_OF_RANKING,
//                            OnboardingRejectionReasonCode.CITIZEN_OUT_OF_RANKING,
//                            null,
//                            null,
//                            "9"),
//                    new OnboardingRejectionReason(
//                            OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL,
//                            OnboardingRejectionReasonCode.AUTOMATED_CRITERIA_BIRTHDATE_FAIL,
//                            "AGID",
//                            "Agenzia per l'Italia Digitale",
//                            null),
//                    new OnboardingRejectionReason(
//                            OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL,
//                            OnboardingRejectionReasonCode.AUTOMATED_CRITERIA_ISEE_FAIL,
//                            "INPS",
//                            "Istituto Nazionale Previdenza Sociale",
//                            null),
//                    new OnboardingRejectionReason(
//                            OnboardingRejectionReasonType.ISEE_TYPE_KO,
//                            OnboardingRejectionReasonCode.ISEE_TYPE_FAIL,
//                            "INPS",
//                            "Istituto Nazionale Previdenza Sociale",
//                            "ISEE non disponibile"),
//                    new OnboardingRejectionReason(
//                            OnboardingRejectionReasonType.RESIDENCE_KO,
//                            OnboardingRejectionReasonCode.RESIDENCE_FAIL,
//                            "AGID",
//                            "Agenzia per l'Italia Digitale",
//                            "Residenza non disponibile"),
//                    new OnboardingRejectionReason(
//                            OnboardingRejectionReasonType.BIRTHDATE_KO,
//                            OnboardingRejectionReasonCode.RESIDENCE_FAIL,
//                            "AGID",
//                            "Agenzia per l'Italia Digitale",
//                            "Data di nascita non disponibile"),
//                    new OnboardingRejectionReason(
//                            OnboardingRejectionReasonType.FAMILY_KO,
//                            OnboardingRejectionReasonCode.FAMILY_FAIL,
//                            "INPS",
//                            "Istituto Nazionale Previdenza Sociale",
//                            "Nucleo familiare non disponibile"),
//                    new OnboardingRejectionReason(
//                            OnboardingRejectionReasonType.CONSENSUS_MISSED,
//                            OnboardingRejectionReasonCode.CONSENSUS_CHECK_TC_FAIL,
//                            null,
//                            null,
//                            null),
//                    new OnboardingRejectionReason(
//                            OnboardingRejectionReasonType.CONSENSUS_MISSED,
//                            OnboardingRejectionReasonCode.CONSENSUS_CHECK_PDND_FAIL,
//                            null,
//                            null,
//                            null),
//                    new OnboardingRejectionReason(
//                            OnboardingRejectionReasonType.INVALID_REQUEST,
//                            OnboardingRejectionReasonCode.INVALID_INITIATIVE_ID,
//                            null,
//                            null,
//                            null),
//                    new OnboardingRejectionReason(
//                            OnboardingRejectionReasonType.INVALID_REQUEST,
//                            OnboardingRejectionReasonCode.CONSENSUS_CHECK_TC_ACCEPT_FAIL,
//                            null,
//                            null,
//                            null),
//                    new OnboardingRejectionReason(
//                            OnboardingRejectionReasonType.INVALID_REQUEST,
//                            OnboardingRejectionReasonCode.CONSENSUS_CHECK_CRITERIA_CONSENSUS_FAIL,
//                            null,
//                            null,
//                            null),
//                    new OnboardingRejectionReason(
//                            OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL,
//                            OnboardingRejectionReasonCode.AUTOMATED_CRITERIA_ISEE_FAIL,
//                            "INPS",
//                            "Istituto Nazionale Previdenza Sociale",
//                            null),
//                    new OnboardingRejectionReason(
//                            OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL,
//                            OnboardingRejectionReasonCode.AUTOMATED_CRITERIA_RESIDENCE_FAIL,
//                            "INPS",
//                            "Istituto Nazionale Previdenza Sociale",
//                            null),
//                    new OnboardingRejectionReason(
//                            OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL,
//                            OnboardingRejectionReasonCode.AUTOMATED_CRITERIA_BIRTHDATE_FAIL,
//                            "INPS",
//                            "Istituto Nazionale Previdenza Sociale",
//                            null),
                    new OnboardingRejectionReason(
                            OnboardingRejectionReasonType.TECHNICAL_ERROR,
                            null,
                            null,
                            null,
                            null)


            );


    EvaluationDTO evaluationDTO =
            new EvaluationDTO(
                    "TEST_TOKEN-USERID",
                    "INITIATIVEID",
                    "INITIATIVE_NAME",
                    TEST_DATE_ONLY_DATE, //initiative endDate
                    "ORGANIZATIONID",
                    NotificationConstants.STATUS_ONBOARDING_DEMANDED,
                    TEST_DATE, //ADMISSIBILITY TEST CHEKH
                    TEST_DATE, //CONSENSUS TIMESTAMP
                    list, //ONBOARDING REJECTIONS
                    new BigDecimal(500),  //BENEFICIARY BUDGET
                    1L); // REANKING VALUE

    System.out.println("SUBJECT");
    System.out.println(notificationMarkdown.getSubject(evaluationDTO));
    System.out.println("\nMARCKDOWN");
    System.out.println(notificationMarkdown.getMarkdown(evaluationDTO));

  }
}
