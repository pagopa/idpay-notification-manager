package it.gov.pagopa.notification.manager.controller.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.service.onboarding.OnboardingIoNotificationImpl;
import it.gov.pagopa.notification.manager.service.onboarding.OnboardingWebNotificationImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;
import java.util.List;

import static it.gov.pagopa.notification.manager.enums.Channel.IO;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(
    value = {OnboardingNotificationController.class},
    excludeAutoConfiguration = SecurityAutoConfiguration.class)
class OnboardingNotificationControllerTest {

  private static final String BASE_URL = "/idpay/notifications/";

  private static final String USER_ID = "USER_ID";
  private static final String INITIATIVE_ID = "INITIATIVE_ID";
  private static final LocalDate TEST_DATE = LocalDate.now();

  private static final String ORGANIZATION_NAME = "ORGANIZATION_NAME";

  private static final String FISCAL_CODE = "FISCAL_CODE";
  private static final EvaluationDTO EVALUATION_DTO =
          new EvaluationDTO(
                  USER_ID,
                  INITIATIVE_ID,
                  INITIATIVE_ID,
                  TEST_DATE,
                  INITIATIVE_ID,
                  ORGANIZATION_NAME,
                  NotificationConstants.STATUS_ONBOARDING_OK,
                  TEST_DATE.atStartOfDay(),
                  TEST_DATE.atStartOfDay(),
                  List.of(),
                  50000L,
                  1L,
                  true,
                  null,
                  IO,
                  null,
                  null,
                  FISCAL_CODE,
                  null
          );


  @MockBean
  OnboardingWebNotificationImpl onboardingWebNotificationImplMock;
  @MockBean
  OnboardingIoNotificationImpl onboardingIoNotificationImplMock;

  @Autowired protected MockMvc mvc;

  @Autowired ObjectMapper objectMapper;



@Test
void processWebNotification_ok() throws Exception {

  Mockito.when(onboardingWebNotificationImplMock.processNotification(EVALUATION_DTO)).thenReturn("OK");


  mvc.perform(MockMvcRequestBuilders.put(BASE_URL + "processWebNotification")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(EVALUATION_DTO)))
          .andExpect(MockMvcResultMatchers.status().isOk())
          .andReturn();
}

  @Test
  void processIoNotification_ok() throws Exception {

    Mockito.when(onboardingIoNotificationImplMock.processNotification(EVALUATION_DTO)).thenReturn("OK");


    mvc.perform(MockMvcRequestBuilders.put(BASE_URL + "processIoNotification")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(EVALUATION_DTO)))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
  }
}