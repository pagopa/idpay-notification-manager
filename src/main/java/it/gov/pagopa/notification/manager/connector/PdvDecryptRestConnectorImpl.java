package it.gov.pagopa.notification.manager.connector;

import it.gov.pagopa.notification.manager.dto.FiscalCodeResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PdvDecryptRestConnectorImpl implements PdvDecryptRestConnector {

  private final PdvDecryptRestClient pdvDecryptRestClient;
  private final String subscriptionKey;

  public PdvDecryptRestConnectorImpl(
      PdvDecryptRestClient pdvDecryptRestClient,
      @Value("${rest-client.decrypt.api-key}") String subscriptionKey) {
    this.pdvDecryptRestClient = pdvDecryptRestClient;
    this.subscriptionKey = subscriptionKey;
  }

  @Override
  public FiscalCodeResource getPii(String token) {
    return pdvDecryptRestClient.getPii(token, subscriptionKey);
  }
}
