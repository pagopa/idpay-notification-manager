package it.gov.pagopa.notification.manager.connector;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import it.gov.pagopa.notification.manager.dto.EmailMessageDTO;
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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(
        initializers = EmailNotificationFeignClientTest.WireMockInitializer.class,
        classes = {
                FeignAutoConfiguration.class,
                HttpMessageConvertersAutoConfiguration.class,
                EmailNotificationFeignClientTest.TestConfig.class
        })
@TestPropertySource(properties = "spring.application.name=idpay-notification-manager-integration-rest")
class EmailNotificationFeignClientTest {

    @Autowired
    private EmailNotificationFeignClient emailNotificationFeignClient;

    @Test
    void sendEmail_test() {
        EmailMessageDTO email = EmailMessageDTO.builder()
                .recipientEmail("test@pagopa.it")
                .senderEmail("noreply@pagopa.it")
                .subject("Test Subject")
                .content("Test body")
                .templateName("defaultTemplate")
                .templateValues(Map.of("key1", "value1"))
                .build();

        ResponseEntity<Void> response = emailNotificationFeignClient.sendEmail(email);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    public static class WireMockInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            WireMockServer wireMockServer = new WireMockServer(new WireMockConfiguration().dynamicPort());
            wireMockServer.start();

            wireMockServer.stubFor(post(urlEqualTo("/idpay/email-notification/notify"))
                    .willReturn(aResponse().withStatus(200)));

            applicationContext.getBeanFactory().registerSingleton("wireMockServer", wireMockServer);

            applicationContext.addApplicationListener(event -> {
                if (event instanceof ContextClosedEvent) {
                    wireMockServer.stop();
                }
            });

            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    String.format(
                            "rest-client.notification.email.base-url=http://%s:%d",
                            wireMockServer.getOptions().bindAddress(),
                            wireMockServer.port()
                    )
            );
        }
    }

    @org.springframework.context.annotation.Configuration
    @org.springframework.cloud.openfeign.EnableFeignClients(clients = EmailNotificationFeignClient.class)
    static class TestConfig {
    }
}
