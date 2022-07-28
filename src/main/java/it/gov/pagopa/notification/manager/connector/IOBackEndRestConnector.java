package it.gov.pagopa.notification.manager.connector;

import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationResource;
import it.gov.pagopa.notification.manager.dto.ProfileResource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;

public interface IOBackEndRestConnector {

  NotificationResource notify(@RequestBody @Valid NotificationDTO notificationDTO);
  ProfileResource getProfile(@PathVariable("fiscal_code") String fiscalCode);
}
