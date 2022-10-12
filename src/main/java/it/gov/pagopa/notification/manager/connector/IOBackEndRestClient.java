package it.gov.pagopa.notification.manager.connector;

import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationResource;
import it.gov.pagopa.notification.manager.dto.ProfileResource;
import it.gov.pagopa.notification.manager.dto.ServiceResource;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

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

  @GetMapping(
      value = "${rest-client.notification.backend-io.profile.url}/{fiscal_code}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  ProfileResource getProfile(
      @PathVariable("fiscal_code") String fiscalCode,
      @RequestHeader("Ocp-Apim-Subscription-Key") String token);

  @GetMapping(
      value = "${rest-client.notification.backend-io.service.url}/{service_id}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  ServiceResource getService(
      @PathVariable("service_id") String serviceId,
      @RequestHeader("Ocp-Apim-Subscription-Key") String token);
}
