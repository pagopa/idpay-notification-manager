package it.gov.pagopa.notification.manager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.notification.manager.connector.IOBackEndRestConnector;
import it.gov.pagopa.notification.manager.connector.PdvDecryptRestConnector;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.FiscalCodeResource;
import it.gov.pagopa.notification.manager.dto.MessageContent;
import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationResource;
import it.gov.pagopa.notification.manager.dto.ProfileResource;
import it.gov.pagopa.notification.manager.dto.mapper.EvaluationDTOToNotificationMapper;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationDTOMapper;
import it.gov.pagopa.notification.manager.event.producer.OutcomeProducer;
import it.gov.pagopa.notification.manager.model.Notification;
import it.gov.pagopa.notification.manager.model.NotificationMarkdown;
import it.gov.pagopa.notification.manager.repository.NotificationManagerRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = NotificationManagerServiceImpl.class)
@TestPropertySource(properties = {"notification.backend-io.ttl=3600"})
class NotificationManagerServiceTest {

  private static final EvaluationDTO EVALUTATION_DTO =
      new EvaluationDTO("TEST_TOKEN", "",  "", LocalDateTime.now(), null);

  private static final String TEST_TOKEN = "TEST_TOKEN";
  private static final FiscalCodeResource FISCAL_CODE_RESOURCE = new FiscalCodeResource();
  private static final ProfileResource PROFILE_RESOURCE = new ProfileResource();

  private static final String FISCAL_CODE = "TEST_FISCAL_CODE";
  private static final String TEST_NOTIFICATION_ID = "NOTIFICATION_ID";
  private static final Long TTL = 3600L;
  private static final String SUBJECT = "SUBJECT";
  private static final String MARKDOWN = "MARKDOWN";
  private static final NotificationResource NOTIFICATION_RESOURCE = new NotificationResource();
  private static final NotificationDTO NOTIFICATION_DTO = new NotificationDTO();
  private static final Notification NOTIFICATION =
      Notification.builder()
          .notificationDate(LocalDateTime.now())
          .initiativeId(EVALUTATION_DTO.getInitiativeId())
          .userId(EVALUTATION_DTO.getUserId())
          .onboardingOutcome(EVALUTATION_DTO.getStatus())
          .rejectReasons(EVALUTATION_DTO.getOnboardingRejectionReasons())
          .build();

  static {
    FISCAL_CODE_RESOURCE.setPii(FISCAL_CODE);
    PROFILE_RESOURCE.setSenderAllowed(true);
    NOTIFICATION_RESOURCE.setId(TEST_NOTIFICATION_ID);

    MessageContent messageContent = new MessageContent();
    messageContent.setSubject(SUBJECT);
    messageContent.setMarkdown(MARKDOWN);
    NOTIFICATION_DTO.setFiscalCode(FISCAL_CODE);
    NOTIFICATION_DTO.setTimeToLive(TTL);
    NOTIFICATION_DTO.setContent(messageContent);
  }

  @Autowired NotificationManagerService notificationManagerService;

  @MockBean OutcomeProducer outcomeProducer;
  @MockBean NotificationDTOMapper notificationDTOMapper;
  @MockBean EvaluationDTOToNotificationMapper evaluationDTOToNotificationMapper;
  @MockBean NotificationManagerRepository notificationManagerRepository;
  @MockBean PdvDecryptRestConnector pdvDecryptRestConnector;
  @MockBean IOBackEndRestConnector ioBackEndRestConnector;
  @MockBean NotificationMarkdown notificationMarkdown;

  @Test
  void sendToQueue() {
    Mockito.doNothing().when(outcomeProducer).sendOutcome(EVALUTATION_DTO);

    notificationManagerService.addOutcome(EVALUTATION_DTO);

    Mockito.verify(outcomeProducer, Mockito.times(1)).sendOutcome(EVALUTATION_DTO);
  }

  @Test
  void notify_ok() {
    Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
    Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE)).thenReturn(PROFILE_RESOURCE);
    Mockito.when(notificationMarkdown.getSubject(EVALUTATION_DTO)).thenReturn(SUBJECT);
    Mockito.when(notificationMarkdown.getMarkdown(EVALUTATION_DTO)).thenReturn(MARKDOWN);
    Mockito.when(
            notificationDTOMapper.map(
                Mockito.eq(FISCAL_CODE),
                Mockito.any(Long.class),
                Mockito.anyString(),
                Mockito.anyString()))
        .thenReturn(NOTIFICATION_DTO);
    Mockito.when(evaluationDTOToNotificationMapper.map(EVALUTATION_DTO)).thenReturn(NOTIFICATION);
    Mockito.when(ioBackEndRestConnector.notify(NOTIFICATION_DTO)).thenReturn(NOTIFICATION_RESOURCE);
    try {
      notificationManagerService.notify(EVALUTATION_DTO);
    } catch (FeignException e) {
      Assertions.fail();
    }

    Mockito.verify(notificationManagerRepository, Mockito.times(1))
        .save(Mockito.any(Notification.class));
  }

  @Test
  void notify_ko() {
    Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
    Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE)).thenReturn(PROFILE_RESOURCE);
    Mockito.when(notificationMarkdown.getSubject(EVALUTATION_DTO)).thenReturn(SUBJECT);
    Mockito.when(notificationMarkdown.getMarkdown(EVALUTATION_DTO)).thenReturn(MARKDOWN);
    Mockito.when(
            notificationDTOMapper.map(
                Mockito.eq(FISCAL_CODE),
                Mockito.any(Long.class),
                Mockito.anyString(),
                Mockito.anyString()))
        .thenReturn(NOTIFICATION_DTO);
    Mockito.when(evaluationDTOToNotificationMapper.map(EVALUTATION_DTO)).thenReturn(NOTIFICATION);
    Request request =
        Request.create(
            Request.HttpMethod.POST, "url", new HashMap<>(), null, new RequestTemplate());
    Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
        .when(ioBackEndRestConnector)
        .notify(NOTIFICATION_DTO);

    try {
      notificationManagerService.notify(EVALUTATION_DTO);
    } catch (FeignException e) {
      assertEquals(HttpStatus.BAD_REQUEST.value(), e.status());
    }

    Mockito.verify(notificationManagerRepository, Mockito.times(1))
        .save(Mockito.any(Notification.class));
  }

  @Test
  void notify_ko_user_not_allowed() {
    Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
    Request request =
        Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
    Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
        .when(ioBackEndRestConnector)
        .getProfile(FISCAL_CODE);

    notificationManagerService.notify(EVALUTATION_DTO);

    Mockito.verify(notificationManagerRepository, Mockito.times(0))
        .save(Mockito.any(Notification.class));
  }
}
