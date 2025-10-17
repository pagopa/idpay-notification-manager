package it.gov.pagopa.notification.manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationReminderQueueDTO;
import it.gov.pagopa.notification.manager.enums.Channel;
import it.gov.pagopa.notification.manager.service.NotificationManagerService;
import it.gov.pagopa.notification.manager.service.WebNotificationManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;
import java.util.List;

import static it.gov.pagopa.notification.manager.enums.Channel.IO;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(
        value = {WebNotificationManagerController.class},
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class WebNotificationManagerControllerTest {

    private static final String BASE_URL = "/idpay/notifications/";

    private static final String OPERATION_TYPE = "REMINDER";
    private static final String USER_ID = "USER_ID";
    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    private static final String SERVICE_ID = "SERVICE_ID";
    private static final String INITIATIVE_NAME = "INITIATIVE_NAME";
    private static final String NAME = "NAME";
    private static final String SURNAME = "SURNAME";
    private static final String USER_MAIL = "USER_MAIL";

    private static final NotificationReminderQueueDTO NOTIFICATION_REMINDER_QUEUE_DTO =
            NotificationReminderQueueDTO.builder()
                    .operationType(OPERATION_TYPE)
                    .userId(USER_ID)
                    .initiativeId(INITIATIVE_ID)
                    .serviceId(SERVICE_ID)
                    .channel(Channel.WEB)
                    .initiativeName(INITIATIVE_NAME)
                    .name(NAME)
                    .surname(SURNAME)
                    .userMail(USER_MAIL)
                    .build();


    @MockBean
    WebNotificationManagerService webNotificationManagerServiceMock;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;


    @Test
    void sendReminderMail_ok() throws Exception {
        Mockito.doNothing().when(webNotificationManagerServiceMock).sendReminderMail(NOTIFICATION_REMINDER_QUEUE_DTO);

        mvc.perform(MockMvcRequestBuilders.put(BASE_URL + "sendReminderMail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(NOTIFICATION_REMINDER_QUEUE_DTO)))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andReturn();
    }
}
