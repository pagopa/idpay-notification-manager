package it.gov.pagopa.notification.manager.service.io.placeholder;

public interface NotificationPlaceholder<T> {

    String key();

    String resolve(T source);
}
