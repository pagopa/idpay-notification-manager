package it.gov.pagopa.notification.manager.controller;

import it.gov.pagopa.notification.manager.dto.event.NotificationReminderQueueDTO;
import it.gov.pagopa.notification.manager.enums.Channel;
import it.gov.pagopa.notification.manager.service.WebNotificationManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(
        value = {WebNotificationManagerController.class}
        , excludeAutoConfiguration =  { UserDetailsServiceAutoConfiguration.class , SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
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


    @MockitoBean
    CacheManager cacheManager;

    @MockitoBean
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
