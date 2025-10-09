package it.gov.pagopa.notification.manager.service;

import it.gov.pagopa.notification.manager.dto.event.NotificationReminderQueueDTO;

public interface WebNotificationManagerService {
    void sendReminderMail(NotificationReminderQueueDTO notificationQueueDTO);
}
