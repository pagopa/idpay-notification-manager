package it.gov.pagopa.notification.manager.repository;

import it.gov.pagopa.notification.manager.model.Notification;

import java.util.List;

public interface NotificationManagerRecoverRepository {
    Notification findKoToRecover();
}
