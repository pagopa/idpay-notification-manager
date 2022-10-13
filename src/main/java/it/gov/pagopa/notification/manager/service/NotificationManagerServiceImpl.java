package it.gov.pagopa.notification.manager.service;

import feign.FeignException;
import it.gov.pagopa.notification.manager.connector.IOBackEndRestConnector;
import it.gov.pagopa.notification.manager.connector.PdvDecryptRestConnector;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationResource;
import it.gov.pagopa.notification.manager.dto.ProfileResource;
import it.gov.pagopa.notification.manager.dto.event.AnyOfNotificationQueueDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationCitizenOnQueueDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationIbanQueueDTO;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeAdditionalInfoDTO;
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
  private final InitiativeRestConnector initiativeRestConnector;
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
      InitiativeRestConnector initiativeRestConnector,
      IOBackEndRestConnector ioBackEndRestConnector,
      PdvDecryptRestConnector pdvDecryptRestConnector,
      NotificationManagerRepository notificationManagerRepository,
      NotificationDTOMapper notificationDTOMapper,
      NotificationMapper notificationMapper,
      NotificationMarkdown notificationMarkdown) {
    this.outcomeProducer = outcomeProducer;
    this.initiativeRestConnector = initiativeRestConnector;
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
//    ServiceResource serviceResource = getService(evaluationDTO.getServiceId());
    InitiativeAdditionalInfoDTO ioTokens = null;
    try {
      ioTokens = initiativeRestConnector.getIOTokens(evaluationDTO.getInitiativeId());
    } catch (FeignException e) {
      log.error("[NOTIFY] [%d] Cannot send request: %s".formatted(e.status(), e.contentUTF8()));
    }

    if (ioTokens == null) {
      notificationKO(notification);
      return;
    }

    log.info("[NOTIFY] Sending request to pdv");
    String fiscalCode = decryptUserToken(evaluationDTO.getUserId());

    if (fiscalCode == null) {
      notificationKO(notification);
      return;
    }

    if (isNotSenderAllowed(fiscalCode, ioTokens.getPrimaryTokenIO())) {
      notificationKO(notification);
      return;
    }

    String subject = notificationMarkdown.getSubject(evaluationDTO);
    String markdown = notificationMarkdown.getMarkdown(evaluationDTO);

    NotificationDTO notificationDTO =
        notificationDTOMapper.map(fiscalCode, timeToLive, subject, markdown);

    String notificationId = sendNotification(notificationDTO, ioTokens.getPrimaryTokenIO());

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

//  private ServiceResource getService(String serviceId) {
//    try {
//      return ioBackEndRestConnector.getService(serviceId);
//    } catch (FeignException e) {
//      log.error("[NOTIFY] [%d] Cannot send request: %s".formatted(e.status(), e.contentUTF8()));
//      return null;
//    }
//  }

  @Override
  public void sendNotificationFromOperationType(AnyOfNotificationQueueDTO anyOfNotificationQueueDTO) {
    String fiscalCode = null;
//    ServiceResource serviceResource = null;
    Notification notification = null;
    String subject = "";
    String markdown = "";
    InitiativeAdditionalInfoDTO ioTokens = null;
    if (anyOfNotificationQueueDTO instanceof NotificationCitizenOnQueueDTO notificationCitizenOnQueueDTO){
//      log.info(
//              "[NOTIFY] Sending request to IO getService with serviceId {}",
//              notificationCitizenOnQueueDTO.getServiceId());
      notification = notificationMapper.toEntity(notificationCitizenOnQueueDTO);
//      serviceResource = getService(notificationCitizenOnQueueDTO.getServiceId());
      log.debug("[NOTIFY] Getting IO Tokens");
      try {
        ioTokens = initiativeRestConnector.getIOTokens(notificationCitizenOnQueueDTO.getInitiativeId());
      } catch (FeignException e) {
        log.error("[NOTIFY] [%d] Cannot send request: %s".formatted(e.status(), e.contentUTF8()));
      }

      log.info("[NOTIFY] Sending request to pdv");
      fiscalCode = decryptUserToken(notificationCitizenOnQueueDTO.getUserId());

      subject = notificationMarkdown.getSubjectInitiativePublishing();
      markdown = notificationMarkdown.getMarkdownInitiativePublishing();
    }
    if (anyOfNotificationQueueDTO instanceof NotificationIbanQueueDTO notificationIbanQueueDTO){
//      log.info(
//              "[NOTIFY] Sending request to IO getService with serviceId {}",
//              notificationIbanQueueDTO.getServiceId());
      notification = notificationMapper.toEntity(notificationIbanQueueDTO);
//      serviceResource = getService(notificationIbanQueueDTO.getServiceId());
      log.debug("[NOTIFY] Getting IO Tokens");
      try {
        ioTokens = initiativeRestConnector.getIOTokens(notificationIbanQueueDTO.getInitiativeId());
      } catch (FeignException e) {
        log.error("[NOTIFY] [%d] Cannot send request: %s".formatted(e.status(), e.contentUTF8()));
      }

      log.info("[NOTIFY] Sending request to pdv");
      fiscalCode = decryptUserToken(notificationIbanQueueDTO.getUserId());

      subject = notificationMarkdown.getSubjectCheckIbanKo();
      markdown = notificationMarkdown.getMarkdownCheckIbanKo();
    }

//    if (serviceResource == null) {
    if (ioTokens == null) {
      if (notification != null) {
        notificationKO(notification);
      }
      return;
    }

    if (fiscalCode == null) {
      notificationKO(notification);
      return;
    }

    if (isNotSenderAllowed(fiscalCode, ioTokens.getPrimaryTokenIO())) {
      notificationKO(notification);
      return;
    }

    NotificationDTO notificationDTO =
        notificationDTOMapper.map(fiscalCode, timeToLive, subject, markdown);

    String notificationId = sendNotification(notificationDTO, ioTokens.getPrimaryTokenIO());

    if (notificationId == null) {
      notificationKO(notification);
      return;
    }

    log.info("[NOTIFY] Notification ID: {}", notificationId);

    notification.setNotificationId(notificationId);
    notification.setNotificationStatus("OK");
    notificationManagerRepository.save(notification);
  }
}
