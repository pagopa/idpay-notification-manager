package it.gov.pagopa.notification.manager.dto.mapper;

import it.gov.pagopa.notification.manager.dto.MessageContent;
import it.gov.pagopa.notification.manager.dto.NotificationCheckIbanDTO;
import org.springframework.stereotype.Service;

@Service
public class NotificationCheckibanMapper {
  public NotificationCheckIbanDTO map(
      String fiscalCode, Long timeToLive, String subject, String markdown) {

    final NotificationCheckIbanDTO notification = new NotificationCheckIbanDTO();
    notification.setFiscalCode(fiscalCode);

    MessageContent messageContent = new MessageContent();
    messageContent.setSubject(subject);
    messageContent.setMarkdown(markdown);

    notification.setContent(messageContent);

    return notification;
  }

}
