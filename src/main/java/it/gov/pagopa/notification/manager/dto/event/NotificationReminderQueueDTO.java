package it.gov.pagopa.notification.manager.dto.event;

import it.gov.pagopa.notification.manager.dto.PersonNameable;
import it.gov.pagopa.notification.manager.enums.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationReminderQueueDTO extends NotificationQueueDTO implements PersonNameable {
    private Channel channel;
    private String initiativeName;
    private String name;
    private String surname;
    private String userMail;
}
