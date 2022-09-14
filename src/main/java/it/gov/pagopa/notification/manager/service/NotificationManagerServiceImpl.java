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
import it.gov.pagopa.notification.manager.dto.mapper.EvaluationDTOToNotificationMapper;
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
      NotificationMarkdown notificationMarkdown,
      NotificationQueueDTOToNotificationMapper notificationQueueDTOToNotificationMapper) {
    this.outcomeProducer = outcomeProducer;
    this.ioBackEndRestConnector = ioBackEndRestConnector;
    this.pdvDecryptRestConnector = pdvDecryptRestConnector;
    this.notificationManagerRepository = notificationManagerRepository;
    this.notificationDTOMapper = notificationDTOMapper;
    this.evaluationDTOToNotificationMapper = evaluationDTOToNotificationMapper;
    this.notificationMarkdown = notificationMarkdown;
    this.notificationQueueDTOToNotificationMapper = notificationQueueDTOToNotificationMapper;
  }

  @Override
  public void notify(EvaluationDTO evaluationDTO) {
    String fiscalCode = decryptUserToken(evaluationDTO.getUserId());
    if (isSenderAllowed(fiscalCode, "api-key")) {

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
        NotificationResource notificationResource = ioBackEndRestConnector.notify(notificationDTO, "api-key");
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

  private boolean isSenderAllowed(String fiscalCode, String primaryKey) {
    try {
      ProfileResource profileResource = ioBackEndRestConnector.getProfile(fiscalCode,primaryKey);
      return profileResource.isSenderAllowed();
    } catch (FeignException e) {
      return false;
    }
  }

  private ServiceResource getService(String serviceId){
    try{
      return ioBackEndRestConnector.getService(serviceId);
    } catch (FeignException e) {
    log.error("[%d] Cannot send request: %s".formatted(e.status(), e.contentUTF8()));
    return null;
  }
  }

  @Override
  public void checkIbanKo(NotificationQueueDTO notificationQueueDTO) {
    String fiscalCode = decryptUserToken(notificationQueueDTO.getUserId());
    ServiceResource serviceResource = getService(notificationQueueDTO.getServiceId());

    if(serviceResource == null){
      return;
    }

    if (isSenderAllowed(fiscalCode,serviceResource.getPrimary_key())) {
      Notification notification = notificationQueueDTOToNotificationMapper.map(
          notificationQueueDTO);

      String subject = notificationMarkdown.getSubjectCheckIbanKo();
      String markdown = "notificationMarkdown.getMarkdown(evaluationDTO)";
      try {
        NotificationDTO notificationDTO =
            notificationDTOMapper.map(
                fiscalCode,
                timeToLive,
                subject,
                markdown);
        notification.setNotificationCheckIbanStatus("OK");

        NotificationResource notificationResource = ioBackEndRestConnector.notify(notificationDTO,serviceResource.getPrimary_key());
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










