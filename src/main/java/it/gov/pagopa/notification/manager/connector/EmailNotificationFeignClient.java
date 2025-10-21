package it.gov.pagopa.notification.manager.connector;

import it.gov.pagopa.notification.manager.dto.EmailMessageDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "${rest-client.notification.email-notification.service.name}",
        url = "${rest-client.notification.email-notification.service.base-url}"
)
public interface EmailNotificationFeignClient {

    @PostMapping("/idpay/email-notification/notify")
    ResponseEntity<Void> sendEmail(@RequestBody EmailMessageDTO body);
}
