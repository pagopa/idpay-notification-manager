package it.gov.pagopa.notification.manager.connector;

import it.gov.pagopa.notification.manager.dto.FiscalCodeDTO;
import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationResource;
import it.gov.pagopa.notification.manager.dto.ProfileResource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

public interface IOBackEndRestConnector {

  NotificationResource notify(@RequestBody @Valid NotificationDTO notificationDTO,String primaryKey);
  ProfileResource getProfile(@RequestBody @Valid FiscalCodeDTO fiscalCode, String primaryKey);

}
