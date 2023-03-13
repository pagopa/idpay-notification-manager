package it.gov.pagopa.notification.manager.repository;

import it.gov.pagopa.notification.manager.model.Notification;

public interface NotificationManagerRecoverRepository {
    Notification findKoToRecover();
}
