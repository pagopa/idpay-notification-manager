package it.gov.pagopa.notification.manager.connector;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import it.gov.pagopa.notification.manager.config.NotificationManagerConfig;
import it.gov.pagopa.notification.manager.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(
    initializers = IOBackEndRestClientTest.WireMockInitializer.class,
    classes = {
      IOBackEndRestConnectorImpl.class,
      NotificationManagerConfig.class,
      FeignAutoConfiguration.class,
      HttpMessageConvertersAutoConfiguration.class
    })
@TestPropertySource(
    locations = "classpath:application.yml",
    properties = {
      "spring.application.name=idpay-notification-manager-integration-rest",
      "rest-client.notification.backend-io.notify.url=/api/v1/messages",
      "rest-client.notification.backend-io.profile.url=/api/v1/profiles",
      "rest-client.notification.backend-io.service.url=/api/v1/services"
    })
class IOBackEndRestClientTest {

  private static final String FISCAL_CODE = "AAAAAA00A00A000A";
  private static final String PRIMARY_KEY = "PRIMARY_KEY";
  private static final String SERVICE_ID = "SERVICE_ID";

  @Autowired private IOBackEndRestClient restClient;

  @Autowired private IOBackEndRestConnector restConnector;

  @Test
  void notify_test() {


    final NotificationDTO notification = new NotificationDTO();
    notification.setFiscalCode("test");
    notification.setTimeToLive(3600L);

    MessageContent messageContent = new MessageContent();
    messageContent.setSubject("subject");
    messageContent.setMarkdown("markdown");

    notification.setContent(messageContent);

    final NotificationResource actualResponse = restConnector.notify(notification, PRIMARY_KEY);

    assertNotNull(actualResponse);
    assertEquals("ok", actualResponse.getId());
  }

  @Test
  void getProfile_test() {

    final ProfileResource actualResponse = restConnector.getProfile(FISCAL_CODE, PRIMARY_KEY);

    assertNotNull(actualResponse);
    assertTrue(actualResponse.isSenderAllowed());
    assertNotNull(actualResponse.getPreferredLanguages());
  }

  @Test
  void getService_test() {

    final ServiceResource actualResponse = restConnector.getService(SERVICE_ID);

    assertNotNull(actualResponse);
  }

  public static class WireMockInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      WireMockServer wireMockServer = new WireMockServer(new WireMockConfiguration().dynamicPort());
      wireMockServer.start();

      applicationContext.getBeanFactory().registerSingleton("wireMockServer", wireMockServer);

      applicationContext.addApplicationListener(
          applicationEvent -> {
            if (applicationEvent instanceof ContextClosedEvent) {
              wireMockServer.stop();
            }
          });

      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
          applicationContext,
          String.format(
              "rest-client.notification.backend-io.base-url=http://%s:%d",
              wireMockServer.getOptions().bindAddress(), wireMockServer.port()));
    }
  }
}
