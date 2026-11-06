package it.gov.pagopa.notification.manager.config;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.notification.manager.exception.NotFoundNotificationEventTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ServiceExceptionConfig {

  @Bean
  public Map<Class<? extends ServiceException>, HttpStatus> serviceExceptionMapper() {
    Map<Class<? extends ServiceException>, HttpStatus> exceptionMap = new HashMap<>();

    // Unprocessable content
    exceptionMap.put(NotFoundNotificationEventTemplate.class, HttpStatus.UNPROCESSABLE_CONTENT);

    return exceptionMap;
  }

}
