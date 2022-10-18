package it.gov.pagopa.notification.manager.service;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.notification.manager.connector.IOBackEndRestConnector;
import it.gov.pagopa.notification.manager.connector.PdvDecryptRestConnector;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.*;
import it.gov.pagopa.notification.manager.dto.event.NotificationIbanQueueDTO;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeAdditionalInfoDTO;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationDTOMapper;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationMapper;
import it.gov.pagopa.notification.manager.event.producer.OutcomeProducer;
import it.gov.pagopa.notification.manager.model.Notification;
import it.gov.pagopa.notification.manager.model.NotificationMarkdown;
import it.gov.pagopa.notification.manager.repository.NotificationManagerRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = NotificationManagerServiceImpl.class)
@TestPropertySource(properties = {"rest-client.notification.backend-io.ttl=3600"})
class NotificationManagerServiceTest {

  private static final String TEST_TOKEN = "TEST_TOKEN";
  private static final String INITIATIVE_ID = "INITIATIVE_ID";
  private static final LocalDateTime TEST_DATE = LocalDateTime.now();
  private static final FiscalCodeResource FISCAL_CODE_RESOURCE = new FiscalCodeResource();
  private static final ProfileResource PROFILE_RESOURCE = new ProfileResource();
  private static final ProfileResource PROFILE_RESOURCE_KO = new ProfileResource();

  private static final String FISCAL_CODE = "TEST_FISCAL_CODE";
  private static final String PRIMARY_KEY = "PRIMARY_KEY";
  private static final String SECONDARY_KEY = "SECONDARY_KEY";
  private static final String TEST_NOTIFICATION_ID = "NOTIFICATION_ID";
  private static final Long TTL = 3600L;
  private static final String SUBJECT = "SUBJECT";
  private static final String MARKDOWN = "MARKDOWN";
  private static final String OPERATION_TYPE = "OPERATION_TYPE";
  private static final String SERVICE_ID = "SERVICE_ID";
  private static final String IBAN = "IBAN";

  private static final EvaluationDTO EVALUATION_DTO =
      new EvaluationDTO(
          TEST_TOKEN,
          INITIATIVE_ID,
          INITIATIVE_ID,
          TEST_DATE,
          INITIATIVE_ID,
          NotificationConstants.STATUS_ONBOARDING_OK,
          TEST_DATE,
          List.of(),
          new BigDecimal(500),
          INITIATIVE_ID);
  private static final NotificationResource NOTIFICATION_RESOURCE = new NotificationResource();
  private static final NotificationDTO NOTIFICATION_DTO = new NotificationDTO();
  private static final Notification NOTIFICATION =
      Notification.builder()
          .notificationDate(LocalDateTime.now())
          .initiativeId(EVALUATION_DTO.getInitiativeId())
          .userId(EVALUATION_DTO.getUserId())
          .onboardingOutcome(EVALUATION_DTO.getStatus())
          .rejectReasons(EVALUATION_DTO.getOnboardingRejectionReasons())
          .build();
  private static final ServiceResource SERVICE_RESOURCE = new ServiceResource();
  private static final InitiativeAdditionalInfoDTO INITIATIVE_ADDITIONAL_INFO_DTO = InitiativeAdditionalInfoDTO.builder()
          .primaryTokenIO(PRIMARY_KEY)
          .secondaryTokenIO(SECONDARY_KEY)
          .build();

  private static final NotificationIbanQueueDTO NOTIFICATION_QUEUE_DTO = NotificationIbanQueueDTO.builder()
      .operationType(OPERATION_TYPE)
      .userId(TEST_TOKEN)
      .initiativeId(INITIATIVE_ID)
      .serviceId(SERVICE_ID)
      .iban(IBAN)
      .build();

  static {
    FISCAL_CODE_RESOURCE.setPii(FISCAL_CODE);
    PROFILE_RESOURCE.setSenderAllowed(true);
    PROFILE_RESOURCE_KO.setSenderAllowed(false);
    NOTIFICATION_RESOURCE.setId(TEST_NOTIFICATION_ID);

    MessageContent messageContent = new MessageContent();
    messageContent.setSubject(SUBJECT);
    messageContent.setMarkdown(MARKDOWN);
    NOTIFICATION_DTO.setFiscalCode(FISCAL_CODE);
    NOTIFICATION_DTO.setTimeToLive(TTL);
    NOTIFICATION_DTO.setContent(messageContent);
    SERVICE_RESOURCE.setPrimaryKey(PRIMARY_KEY);
  }

  @Autowired NotificationManagerService notificationManagerService;
  @MockBean OutcomeProducer outcomeProducer;
  @MockBean NotificationDTOMapper notificationDTOMapper;
  @MockBean NotificationMapper notificationMapper;
  @MockBean NotificationManagerRepository notificationManagerRepository;
  @MockBean PdvDecryptRestConnector pdvDecryptRestConnector;
  @MockBean IOBackEndRestConnector ioBackEndRestConnector;
  @MockBean InitiativeRestConnector initiativeRestConnector;
  @MockBean NotificationMarkdown notificationMarkdown;

  @Test
  void sendToQueue() {
    Mockito.doNothing().when(outcomeProducer).sendOutcome(EVALUATION_DTO);

    notificationManagerService.addOutcome(EVALUATION_DTO);

    Mockito.verify(outcomeProducer, Mockito.times(1)).sendOutcome(EVALUATION_DTO);
  }

  @Test
  void notify_ok() {
    Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
    Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE, PRIMARY_KEY))
        .thenReturn(PROFILE_RESOURCE);
//    Mockito.when(ioBackEndRestConnector.getService(EVALUATION_DTO.getServiceId()))
//        .thenReturn(SERVICE_RESOURCE);
    Mockito.when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId()))
        .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
    Mockito.when(notificationMarkdown.getSubject(EVALUATION_DTO)).thenReturn(SUBJECT);
    Mockito.when(notificationMarkdown.getMarkdown(EVALUATION_DTO)).thenReturn(MARKDOWN);
    Mockito.when(
            notificationDTOMapper.map(
                Mockito.eq(FISCAL_CODE),
                Mockito.any(Long.class),
                Mockito.anyString(),
                Mockito.anyString()))
        .thenReturn(NOTIFICATION_DTO);
    Mockito.when(notificationMapper.evaluationToNotification(EVALUATION_DTO))
        .thenReturn(NOTIFICATION);
    Mockito.when(ioBackEndRestConnector.notify(NOTIFICATION_DTO, PRIMARY_KEY))
        .thenReturn(NOTIFICATION_RESOURCE);
    try {
      notificationManagerService.notify(EVALUATION_DTO);
    } catch (FeignException e) {
      Assertions.fail();
    }

    Mockito.verify(notificationManagerRepository, Mockito.times(1))
        .save(Mockito.any(Notification.class));
  }

  @Test
  void notify_ko() {
    Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
    Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE, PRIMARY_KEY))
        .thenReturn(PROFILE_RESOURCE);
//    Mockito.when(ioBackEndRestConnector.getService(EVALUATION_DTO.getServiceId()))
//        .thenReturn(SERVICE_RESOURCE);
    Mockito.when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId()))
            .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
    Mockito.when(notificationMarkdown.getSubject(EVALUATION_DTO)).thenReturn(SUBJECT);
    Mockito.when(notificationMarkdown.getMarkdown(EVALUATION_DTO)).thenReturn(MARKDOWN);
    Mockito.when(
            notificationDTOMapper.map(
                Mockito.eq(FISCAL_CODE),
                Mockito.any(Long.class),
                Mockito.anyString(),
                Mockito.anyString()))
        .thenReturn(NOTIFICATION_DTO);
    Mockito.when(notificationMapper.evaluationToNotification(EVALUATION_DTO))
        .thenReturn(NOTIFICATION);
    Request request =
        Request.create(
            Request.HttpMethod.POST, "url", new HashMap<>(), null, new RequestTemplate());
    Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
        .when(ioBackEndRestConnector)
        .notify(NOTIFICATION_DTO, PRIMARY_KEY);

    try {
      notificationManagerService.notify(EVALUATION_DTO);
    } catch (FeignException e) {
      assertEquals(HttpStatus.BAD_REQUEST.value(), e.status());
    }

    Mockito.verify(notificationManagerRepository, Mockito.times(1))
        .save(Mockito.any(Notification.class));
  }

  @Test
  void notify_ko_no_service_resource() {

    Request request =
        Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
//    Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
//        .when(ioBackEndRestConnector)
//        .getService(EVALUATION_DTO.getServiceId());
    Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
        .when(initiativeRestConnector)
        .getIOTokens(EVALUATION_DTO.getInitiativeId());

    Mockito.when(notificationMapper.evaluationToNotification(EVALUATION_DTO))
        .thenReturn(NOTIFICATION);

    notificationManagerService.notify(EVALUATION_DTO);
    Mockito.verify(notificationManagerRepository, Mockito.times(1))
        .save(Mockito.any(Notification.class));
  }

  @Test
  void notify_ko_user_not_allowed_feign() {
    Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
//    Mockito.when(ioBackEndRestConnector.getService(EVALUATION_DTO.getServiceId()))
//        .thenReturn(SERVICE_RESOURCE);
    Mockito.when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId()))
            .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
    Request request =
        Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
    Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
        .when(ioBackEndRestConnector)
        .getProfile(FISCAL_CODE, PRIMARY_KEY);

    Mockito.when(notificationMapper.evaluationToNotification(EVALUATION_DTO))
        .thenReturn(NOTIFICATION);

    notificationManagerService.notify(EVALUATION_DTO);

    Mockito.verify(notificationManagerRepository, Mockito.times(1))
        .save(Mockito.any(Notification.class));
  }

  @Test
  void notify_ko_user_not_allowed() {
    Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
//    Mockito.when(ioBackEndRestConnector.getService(EVALUATION_DTO.getServiceId()))
//        .thenReturn(SERVICE_RESOURCE);
    Mockito.when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId()))
            .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
    Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE, PRIMARY_KEY))
        .thenReturn(PROFILE_RESOURCE_KO);
    Mockito.when(notificationMapper.evaluationToNotification(EVALUATION_DTO))
        .thenReturn(NOTIFICATION);

    notificationManagerService.notify(EVALUATION_DTO);

    Mockito.verify(notificationManagerRepository, Mockito.times(1))
        .save(Mockito.any(Notification.class));
  }

  @Test
  void notify_ko_no_cf() {
//    Mockito.when(ioBackEndRestConnector.getService(EVALUATION_DTO.getServiceId()))
//        .thenReturn(SERVICE_RESOURCE);
    Mockito.when(initiativeRestConnector.getIOTokens(EVALUATION_DTO.getInitiativeId()))
            .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
    Request request =
        Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
    Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
        .when(pdvDecryptRestConnector)
        .getPii(TEST_TOKEN);

    Mockito.when(notificationMapper.evaluationToNotification(EVALUATION_DTO))
        .thenReturn(NOTIFICATION);

    notificationManagerService.notify(EVALUATION_DTO);

    Mockito.verify(notificationManagerRepository, Mockito.times(1))
        .save(Mockito.any(Notification.class));
  }

  @Test
  void checkIbanKo_ok() {
    Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
    Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE, PRIMARY_KEY))
        .thenReturn(PROFILE_RESOURCE);
//    Mockito.when(ioBackEndRestConnector.getService(NOTIFICATION_QUEUE_DTO.getServiceId()))
//        .thenReturn(SERVICE_RESOURCE);
    Mockito.when(initiativeRestConnector.getIOTokens(NOTIFICATION_QUEUE_DTO.getInitiativeId()))
            .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
    Mockito.when(notificationMarkdown.getSubjectCheckIbanKo()).thenReturn(SUBJECT);
    Mockito.when(notificationMarkdown.getMarkdownCheckIbanKo()).thenReturn(MARKDOWN);
    Mockito.when(
            notificationDTOMapper.map(
                Mockito.eq(FISCAL_CODE),
                Mockito.any(Long.class),
                Mockito.anyString(),
                Mockito.anyString()))
        .thenReturn(NOTIFICATION_DTO);
//    Mockito.when(notificationMapper.queueToNotification(NOTIFICATION_QUEUE_DTO))
//        .thenReturn(NOTIFICATION);
    Mockito.when(notificationMapper.toEntity(NOTIFICATION_QUEUE_DTO))
            .thenReturn(NOTIFICATION);
    Mockito.when(ioBackEndRestConnector.notify(NOTIFICATION_DTO, PRIMARY_KEY))
        .thenReturn(NOTIFICATION_RESOURCE);
    try {
      notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_QUEUE_DTO);
    } catch (FeignException e) {
      Assertions.fail();
    }

    Mockito.verify(notificationManagerRepository, Mockito.times(1))
        .save(Mockito.any(Notification.class));
  }

  @Test
  void checkIbanKo_ko() {
    Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
    Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE, PRIMARY_KEY))
        .thenReturn(PROFILE_RESOURCE);
//    Mockito.when(ioBackEndRestConnector.getService(NOTIFICATION_QUEUE_DTO.getServiceId()))
//        .thenReturn(SERVICE_RESOURCE);
    Mockito.when(initiativeRestConnector.getIOTokens(NOTIFICATION_QUEUE_DTO.getInitiativeId()))
            .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
    Mockito.when(notificationMarkdown.getSubjectCheckIbanKo()).thenReturn(SUBJECT);
    Mockito.when(notificationMarkdown.getMarkdownCheckIbanKo()).thenReturn(MARKDOWN);
    Mockito.when(
            notificationDTOMapper.map(
                Mockito.eq(FISCAL_CODE),
                Mockito.any(Long.class),
                Mockito.anyString(),
                Mockito.anyString()))
        .thenReturn(NOTIFICATION_DTO);
//    Mockito.when(notificationMapper.queueToNotification(NOTIFICATION_QUEUE_DTO))
//        .thenReturn(NOTIFICATION);
    Mockito.when(notificationMapper.toEntity(NOTIFICATION_QUEUE_DTO))
            .thenReturn(NOTIFICATION);
    Request request =
        Request.create(
            Request.HttpMethod.POST, "url", new HashMap<>(), null, new RequestTemplate());
    Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
        .when(ioBackEndRestConnector)
        .notify(NOTIFICATION_DTO, PRIMARY_KEY);

    try {
      notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_QUEUE_DTO);
    } catch (FeignException e) {
      assertEquals(HttpStatus.BAD_REQUEST.value(), e.status());
    }

    Mockito.verify(notificationManagerRepository, Mockito.times(1))
        .save(Mockito.any(Notification.class));
  }

  @Test
  void checkIbanKo_ko_no_service_resource() {
    Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);

    Request request =
        Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
//    Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
//        .when(ioBackEndRestConnector)
//        .getService(NOTIFICATION_QUEUE_DTO.getServiceId());
    Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
            .when(initiativeRestConnector)
            .getIOTokens(NOTIFICATION_QUEUE_DTO.getInitiativeId());

//    Mockito.when(notificationMapper.queueToNotification(NOTIFICATION_QUEUE_DTO))
//        .thenReturn(NOTIFICATION);
    Mockito.when(notificationMapper.toEntity(NOTIFICATION_QUEUE_DTO))
            .thenReturn(NOTIFICATION);

    notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_QUEUE_DTO);
    Mockito.verify(notificationManagerRepository, Mockito.times(1))
        .save(Mockito.any(Notification.class));
  }

  @Test
  void checkIbanKo_ko_user_not_allowed_feign() {
    Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
//    Mockito.when(ioBackEndRestConnector.getService(NOTIFICATION_QUEUE_DTO.getServiceId()))
//        .thenReturn(SERVICE_RESOURCE);
    Mockito.when(initiativeRestConnector.getIOTokens(NOTIFICATION_QUEUE_DTO.getInitiativeId()))
            .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
    Request request =
        Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
    Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
        .when(ioBackEndRestConnector)
        .getProfile(FISCAL_CODE, PRIMARY_KEY);

//    Mockito.when(notificationMapper.queueToNotification(NOTIFICATION_QUEUE_DTO))
//        .thenReturn(NOTIFICATION);
    Mockito.when(notificationMapper.toEntity(NOTIFICATION_QUEUE_DTO))
            .thenReturn(NOTIFICATION);

    notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_QUEUE_DTO);

    Mockito.verify(notificationManagerRepository, Mockito.times(1))
        .save(Mockito.any(Notification.class));
  }

  @Test
  void checkIbanKo_ko_user_not_allowed() {
    Mockito.when(pdvDecryptRestConnector.getPii(TEST_TOKEN)).thenReturn(FISCAL_CODE_RESOURCE);
//    Mockito.when(ioBackEndRestConnector.getService(NOTIFICATION_QUEUE_DTO.getServiceId()))
//        .thenReturn(SERVICE_RESOURCE);
    Mockito.when(initiativeRestConnector.getIOTokens(NOTIFICATION_QUEUE_DTO.getInitiativeId()))
            .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
    Mockito.when(ioBackEndRestConnector.getProfile(FISCAL_CODE, PRIMARY_KEY))
        .thenReturn(PROFILE_RESOURCE_KO);
//    Mockito.when(notificationMapper.queueToNotification(NOTIFICATION_QUEUE_DTO))
//        .thenReturn(NOTIFICATION);
    Mockito.when(notificationMapper.toEntity(NOTIFICATION_QUEUE_DTO))
            .thenReturn(NOTIFICATION);

    notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_QUEUE_DTO);

    Mockito.verify(notificationManagerRepository, Mockito.times(1))
        .save(Mockito.any(Notification.class));
  }

  @Test
  void checkIbanKo_ko_no_cf() {
//    Mockito.when(ioBackEndRestConnector.getService(NOTIFICATION_QUEUE_DTO.getServiceId()))
//        .thenReturn(SERVICE_RESOURCE);
    Mockito.when(initiativeRestConnector.getIOTokens(NOTIFICATION_QUEUE_DTO.getInitiativeId()))
            .thenReturn(INITIATIVE_ADDITIONAL_INFO_DTO);
    Request request =
        Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
    Mockito.doThrow(new FeignException.NotFound("", request, new byte[0], null))
        .when(pdvDecryptRestConnector)
        .getPii(TEST_TOKEN);

//    Mockito.when(notificationMapper.queueToNotification(NOTIFICATION_QUEUE_DTO))
//        .thenReturn(NOTIFICATION);
    Mockito.when(notificationMapper.toEntity(NOTIFICATION_QUEUE_DTO))
        .thenReturn(NOTIFICATION);

    notificationManagerService.sendNotificationFromOperationType(NOTIFICATION_QUEUE_DTO);

    Mockito.verify(notificationManagerRepository, Mockito.times(1))
        .save(Mockito.any(Notification.class));
  }
}
