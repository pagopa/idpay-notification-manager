package it.gov.pagopa.notification.manager.connector.initiative;

import it.gov.pagopa.notification.manager.dto.initiative.InitiativeAdditionalInfoDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(
    name = "${rest-client.initiative.service.name}",
    url = "${rest-client.initiative.service.base-url}")
public interface InitiativeFeignRestClient {

  @GetMapping(
      value = "/{initiativeId}/token",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Cacheable(value = "initiativeToken", key = "#initiativeId")
  ResponseEntity<InitiativeAdditionalInfoDTO> getTokens(
          @PathVariable("initiativeId") String initiativeId);
}
