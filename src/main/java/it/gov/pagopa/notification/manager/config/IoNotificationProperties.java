package it.gov.pagopa.notification.manager.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "notification.manager.io")
@Getter
@Setter
public class IoNotificationProperties {

    private List<String> locations = List.of(
            "classpath:notifications/default.yml",
            "classpath:notifications/bel25.yml",
            "classpath:notifications/dec26.yml"
    );
}
