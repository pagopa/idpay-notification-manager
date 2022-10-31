package it.gov.pagopa.notification.manager.dto.mapper;

import it.gov.pagopa.notification.manager.dto.MessageContent;
import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = NotificationDTOMapper.class)
class NotificationDTOMapperTest {
  private static final String FISCAL_CODE = "AAAAAA00A00A000A";
  private static final Long TTL = 3600L;
  private static final String SUBJECT = "SUBJECT";
  private static final String MARKDOWN = "MARKDOWN";
  private static final NotificationDTO NOTIFICATION_DTO = new NotificationDTO();

  static{
    MessageContent messageContent = new MessageContent();
    messageContent.setSubject(SUBJECT);
    messageContent.setMarkdown(MARKDOWN);
    NOTIFICATION_DTO.setFiscalCode(FISCAL_CODE);
    NOTIFICATION_DTO.setTimeToLive(TTL);
    NOTIFICATION_DTO.setContent(messageContent);
  }

  @Autowired
  NotificationDTOMapper notificationDTOMapper;

  @Test
  void map() {
    NotificationDTO actual = notificationDTOMapper.map(FISCAL_CODE, TTL, SUBJECT, MARKDOWN);

    assertEquals(NOTIFICATION_DTO, actual);
  }
}
