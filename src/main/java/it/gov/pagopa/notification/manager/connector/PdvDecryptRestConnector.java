package it.gov.pagopa.notification.manager.connector;

import it.gov.pagopa.notification.manager.dto.FiscalCodeResource;
import org.springframework.web.bind.annotation.PathVariable;

public interface PdvDecryptRestConnector {

  FiscalCodeResource getPii(@PathVariable("token") String token);
}
