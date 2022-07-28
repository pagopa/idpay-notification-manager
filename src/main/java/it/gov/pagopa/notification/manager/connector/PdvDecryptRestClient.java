package it.gov.pagopa.notification.manager.connector;

import it.gov.pagopa.notification.manager.dto.FiscalCodeResource;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(name = "${rest-client.decrypt.serviceCode}", url = "${rest-client.decrypt.base-url}")
public interface PdvDecryptRestClient {

  @GetMapping(
      value = "/tokens/{token}/pii",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  FiscalCodeResource getPii(
      @PathVariable("token") String token,
      @RequestHeader("x-api-key") String apiKey);
}
