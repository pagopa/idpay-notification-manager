package it.gov.pagopa.notification.manager.connector;

import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationResource;
import it.gov.pagopa.notification.manager.dto.ProfileResource;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IOBackEndRestConnectorImpl implements IOBackEndRestConnector {

  private final String subscriptionKey;
  private final IOBackEndRestClient ioBackEndRestClient;

  public IOBackEndRestConnectorImpl(
      @Value("${rest-client.notification.backend-io.token-value}") String subscriptionKey,
      IOBackEndRestClient ioBackEndRestClient) {
    this.subscriptionKey = subscriptionKey;
    this.ioBackEndRestClient = ioBackEndRestClient;
  }

  @Override
  public NotificationResource notify(@Valid NotificationDTO notificationDTO) {
    return ioBackEndRestClient.notify(notificationDTO, subscriptionKey);
  }

  @Override
  public ProfileResource getProfile(String fiscalCode) {
    return ioBackEndRestClient.getProfile(fiscalCode, subscriptionKey);
  }
}
