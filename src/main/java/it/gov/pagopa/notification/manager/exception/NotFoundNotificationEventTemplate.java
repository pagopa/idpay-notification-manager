package it.gov.pagopa.notification.manager.exception;

import it.gov.pagopa.common.web.exception.ServiceException;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.ExceptionCode.NOT_FOUND_NOTIFICATION_EVENT_TEMPLATE;


public class NotFoundNotificationEventTemplate extends ServiceException {

  public NotFoundNotificationEventTemplate(String message, boolean printStackTrace, Throwable ex) {
    this(NOT_FOUND_NOTIFICATION_EVENT_TEMPLATE, message,printStackTrace, ex);
  }
  public NotFoundNotificationEventTemplate(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, null, printStackTrace, ex);
  }

  public NotFoundNotificationEventTemplate(String message) {
      this(NOT_FOUND_NOTIFICATION_EVENT_TEMPLATE, message);
  }

  public NotFoundNotificationEventTemplate(String code, String message) {
    this(code, message, false, null);
  }
}
