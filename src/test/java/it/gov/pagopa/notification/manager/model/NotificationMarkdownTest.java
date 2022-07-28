package it.gov.pagopa.notification.manager.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ExtendWith({SpringExtension.class})
@ContextConfiguration(classes = NotificationMarkdown.class)
@TestPropertySource(
    properties = {
      "notification.manager.markdown.double.new.line=\\n\\n",
      "notification.manager.subject.ok=Il tuo Bonus è attivo",
      "notification.manager.subject.ko=Non è stato possibile attivare %initiativeName%",
      "notification.manager.subject.ko.tech=Abbiamo riscontrato dei problemi",
      "notification.manager.markdown.ok=Buone notizie! Hai ottenuto %initiativeName%. Da questo momento puoi visualizzare il bonus nella sezione Portafoglio dell'app IO.\\n\\nTi ricordiamo che per iniziare ad usufruire del bonus devi configurare almeno un metodo di pagamento.\\n\\nPuoi trovare maggiori informazioni sul [sito](http://example.com/).",
      "notification.manager.markdown.ko.pdnd=Purtroppo non hai i requisiti necessari per aderire a %initiativeName% per i seguenti motivi:",
      "notification.manager.markdown.ko.ranking=Purtroppo non è stato possibile attivare %initiativeName% in quanto i tuoi requisiti non rientrano nella graduatoria.",
      "notification.manager.markdown.ko.mistake=Se ritieni che ci sia stato un errore puoi segnalarlo direttamente all'Ente erogatore dell'iniziativa.",
      "notification.manager.markdown.ko.tech=Si è verificato un errore nel processare la tua richiesta di %initiativeName%.\\nTi chiediamo di riprovare.",
      "notification.manager.markdown.ko.apology=Ci scusiamo per il disagio."
    })
class NotificationMarkdownTest {

  private static final String SUBJECT_OK = "Il tuo Bonus è attivo";
  private static final String SUBJECT_KO = "Non è stato possibile attivare Iniziativa di test";
  private static final String SUBJECT_KO_TECH = "Abbiamo riscontrato dei problemi";
  private static final EvaluationDTO EVALUTATION_DTO =
      new EvaluationDTO(
          "TEST_TOKEN", "Iniziativa di test", "ONBOARDING_OK", LocalDateTime.now(), null);
  private static final EvaluationDTO EVALUTATION_DTO_KO_PDND =
      new EvaluationDTO(
          "TEST_TOKEN",
          "Iniziativa di test",
          "ONBOARDING_KO",
          LocalDateTime.now(),
          List.of(NotificationConstants.AUTOMATED_CRITERIA + "_ISEE_FAIL"));

  private static final EvaluationDTO EVALUTATION_DTO_KO_RANKING =
      new EvaluationDTO(
          "TEST_TOKEN",
          "Iniziativa di test",
          "ONBOARDING_KO",
          LocalDateTime.now(),
          List.of(NotificationConstants.RANKING_FAIL));

  private static final EvaluationDTO EVALUTATION_DTO_KO_TECH =
      new EvaluationDTO(
          "TEST_TOKEN",
          "Iniziativa di test",
          "ONBOARDING_KO",
          LocalDateTime.now(),
          List.of("TECH_FAIL"));

  @Autowired NotificationMarkdown notificationMarkdown;

  @Test
  void getSubject_status_ok() {
    String actual = notificationMarkdown.getSubject(EVALUTATION_DTO);
    assertEquals(SUBJECT_OK, actual);
  }

  @Test
  void getSubject_status_ko_pdnd() {
    String actual = notificationMarkdown.getSubject(EVALUTATION_DTO_KO_PDND);
    assertEquals(SUBJECT_KO, actual);
  }

  @Test
  void getSubject_status_ko_ranking() {
    String actual = notificationMarkdown.getSubject(EVALUTATION_DTO_KO_RANKING);
    assertEquals(SUBJECT_KO, actual);
  }

  @Test
  void getSubject_status_ko_tech() {
    String actual = notificationMarkdown.getSubject(EVALUTATION_DTO_KO_TECH);
    assertEquals(SUBJECT_KO_TECH, actual);
  }

  @Test
  void getMarkdown_status_ok() {
    String actual = notificationMarkdown.getMarkdown(EVALUTATION_DTO);
    log.info(actual);
  }

  @Test
  void getMarkdown_status_ko_pdnd() {
    String actual = notificationMarkdown.getMarkdown(EVALUTATION_DTO_KO_PDND);
    log.info(actual);
  }

  @Test
  void getMarkdown_status_ko_ranking() {
    String actual = notificationMarkdown.getMarkdown(EVALUTATION_DTO_KO_RANKING);
    log.info(actual);
  }

  @Test
  void getMarkdown_status_ko_tech() {
    String actual = notificationMarkdown.getMarkdown(EVALUTATION_DTO_KO_TECH);
    log.info(actual);
  }
}
