package it.gov.pagopa.notification.manager.exception;

import it.gov.pagopa.common.web.exception.ServiceException;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.ExceptionCode.NOT_FOUND_NOTIFICATION_EVENT_TEMPLATE;


public class NotFoundNotificationEventTemplate extends ServiceException {

  public NotFoundNotificationEventTemplate(String message) {
      super(NOT_FOUND_NOTIFICATION_EVENT_TEMPLATE, message);
  }

}
