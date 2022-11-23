package it.gov.pagopa.notification.manager.service;

import feign.FeignException;
import it.gov.pagopa.notification.manager.connector.IOBackEndRestConnector;
import it.gov.pagopa.notification.manager.connector.PdvDecryptRestConnector;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationResource;
import it.gov.pagopa.notification.manager.dto.ProfileResource;
import it.gov.pagopa.notification.manager.dto.event.AnyOfNotificationQueueDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationCitizenOnQueueDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationIbanQueueDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationRefundQueueDTO;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeAdditionalInfoDTO;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationDTOMapper;
import it.gov.pagopa.notification.manager.dto.mapper.NotificationMapper;
import it.gov.pagopa.notification.manager.event.producer.OutcomeProducer;
import it.gov.pagopa.notification.manager.model.Notification;
import it.gov.pagopa.notification.manager.model.NotificationMarkdown;
import it.gov.pagopa.notification.manager.repository.NotificationManagerRepository;
import it.gov.pagopa.notification.manager.utils.AESUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationManagerServiceImpl implements NotificationManagerService {
  @Autowired
  AESUtil aesUtil;
  @Autowired
  OutcomeProducer outcomeProducer;
  @Autowired
  InitiativeRestConnector initiativeRestConnector;
  @Autowired
  IOBackEndRestConnector ioBackEndRestConnector;
  @Autowired
  NotificationManagerRepository notificationManagerRepository;
  @Autowired
  NotificationDTOMapper notificationDTOMapper;
  @Autowired
  PdvDecryptRestConnector pdvDecryptRestConnector;
  @Autowired
  NotificationMapper notificationMapper;
  @Autowired
  NotificationMarkdown notificationMarkdown;
  @Value("${util.crypto.aes.secret-type.pbe.passphrase}")
  private String passphrase;
  @Value("${rest-client.notification.backend-io.ttl}")
  private Long timeToLive;

  @Override
  public void notify(EvaluationDTO evaluationDTO) {
    Notification notification = notificationMapper.evaluationToNotification(evaluationDTO);
    InitiativeAdditionalInfoDTO ioTokens = null;

    try {
      log.info("[NOTIFY] Sending request to INITIATIVE");
      ioTokens = initiativeRestConnector.getIOTokens(evaluationDTO.getInitiativeId());
    } catch (FeignException e) {
      log.error(NotificationConstants.FEIGN_KO.formatted(e.status(), e.contentUTF8()));
    }

    if (ioTokens == null) {
      notificationKO(notification);
      return;
    }

    log.info(NotificationConstants.REQUEST_PDV);
    String fiscalCode = decryptUserToken(evaluationDTO.getUserId());

    if (fiscalCode == null) {
      notificationKO(notification);
      return;
    }

    log.info("[NOTIFY] Sending request to DECRYPT_TOKEN");
    String tokenDecrypt = aesUtil.decrypt(passphrase, ioTokens.getPrimaryTokenIO());

    if (isNotSenderAllowed(fiscalCode, tokenDecrypt)) {
      notificationKO(notification);
      return;
    }

    String subject = notificationMarkdown.getSubject(evaluationDTO);
    String markdown = notificationMarkdown.getMarkdown(evaluationDTO);

    NotificationDTO notificationDTO =
        notificationDTOMapper.map(fiscalCode, timeToLive, subject, markdown);

    String notificationId = sendNotification(notificationDTO, tokenDecrypt);

    if (notificationId == null) {
      notificationKO(notification);
      return;
    }

    log.info("[NOTIFY] Notification ID: {}", notificationId);

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
      log.info("[NOTIFY] Notification sent");
      return notificationResource.getId();
    } catch (FeignException e) {
      log.error("[NOTIFY] [{}] Cannot send notification: {}", e.status(), e.contentUTF8());
      return null;
    }
  }

  private String decryptUserToken(String token) {
    log.info(NotificationConstants.REQUEST_PDV);
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

  private InitiativeAdditionalInfoDTO getIoTokens(String initiativeId){
    log.debug(NotificationConstants.IO_TOKENS);
    try {
      return initiativeRestConnector.getIOTokens(initiativeId);
    } catch (FeignException e) {
      log.error(NotificationConstants.FEIGN_KO.formatted(e.status(), e.contentUTF8()));
      return null;
    }
  }

  @Override
  public void sendNotificationFromOperationType(AnyOfNotificationQueueDTO anyOfNotificationQueueDTO) {
    String fiscalCode = null;
    Notification notification = null;
    String subject = "";
    String markdown = "";
    InitiativeAdditionalInfoDTO ioTokens = null;

    if (anyOfNotificationQueueDTO instanceof NotificationCitizenOnQueueDTO notificationCitizenOnQueueDTO){

      notification = notificationMapper.toEntity(notificationCitizenOnQueueDTO);

      ioTokens = getIoTokens(notificationCitizenOnQueueDTO.getInitiativeId());

      fiscalCode = decryptUserToken(notificationCitizenOnQueueDTO.getUserId());

      subject = notificationMarkdown.getSubjectInitiativePublishing();
      markdown = notificationMarkdown.getMarkdownInitiativePublishing();
    }
    if (anyOfNotificationQueueDTO instanceof NotificationIbanQueueDTO notificationIbanQueueDTO){

      notification = notificationMapper.toEntity(notificationIbanQueueDTO);

      ioTokens = getIoTokens(notificationIbanQueueDTO.getInitiativeId());

      fiscalCode = decryptUserToken(notificationIbanQueueDTO.getUserId());

      subject = notificationMarkdown.getSubjectCheckIbanKo();
      markdown = notificationMarkdown.getMarkdownCheckIbanKo();
    }

    if (anyOfNotificationQueueDTO instanceof NotificationRefundQueueDTO notificationRefundOnQueueDTO){

      notification = notificationMapper.toEntity(notificationRefundOnQueueDTO);

      ioTokens = getIoTokens(notificationRefundOnQueueDTO.getInitiativeId());

      fiscalCode = decryptUserToken(notificationRefundOnQueueDTO.getUserId());

      subject = notificationMarkdown.getSubjectRefund(notificationRefundOnQueueDTO.getStatus());
      markdown = notificationMarkdown.getMarkdownRefund(notificationRefundOnQueueDTO.getStatus(),
          BigDecimal.valueOf(notificationRefundOnQueueDTO.getRefundReward()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_DOWN).toString());
    }

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

    String tokenDecrypt = aesUtil.decrypt(passphrase, ioTokens.getPrimaryTokenIO());

    if (isNotSenderAllowed(fiscalCode, tokenDecrypt)) {
      notificationKO(notification);
      return;
    }

    NotificationDTO notificationDTO =
        notificationDTOMapper.map(fiscalCode, timeToLive, subject, markdown);

    String notificationId = sendNotification(notificationDTO, tokenDecrypt);

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
