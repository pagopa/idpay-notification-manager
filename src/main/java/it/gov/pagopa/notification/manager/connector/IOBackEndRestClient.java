package it.gov.pagopa.notification.manager.connector;

import it.gov.pagopa.notification.manager.dto.FiscalCodeDTO;
import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationResource;
import it.gov.pagopa.notification.manager.dto.ProfileResource;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(
    name = "${rest-client.notification.backend-io.serviceCode}",
    url = "${rest-client.notification.backend-io.base-url}")
public interface IOBackEndRestClient {

  @PostMapping(
      value = "${rest-client.notification.backend-io.notify.url}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  NotificationResource notify(
      @RequestBody @Valid NotificationDTO notificationDTO,
      @RequestHeader("Ocp-Apim-Subscription-Key") String token);

  @PostMapping(
      value = "${rest-client.notification.backend-io.profile.url}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  ProfileResource getProfile(
      @RequestBody @Valid FiscalCodeDTO fiscalCode,
      @RequestHeader("Ocp-Apim-Subscription-Key") String token);

}
