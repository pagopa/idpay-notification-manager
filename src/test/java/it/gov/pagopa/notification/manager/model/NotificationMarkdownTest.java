package it.gov.pagopa.notification.manager.model;

import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static it.gov.pagopa.notification.manager.enums.Channel.IO;
import static it.gov.pagopa.notification.manager.enums.Channel.WEB;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
@ContextConfiguration(classes = NotificationMarkdown.class)
class NotificationMarkdownTest {

  private static final String SUBJECT_OK = "Il tuo Bonus è attivo";
  private static final String SUBJECT_OK_TYPE2 = "Il tuo Bonus è attivo!";
  private static final String SUBJECT_KO = "Non è stato possibile attivare TESTINITIATIVE01";
  private static final String SUBJECT_KO_TECH = "Abbiamo riscontrato dei problemi";
  private static final String MARKDOWN_OK = """
            ---
            it:
                cta_1:\s
                    text: "Vai all'iniziativa"
                    action: "ioit://idpay/initiative/%s"
            en:
                cta_1:\s
                    text: "Go to the bonus page"
                    action: "ioit://idpay/initiative/%s"
            ---
                        
            Buone notizie! Hai ottenuto %s. Da questo momento puoi visualizzare il bonus nella sezione Portafoglio dell'app IO.
                        
            Ti ricordiamo che per iniziare ad usufruire del bonus devi configurare almeno un metodo di pagamento.
                        
            Puoi trovare maggiori informazioni sul [sito](http://example.com/).""";
  private static final String MARKDOWN_OK_TYPE2 = """
                  Buone notizie! Ai soli fini della sperimentazione, hai i requisiti per procedere con l'iniziativa.
                  Da questo momento puoi visualizzare il bonus nella sezione Portafoglio.
                 
                  Per utilizzarlo, devi prima caricare i giustificativi di spesa.""";
  private static final String USER_ID = "USER_ID";
  private static final String INITIATIVE_ID = "TESTINITIATIVE01";
  private static final String INITIATIVE_NAME = "NAMETESTINITIATIVE01";
  private static final String ORGANIZATION_NAME = "NAMETESTORGANIZATION01";
  private static final LocalDateTime TEST_DATE = LocalDateTime.now();
  private static final LocalDate TEST_DATE_ONLY_DATE = LocalDate.now();

  private static final EvaluationDTO EVALUATION_DTO =
          new EvaluationDTO(
                  USER_ID,
                  INITIATIVE_ID,
                  INITIATIVE_NAME,
                  TEST_DATE_ONLY_DATE,
                  INITIATIVE_ID,
                  ORGANIZATION_NAME,
                  NotificationConstants.STATUS_ONBOARDING_OK,
                  TEST_DATE,
                  TEST_DATE,
                  List.of(),
                  50000L,
                  1L,
                  true,
                  null,
                  IO
          );

  private static final EvaluationDTO EVALUATION_DTO_TYPE2 =
          new EvaluationDTO(
                  USER_ID,
                  INITIATIVE_ID,
                  "test bonus",
                  TEST_DATE_ONLY_DATE,
                  INITIATIVE_ID,
                  "COMUNE DI GUIDONIA MONTECELIO",
                  NotificationConstants.STATUS_ONBOARDING_OK,
                  TEST_DATE,
                  TEST_DATE,
                  List.of(),
                  50000L,
                  1L,
                  true,
                  null,
                  IO
          );

  private static final EvaluationDTO EVALUATION_DTO_KO_PDND =
          new EvaluationDTO(
                  USER_ID,
                  INITIATIVE_ID,
                  INITIATIVE_ID,
                  TEST_DATE_ONLY_DATE,
                  INITIATIVE_ID,
                  ORGANIZATION_NAME,
                  NotificationConstants.STATUS_ONBOARDING_KO,
                  TEST_DATE,
                  TEST_DATE,
                  List.of(new OnboardingRejectionReason(OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL, OnboardingRejectionReasonCode.AUTOMATED_CRITERIA_ISEE_FAIL, "AUTHORITY", "LABEL", "DETAIL")),
                  50000L,
                  1L,
                  true,
                  null,
                  IO
          );

  private static final EvaluationDTO EVALUATION_DTO_KO_RANKING =
          new EvaluationDTO(
                  USER_ID,
                  INITIATIVE_ID,
                  INITIATIVE_ID,
                  TEST_DATE_ONLY_DATE,
                  INITIATIVE_ID,
                  ORGANIZATION_NAME,
                  NotificationConstants.STATUS_ONBOARDING_KO,
                  TEST_DATE,
                  TEST_DATE,
                  List.of(new OnboardingRejectionReason(OnboardingRejectionReasonType.OUT_OF_RANKING, null, "AUTHORITY", "LABEL", "DETAIL")),
                  50000L,
                  1L,
                  true,
                  null,
                  IO
          );

  private static final EvaluationDTO EVALUATION_DTO_KO_TECH =
          new EvaluationDTO(
                  USER_ID,
                  INITIATIVE_ID,
                  INITIATIVE_ID,
                  TEST_DATE_ONLY_DATE,
                  INITIATIVE_ID,
                  ORGANIZATION_NAME,
                  NotificationConstants.STATUS_ONBOARDING_KO,
                  TEST_DATE,
                  TEST_DATE,
                  List.of(new OnboardingRejectionReason(OnboardingRejectionReasonType.TECHNICAL_ERROR, null, "AUTHORITY", "LABEL", "DETAIL")),
                  50000L,
                  1L,
                  true,
                  null,
                  IO
          );


  @Autowired NotificationMarkdown notificationMarkdown;

  @Test
  void getSubject_status_ok() {
    String actual = notificationMarkdown.getSubject(EVALUATION_DTO);
    assertEquals(SUBJECT_OK, actual);
  }

  @Test
  void getSubjectType2_status_ok() {
    String actual = notificationMarkdown.getSubject(EVALUATION_DTO_TYPE2);
    assertEquals(SUBJECT_OK_TYPE2, actual);
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
    String expectedMarkdownOk = MARKDOWN_OK
            .formatted(EVALUATION_DTO.getInitiativeId(),
                    EVALUATION_DTO.getInitiativeId(),
                    EVALUATION_DTO.getInitiativeName());
    String actual = notificationMarkdown.getMarkdown(EVALUATION_DTO);

    Assertions.assertEquals(expectedMarkdownOk, actual);
  }

  @Test
  void getMarkdownType2_status_ok() {
      String actual = notificationMarkdown.getMarkdown(EVALUATION_DTO_TYPE2);

    Assertions.assertEquals(MARKDOWN_OK_TYPE2, actual);
  }

  @Test
  void getMarkdown_status_ko_pdnd() {
    String expectedMarkdown = """
            Purtroppo non è stato possibile aderire a %s per i seguenti motivi:
                        
            * %s
                        
            Se ritieni che ci sia stato un errore puoi segnalarlo direttamente all'Ente erogatore dell'iniziativa.
                        
            Ci scusiamo per il disagio."""
            .formatted(
                    EVALUATION_DTO_KO_TECH.getInitiativeName(),
                    EVALUATION_DTO_KO_PDND.getOnboardingRejectionReasons().get(0).getDetail());
    String actual = notificationMarkdown.getMarkdown(EVALUATION_DTO_KO_PDND);
    Assertions.assertEquals(expectedMarkdown, actual);
  }

  @Test
  void getMarkdown_status_ko_ranking() {
    String expectedMarkdownRankingKo = """
            Purtroppo non è stato possibile attivare %s in quanto i tuoi requisiti non rientrano nella graduatoria.
                        
            Se ritieni che ci sia stato un errore puoi segnalarlo direttamente all'Ente erogatore dell'iniziativa.
                        
            Ci scusiamo per il disagio.""".formatted(EVALUATION_DTO_KO_RANKING.getInitiativeName());

    String actual = notificationMarkdown.getMarkdown(EVALUATION_DTO_KO_RANKING);

    Assertions.assertEquals(expectedMarkdownRankingKo, actual);
  }

  @Test
  void getMarkdown_status_ko_tech() {
    String expectedMarkdown = """
            Si è verificato un errore nel processare la tua richiesta di %s.
            Se ritieni che ci sia stato un errore puoi segnalarlo direttamente all'Ente erogatore dell'iniziativa.
                        
            Ci scusiamo per il disagio."""
            .formatted(EVALUATION_DTO_KO_TECH.getInitiativeName());

    String actual = notificationMarkdown.getMarkdown(EVALUATION_DTO_KO_TECH);
    Assertions.assertEquals(expectedMarkdown, actual);
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
  void getSubject_demanded(){
    EvaluationDTO evaluationDto = getEvaluationDto(NotificationConstants.STATUS_ONBOARDING_DEMANDED, null);
    String expectedSubject = "Attiva il bonus "+evaluationDto.getInitiativeName();

    String subjectDemanded = notificationMarkdown.getSubject(evaluationDto);

    Assertions.assertNotNull(subjectDemanded);
    Assertions.assertEquals(expectedSubject, subjectDemanded);
  }

  @Test
  void getMarkdown_demanded(){
    EvaluationDTO evaluationDto = getEvaluationDto(NotificationConstants.STATUS_ONBOARDING_DEMANDED, null);
    String expectedMarkdown = "Buone notizie! Grazie ad un tuo familiare puoi ottenere %s.\n\n".formatted(evaluationDto.getInitiativeName())+
            "Aderisci all’iniziativa per usufruire del bonus.\n\n"+
            "Puoi trovare maggiori informazioni sul [sito](http://example.com/).";

    String markdownDemanded = notificationMarkdown.getMarkdown(evaluationDto);

    Assertions.assertNotNull(markdownDemanded);
    Assertions.assertEquals(expectedMarkdown, markdownDemanded);

  }

  @Test
  void getSubject_joined(){
    EvaluationDTO evaluationDto = getEvaluationDto(NotificationConstants.STATUS_ONBOARDING_JOINED, null);
    String expectedSubject = "Il tuo Bonus è attivo";

    String subjectJoined = notificationMarkdown.getSubject(evaluationDto);
    Assertions.assertEquals(expectedSubject, subjectJoined);
  }

  @Test
  void getMarkdown_joined(){
    EvaluationDTO evaluationDto = getEvaluationDto(NotificationConstants.STATUS_ONBOARDING_JOINED, null);
    String markdownExpectedJoined = MARKDOWN_OK
            .formatted(evaluationDto.getInitiativeId(),
                    evaluationDto.getInitiativeId(),
                    evaluationDto.getInitiativeName());

    String markdownJoined = notificationMarkdown.getMarkdown(evaluationDto);

    Assertions.assertEquals(markdownExpectedJoined, markdownJoined);
  }

  @Test
  void getSubject_budgetExhausted(){
    EvaluationDTO evaluationDto = getEvaluationDto(NotificationConstants.STATUS_ONBOARDING_KO,
            List.of(OnboardingRejectionReason.builder()
                    .type(OnboardingRejectionReasonType.BUDGET_EXHAUSTED)
                    .code(OnboardingRejectionReasonCode.INITIATIVE_BUDGET_EXHAUSTED)
                    .build()));

    String expectedSubject = "Non è stato possibile attivare %s".formatted(evaluationDto.getInitiativeName());

    String actual = notificationMarkdown.getSubject(evaluationDto);
    Assertions.assertEquals(expectedSubject, actual);
  }

  @Test
  void getMarkdown_budgetExhausted(){

    EvaluationDTO evaluationDto = getEvaluationDto(NotificationConstants.STATUS_ONBOARDING_KO,
            List.of(OnboardingRejectionReason.builder()
                    .type(OnboardingRejectionReasonType.BUDGET_EXHAUSTED)
                    .code(OnboardingRejectionReasonCode.INITIATIVE_BUDGET_EXHAUSTED)
                    .build()));

    String expectedMarkdown = """
            Purtroppo non è stato possibile attivare %s in quanto è terminato il budget disponibile.
                        
            Ci scusiamo per il disagio."""
            .formatted(evaluationDto.getInitiativeName());

    String actual = notificationMarkdown.getMarkdown(evaluationDto);
    Assertions.assertEquals(expectedMarkdown, actual);
  }
  @Test
  void getSubject_notRetrieveDataPDND(){
    EvaluationDTO evaluationDto = getEvaluationDto(NotificationConstants.STATUS_ONBOARDING_KO,
            List.of(OnboardingRejectionReason.builder()
                    .type(OnboardingRejectionReasonType.ISEE_TYPE_KO)
                    .code(OnboardingRejectionReasonCode.ISEE_TYPE_FAIL)
                    .authority("INPS")
                    .authorityLabel("Istituto Nazionale Previdenza Sociale")
                    .detail("ISEE non disponibile").build()));

    String expectedSubject = "Non è stato possibile attivare %s".formatted(evaluationDto.getInitiativeName());

    String actual = notificationMarkdown.getSubject(evaluationDto);
    Assertions.assertEquals(expectedSubject, actual);
  }

  @Test
  void getMarkdown_notRetrieveDataPDND(){
    OnboardingRejectionReason IseeTypeRejection = OnboardingRejectionReason.builder()
            .type(OnboardingRejectionReasonType.ISEE_TYPE_KO)
            .code(OnboardingRejectionReasonCode.ISEE_TYPE_FAIL)
            .authority("INPS")
            .authorityLabel("Istituto Nazionale Previdenza Sociale")
            .detail("ISEE non disponibile").build();
    EvaluationDTO evaluationDto = getEvaluationDto(NotificationConstants.STATUS_ONBOARDING_KO,
            List.of(IseeTypeRejection));

    String expectedMarkdown = """
            Purtroppo non hai i requisiti necessari per aderire a %s per i seguenti motivi:
                        
            * %s : %s
                        
            Se ritieni che ci sia stato un errore puoi segnalarlo direttamente all'Ente erogatore dell'iniziativa.
                        
            Ci scusiamo per il disagio."""
            .formatted(evaluationDto.getInitiativeName(),
                    IseeTypeRejection.getAuthorityLabel(), IseeTypeRejection.getDetail());

    String actual = notificationMarkdown.getMarkdown(evaluationDto);
    Assertions.assertEquals(expectedMarkdown, actual);
  }

  private EvaluationDTO getEvaluationDto(String status, List<OnboardingRejectionReason> rejectionReasons){
    return new EvaluationDTO(
            USER_ID,
            INITIATIVE_ID,
            INITIATIVE_NAME,
            TEST_DATE_ONLY_DATE,
            "ORGANIZATIONID",
            ORGANIZATION_NAME,
            status,
            TEST_DATE,
            TEST_DATE,
            rejectionReasons,
            50000L,
            1L,
            true,
            null,
            IO
    );

  }

}
