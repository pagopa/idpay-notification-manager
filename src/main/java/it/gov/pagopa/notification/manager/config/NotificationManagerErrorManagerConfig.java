package it.gov.pagopa.notification.manager.config;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationManagerErrorManagerConfig {
    @Bean
    ErrorDTO defaultErrorDTO() {
        return new ErrorDTO(
                NotificationConstants.ExceptionCode.GENERIC_ERROR,
                "A generic error occurred"
        );
    }

    @Bean
    ErrorDTO tooManyRequestsErrorDTO() {
        return new ErrorDTO(NotificationConstants.ExceptionCode.TOO_MANY_REQUESTS, "Too Many Requests");
    }

    @Bean
    ErrorDTO templateValidationErrorDTO(){
        return new ErrorDTO(NotificationConstants.ExceptionCode.INVALID_REQUEST, null);
    }
}
