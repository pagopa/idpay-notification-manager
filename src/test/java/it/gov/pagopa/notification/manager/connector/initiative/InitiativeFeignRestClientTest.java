package it.gov.pagopa.notification.manager.connector.initiative;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import it.gov.pagopa.notification.manager.config.NotificationManagerConfig;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeAdditionalInfoDTO;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeNotificationDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(
        initializers = InitiativeFeignRestClientTest.WireMockInitializer.class,
        classes = {
                InitiativeRestConnectorImpl.class,
                NotificationManagerConfig.class,
                FeignAutoConfiguration.class,
                HttpMessageConvertersAutoConfiguration.class
        })
@TestPropertySource(
        locations = "classpath:application.yml",
        properties = {
                "spring.application.name=idpay-notification-manager-integration-rest",
                "rest-client.notification.email.base-url=http://dummy",
                "rest-client.notification.email.notify.url=/dummy"
        })
class InitiativeFeignRestClientTest {
    private static final String INITIATIVE_ID = "INITIATIVE_ID";

    @Autowired private InitiativeRestConnector initiativeRestConnector;

    @Test
    void getService_test() {

        final InitiativeAdditionalInfoDTO actualResponse = initiativeRestConnector.getIOTokens(INITIATIVE_ID);

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
                            "rest-client.initiative.service.base-url=http://%s:%d/idpay/initiative",
                            wireMockServer.getOptions().bindAddress(), wireMockServer.port()));
        }
    }

    @Autowired
    private WireMockServer wireMockServer;

    @Test
    void getInitiativeDetailInfo_test() {
        wireMockServer.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.get(
                                com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching("/idpay/initiative/.*"))
                        .willReturn(
                                com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"emailFlux\":\"DEC26\"}")));

        final InitiativeNotificationDTO actualResponse = initiativeRestConnector.getInitiativeDetailInfo(INITIATIVE_ID);

        assertNotNull(actualResponse);
    }
}
