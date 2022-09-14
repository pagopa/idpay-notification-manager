package it.gov.pagopa.notification.manager.service;

import feign.FeignException;
import it.gov.pagopa.notification.manager.connector.IOBackEndRestConnector;
import it.gov.pagopa.notification.manager.connector.PdvDecryptRestConnector;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationCheckIbanDTO;
import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationQueueDTO;
import it.gov.pagopa.notification.manager.dto.NotificationResource;
import it.gov.pagopa.notification.manager.dto.ProfileResource;
import it.gov.pagopa.notification.manager.dto.ServiceResource;
import it.gov.pagopa.notification.manager.dto.mapper.EvaluationDTOToNotificationMapper;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationCheckibanMapper;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationDTOMapper;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationQueueDTOToNotificationMapper;
import it.gov.pagopa.notification.manager.event.producer.OutcomeProducer;
import it.gov.pagopa.notification.manager.model.Notification;
import it.gov.pagopa.notification.manager.model.NotificationMarkdown;
import it.gov.pagopa.notification.manager.repository.NotificationManagerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationManagerServiceImpl implements NotificationManagerService {

  private final OutcomeProducer outcomeProducer;
  private final IOBackEndRestConnector ioBackEndRestConnector;
  private final NotificationManagerRepository notificationManagerRepository;
  private final NotificationDTOMapper notificationDTOMapper;
  private final PdvDecryptRestConnector pdvDecryptRestConnector;
  private final EvaluationDTOToNotificationMapper evaluationDTOToNotificationMapper;
  private final NotificationCheckibanMapper notificationCheckibanMapper;
  private final NotificationMarkdown notificationMarkdown;
  private final NotificationQueueDTOToNotificationMapper notificationQueueDTOToNotificationMapper;
  @Value("${notification.backend-io.ttl}")
  private Long timeToLive;

  public NotificationManagerServiceImpl(
      OutcomeProducer outcomeProducer,
      IOBackEndRestConnector ioBackEndRestConnector,
      PdvDecryptRestConnector pdvDecryptRestConnector,
      NotificationManagerRepository notificationManagerRepository,
      NotificationDTOMapper notificationDTOMapper,
      EvaluationDTOToNotificationMapper evaluationDTOToNotificationMapper,
      NotificationCheckibanMapper notificationCheckibanMapper,
      NotificationMarkdown notificationMarkdown,
      NotificationQueueDTOToNotificationMapper notificationQueueDTOToNotificationMapper) {
    this.outcomeProducer = outcomeProducer;
    this.ioBackEndRestConnector = ioBackEndRestConnector;
    this.pdvDecryptRestConnector = pdvDecryptRestConnector;
    this.notificationManagerRepository = notificationManagerRepository;
    this.notificationDTOMapper = notificationDTOMapper;
    this.evaluationDTOToNotificationMapper = evaluationDTOToNotificationMapper;
    this.notificationCheckibanMapper = notificationCheckibanMapper;
    this.notificationMarkdown = notificationMarkdown;
    this.notificationQueueDTOToNotificationMapper = notificationQueueDTOToNotificationMapper;
  }

  @Override
  public void notify(EvaluationDTO evaluationDTO) {
    // chiamata per API Key
    String fiscalCode = decryptUserToken(evaluationDTO.getUserId());
    if (isSenderAllowed(fiscalCode)) {

      Notification notification = evaluationDTOToNotificationMapper.map(evaluationDTO);

      String subject = notificationMarkdown.getSubject(evaluationDTO);
      String markdown = notificationMarkdown.getMarkdown(evaluationDTO);

      NotificationDTO notificationDTO =
          notificationDTOMapper.map(
              fiscalCode,
              timeToLive,
              subject,
              markdown);
      try {
        NotificationResource notificationResource = ioBackEndRestConnector.notify(notificationDTO);
        notification.setNotificationId(notificationResource.getId());
        notification.setNotificationStatus("OK");
        log.info("" + notification.getNotificationId());
      } catch (FeignException e) {
        log.error("[%d] Cannot send notification: %s".formatted(e.status(), e.contentUTF8()));
        notification.setNotificationStatus("KO");
      }
      notificationManagerRepository.save(notification);
      return;
    }
    log.warn("The user is not enabled to receive notifications!");
  }

  @Override
  public void addOutcome(EvaluationDTO evaluationDTO) {
    outcomeProducer.sendOutcome(evaluationDTO);
  }

  private String decryptUserToken(String token) {
    return pdvDecryptRestConnector.getPii(token).getPii();
  }

  private boolean isSenderAllowed(String fiscalCode) {
    try {
      ProfileResource profileResource = ioBackEndRestConnector.getProfile(fiscalCode);
      return profileResource.isSenderAllowed();
    } catch (FeignException e) {
      return false;
    }
  }

  @Override
  public void checkIbanKo(NotificationQueueDTO notificationQueueDTO) {
    String fiscalCode = decryptUserToken(notificationQueueDTO.getUserId());
    if (isSenderAllowed(fiscalCode)) {
      ServiceResource serviceResource = ioBackEndRestConnector.getService(
          notificationQueueDTO.getServiceId());

      Notification notification = notificationQueueDTOToNotificationMapper.map(
          notificationQueueDTO);

      String subject = notificationMarkdown.getSubjectCheckIbanKo();
      String markdown = "notificationMarkdown.getMarkdown(evaluationDTO)";
      try {
        NotificationCheckIbanDTO notificationCheckIbanDTO = notificationCheckibanMapper.map(
            notificationQueueDTO.getInitiativeId(), fiscalCode, serviceResource.getPrimary_key(),
            subject, markdown);
        notificationCheckIbanDTO.setNotificationId(notification.getNotificationId());
        notification.setNotificationCheckIbanStatus("OK");

        ioBackEndRestConnector.notifyCheckIbanKo(notificationCheckIbanDTO);

      } catch (FeignException e) {
        log.error("[%d] Cannot send notification: %s".formatted(e.status(), e.contentUTF8()));
        notification.setNotificationCheckIbanStatus("KO");
      }
      notificationManagerRepository.save(notification);

    }
    log.warn("The user is not enabled to receive notifications!");

  }
}










