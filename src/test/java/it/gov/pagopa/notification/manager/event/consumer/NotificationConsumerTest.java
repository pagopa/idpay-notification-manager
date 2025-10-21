package it.gov.pagopa.notification.manager.event.consumer;


import it.gov.pagopa.notification.manager.dto.event.AnyOfNotificationQueueDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationReminderQueueDTO;
import it.gov.pagopa.notification.manager.enums.Channel;
import it.gov.pagopa.notification.manager.service.NotificationManagerService;
import it.gov.pagopa.notification.manager.service.WebNotificationManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.function.Consumer;

import static org.mockito.Mockito.*;

class NotificationConsumerTest {

    private NotificationManagerService notificationManagerService;
    private WebNotificationManagerService webNotificationManagerService;

    private NotificationConsumer configuration;

    @BeforeEach
    void setUp() {
        notificationManagerService = Mockito.mock(NotificationManagerService.class);
        webNotificationManagerService = Mockito.mock(WebNotificationManagerService.class);
        configuration = new NotificationConsumer();
    }

    @Test
    void anyNotificationConsumer_whenReminderAndWeb_callsWebService() {
        NotificationReminderQueueDTO reminderDto = Mockito.mock(
                NotificationReminderQueueDTO.class,
                withSettings().extraInterfaces(AnyOfNotificationQueueDTO.class)
        );

        Channel channel = Mockito.mock(Channel.class);
        when(channel.isWeb()).thenReturn(true);
        when(reminderDto.getChannel()).thenReturn(channel);

        Consumer<AnyOfNotificationQueueDTO> consumer =
                configuration.anyNotificationConsumer(notificationManagerService, webNotificationManagerService);

        consumer.accept(reminderDto);

        verify(webNotificationManagerService, times(1)).sendReminderMail(reminderDto);
        verifyNoInteractions(notificationManagerService);
    }

    @Test
    void anyNotificationConsumer_whenReminderAndNotWeb_callsNotificationManager() {
        NotificationReminderQueueDTO reminderDto = Mockito.mock(
                NotificationReminderQueueDTO.class,
                withSettings().extraInterfaces(AnyOfNotificationQueueDTO.class)
        );

        Channel channel = Mockito.mock(Channel.class);
        when(channel.isWeb()).thenReturn(false);
        when(reminderDto.getChannel()).thenReturn(channel);

        Consumer<AnyOfNotificationQueueDTO> consumer =
                configuration.anyNotificationConsumer(notificationManagerService, webNotificationManagerService);

        consumer.accept(reminderDto);

        verify(notificationManagerService, times(1))
                .sendNotificationFromOperationType(reminderDto);
        verifyNoInteractions(webNotificationManagerService);
    }

    @Test
    void anyNotificationConsumer_whenGenericAnyOf_callsNotificationManager() {
        AnyOfNotificationQueueDTO genericDto = Mockito.mock(AnyOfNotificationQueueDTO.class);

        Consumer<AnyOfNotificationQueueDTO> consumer =
                configuration.anyNotificationConsumer(notificationManagerService, webNotificationManagerService);

        consumer.accept(genericDto);

        verify(notificationManagerService, times(1))
                .sendNotificationFromOperationType(genericDto);
        verifyNoInteractions(webNotificationManagerService);
    }
}
