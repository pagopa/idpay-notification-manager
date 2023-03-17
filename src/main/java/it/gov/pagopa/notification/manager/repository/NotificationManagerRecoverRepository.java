package it.gov.pagopa.notification.manager.repository;

import it.gov.pagopa.notification.manager.model.Notification;

import java.time.LocalDateTime;

public interface NotificationManagerRecoverRepository {
    Notification findKoToRecover(LocalDateTime startTime);
}
