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

  @Value("${rest-client.notification.backend-io.ttl}")
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
    log.info(
        "[NOTIFY] Sending request to IO getService with serviceId {}",
        evaluationDTO.getServiceId());
    Notification notification = notificationMapper.evaluationToNotification(evaluationDTO);
    ServiceResource serviceResource = getService(evaluationDTO.getServiceId());

    if (serviceResource == null) {
      notificationKO(notification);
      return;
    }

    log.info("[NOTIFY] Sending request to pdv");
    String fiscalCode = decryptUserToken(evaluationDTO.getUserId());

    if (fiscalCode == null) {
      notificationKO(notification);
      return;
    }

    if (isNotSenderAllowed(fiscalCode, serviceResource.getPrimaryKey())) {
      notificationKO(notification);
      return;
    }

    String subject = notificationMarkdown.getSubject(evaluationDTO);
    String markdown = notificationMarkdown.getMarkdown(evaluationDTO);

    NotificationDTO notificationDTO =
        notificationDTOMapper.map(fiscalCode, timeToLive, subject, markdown);

    String notificationId = sendNotification(notificationDTO, serviceResource.getPrimaryKey());

    if (notificationId == null) {
      notificationKO(notification);
      return;
    }

    log.info("[NOTIFY] Notification ID: {}", notification.getNotificationId());

    notification.setNotificationId(notificationId);
    notification.setNotificationStatus("OK");
    notificationManagerRepository.save(notification);
  }

  private void notificationKO(Notification notification) {
    notification.setNotificationStatus("KO");
    notificationManagerRepository.save(notification);
  }

  @Override
  public void addOutcome(EvaluationDTO evaluationDTO) {
    outcomeProducer.sendOutcome(evaluationDTO);
  }

  private String sendNotification(NotificationDTO notificationDTO, String primaryKey) {
    try {
      NotificationResource notificationResource =
          ioBackEndRestConnector.notify(notificationDTO, primaryKey);
      return notificationResource.getId();
    } catch (FeignException e) {
      log.error("[NOTIFY] [{}] Cannot send notification: {}", e.status(), e.contentUTF8());
      return null;
    }
  }

  private String decryptUserToken(String token) {
    try {
      return pdvDecryptRestConnector.getPii(token).getPii();
    } catch (FeignException e) {
      return null;
    }
  }

  private boolean isNotSenderAllowed(String fiscalCode, String primaryKey) {
    try {
      ProfileResource profileResource = ioBackEndRestConnector.getProfile(fiscalCode, primaryKey);
      return !profileResource.isSenderAllowed();
    } catch (FeignException e) {
      log.error("[NOTIFY] The user is not enabled to receive notifications!");
      return true;
    }
  }

  private ServiceResource getService(String serviceId) {
    try {
      return ioBackEndRestConnector.getService(serviceId);
    } catch (FeignException e) {
      log.error("[NOTIFY] [%d] Cannot send request: %s".formatted(e.status(), e.contentUTF8()));
      return null;
    }
  }

  @Override
  public void checkIbanKo(NotificationQueueDTO notificationQueueDTO) {
    log.info(
        "[NOTIFY] Sending request to IO getService with serviceId {}",
        notificationQueueDTO.getServiceId());
    Notification notification = notificationMapper.queueToNotification(notificationQueueDTO);
    ServiceResource serviceResource = getService(notificationQueueDTO.getServiceId());

    if (serviceResource == null) {
      notificationKO(notification);
      return;
    }

    log.info("[NOTIFY] Sending request to pdv");
    String fiscalCode = decryptUserToken(notificationQueueDTO.getUserId());

    if (fiscalCode == null) {
      notificationKO(notification);
      return;
    }

    if (isNotSenderAllowed(fiscalCode, serviceResource.getPrimaryKey())) {
      notificationKO(notification);
      return;
    }

    String subject = notificationMarkdown.getSubjectCheckIbanKo();
    String markdown = notificationMarkdown.getMarkdownCheckIbanKo();

    NotificationDTO notificationDTO =
        notificationDTOMapper.map(fiscalCode, timeToLive, subject, markdown);

    String notificationId = sendNotification(notificationDTO, serviceResource.getPrimaryKey());

    if (notificationId == null) {
      notificationKO(notification);
      return;
    }

    log.info("[NOTIFY] Notification ID: {}", notification.getNotificationId());

    notification.setNotificationId(notificationId);
    notification.setNotificationStatus("OK");
    notificationManagerRepository.save(notification);
  }
}
