package it.gov.pagopa.notification.manager.repository;

import it.gov.pagopa.notification.manager.model.Notification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = NotificationManagerRepositoryExtendedImpl.class)
class NotificationManagerRepositoryExtendedImplTest {
    @MockBean
    MongoTemplate mongoTemplate;
    @Autowired
    NotificationManagerRepositoryExtended notificationManagerRepositoryExtended;

    @Test
    void deletePaged() {
        String initiativeId = "initiativeId";
        int pageSize = 2;

        Notification notification = Notification.builder().initiativeId("initiativeId1").build();

        List<Notification> notificationList = new ArrayList<>();
        notificationList.add(notification);

        Mockito.when(mongoTemplate.findAllAndRemove(Mockito.any(Query.class), Mockito.eq(Notification.class)))
                .thenReturn(notificationList);

        List<Notification> deletedGroups = notificationManagerRepositoryExtended.deletePaged(initiativeId, pageSize);

        Mockito.verify(mongoTemplate, Mockito.times(1)).findAllAndRemove(Mockito.any(Query.class),Mockito.eq(Notification.class));

        Assertions.assertEquals(notificationList, deletedGroups);
    }


}