package it.gov.pagopa.notification.manager.service;

import feign.FeignException;
import it.gov.pagopa.notification.manager.connector.IOBackEndRestConnector;
import it.gov.pagopa.notification.manager.connector.PdvDecryptRestConnector;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationQueueDTO;
import it.gov.pagopa.notification.manager.dto.NotificationResource;
import it.gov.pagopa.notification.manager.dto.ProfileResource;
import it.gov.pagopa.notification.manager.dto.ServiceResource;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationDTOMapper;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationMapper;
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
  private final NotificationMapper notificationMapper;
  private final NotificationMarkdown notificationMarkdown;

  @Value("${notification.backend-io.ttl}")
  private Long timeToLive;

  public NotificationManagerServiceImpl(
      OutcomeProducer outcomeProducer,
      IOBackEndRestConnector ioBackEndRestConnector,
      PdvDecryptRestConnector pdvDecryptRestConnector,
      NotificationManagerRepository notificationManagerRepository,
      NotificationDTOMapper notificationDTOMapper,
      NotificationMapper notificationMapper,
      NotificationMarkdown notificationMarkdown) {
    this.outcomeProducer = outcomeProducer;
    this.ioBackEndRestConnector = ioBackEndRestConnector;
    this.pdvDecryptRestConnector = pdvDecryptRestConnector;
    this.notificationManagerRepository = notificationManagerRepository;
    this.notificationDTOMapper = notificationDTOMapper;
    this.notificationMapper = notificationMapper;
    this.notificationMarkdown = notificationMarkdown;
  }

  @Override
  public void notify(EvaluationDTO evaluationDTO) {
    ServiceResource serviceResource = getService(evaluationDTO.getServiceId());

    if (serviceResource == null) {
      return;
    }

    String fiscalCode = decryptUserToken(evaluationDTO.getUserId());

    if (isSenderAllowed(fiscalCode, serviceResource.getPrimaryKey())) {

      Notification notification = notificationMapper.evaluationToNotification(evaluationDTO);

      String subject = notificationMarkdown.getSubject(evaluationDTO);
      String markdown = notificationMarkdown.getMarkdown(evaluationDTO);

      NotificationDTO notificationDTO =
          notificationDTOMapper.map(fiscalCode, timeToLive, subject, markdown);
      try {
        NotificationResource notificationResource =
            ioBackEndRestConnector.notify(notificationDTO, serviceResource.getPrimaryKey());
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
    log.error("The user is not enabled to receive notifications!");
  }

  @Override
  public void addOutcome(EvaluationDTO evaluationDTO) {
    outcomeProducer.sendOutcome(evaluationDTO);
  }

  private String decryptUserToken(String token) {
    return pdvDecryptRestConnector.getPii(token).getPii();
  }

  private boolean isSenderAllowed(String fiscalCode, String primaryKey) {
    try {
      ProfileResource profileResource = ioBackEndRestConnector.getProfile(fiscalCode, primaryKey);
      return profileResource.isSenderAllowed();
    } catch (FeignException e) {
      return false;
    }
  }

  private ServiceResource getService(String serviceId) {
    try {
      return ioBackEndRestConnector.getService(serviceId);
    } catch (FeignException e) {
      log.error("[%d] Cannot send request: %s".formatted(e.status(), e.contentUTF8()));
      return null;
    }
  }

  @Override
  public void checkIbanKo(NotificationQueueDTO notificationQueueDTO) {
    ServiceResource serviceResource = getService(notificationQueueDTO.getServiceId());

    if (serviceResource == null) {
      return;
    }

    String fiscalCode = decryptUserToken(notificationQueueDTO.getUserId());

    if (isSenderAllowed(fiscalCode, serviceResource.getPrimaryKey())) {
      Notification notification =
          notificationMapper.queueToNotification(notificationQueueDTO);

      String subject = notificationMarkdown.getSubjectCheckIbanKo();
      String markdown = "notificationMarkdown.getMarkdown(evaluationDTO)";
      try {
        NotificationDTO notificationDTO =
            notificationDTOMapper.map(fiscalCode, timeToLive, subject, markdown);
        notification.setNotificationCheckIbanStatus("OK");

        NotificationResource notificationResource =
            ioBackEndRestConnector.notify(notificationDTO, serviceResource.getPrimaryKey());
        notification.setNotificationId(notificationResource.getId());

      } catch (FeignException e) {
        log.error("[%d] Cannot send notification: %s".formatted(e.status(), e.contentUTF8()));
        notification.setNotificationCheckIbanStatus("KO");
      }
      notificationManagerRepository.save(notification);
    }
    log.warn("The user is not enabled to receive notifications!");
  }
}
