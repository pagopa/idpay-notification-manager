package it.gov.pagopa.notification.manager.dto.mapper;

import it.gov.pagopa.notification.manager.dto.MessageContent;
import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import org.springframework.stereotype.Service;

@Service
public class NotificationDTOMapper {


  public NotificationDTO map(
      String fiscalCode, Long timeToLive, String subject, String markdown) {

    final NotificationDTO notification = new NotificationDTO();
    notification.setFiscalCode(fiscalCode);
    notification.setTimeToLive(timeToLive);

    MessageContent messageContent = new MessageContent();
    messageContent.setSubject(subject);
    messageContent.setMarkdown(markdown);

    notification.setContent(messageContent);

    return notification;
  }
}
