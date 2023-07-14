package it.gov.pagopa.notification.manager.connector;

import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationResource;
import it.gov.pagopa.notification.manager.dto.ProfileResource;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

@Service
public class IOBackEndRestConnectorImpl implements IOBackEndRestConnector {

  private final IOBackEndRestClient ioBackEndRestClient;

  public IOBackEndRestConnectorImpl(IOBackEndRestClient ioBackEndRestClient) {
    this.ioBackEndRestClient = ioBackEndRestClient;
  }

  @Override
  public NotificationResource notify(@Valid NotificationDTO notificationDTO, String primaryKey) {
    return ioBackEndRestClient.notify(notificationDTO, primaryKey);
  }

  @Override
  public ProfileResource getProfile(String fiscalCode, String primaryKey) {
    return ioBackEndRestClient.getProfile(fiscalCode, primaryKey);
  }
}
