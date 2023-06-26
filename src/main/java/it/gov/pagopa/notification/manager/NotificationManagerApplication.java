package it.gov.pagopa.notification.manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
@EnableCaching
public class NotificationManagerApplication {

  public static void main(String[] args) {
    SpringApplication.run(NotificationManagerApplication.class, args);
  }

}
