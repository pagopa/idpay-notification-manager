package it.gov.pagopa.notification.manager.repository;

import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.model.Notification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.Collections;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@TestPropertySource(properties = {
        "notification.manager.recover.max-retries=3",
        "notification.manager.recover.minutes-before=1440"
})
class NotificationManagerRecoverRepositoryImplTest {

    @Value("${notification.manager.recover.max-retries}")
    private static long maxRetries;
    @Value("${notification.manager.recover.minutes-before}")
    private static long minutesBefore;
    private static final LocalDateTime NOW = LocalDateTime.now();

    private static final Criteria stuckCriteria = Criteria.where(Notification.Fields.notificationStatus).is(NotificationConstants.NOTIFICATION_STATUS_RECOVER)
            .andOperator(
                    Criteria.where(Notification.Fields.retry).lt(maxRetries),
                    Criteria.where(Notification.Fields.retryDate).lt(NOW.minusMinutes(minutesBefore))
            );
    private static final Query STUCK_QUERY = Query.query(stuckCriteria);
    private static final Notification RECOVER_NOTIFICATION = Notification.builder()
            .notificationDate(NOW)
            .initiativeId("INITIATIVEID")
            .userId("USERID")
            .onboardingOutcome(NotificationConstants.STATUS_ONBOARDING_OK)
            .rejectReasons(Collections.emptyList())
            .notificationStatus(NotificationConstants.NOTIFICATION_STATUS_RECOVER)
            .retry(1)
            .retryDate(NOW)
            .build();

    private static final Query NEW_KO_QUERY = Query.query(
            Criteria.where(Notification.Fields.notificationStatus).is(NotificationConstants.NOTIFICATION_STATUS_KO)
                    .and(Notification.Fields.retry).lt(3)
    );
    private static final Notification KO_NOTIFICATION = Notification.builder()
            .notificationDate(NOW)
            .initiativeId("INITIATIVEID")
            .userId("USERID")
            .onboardingOutcome(NotificationConstants.STATUS_ONBOARDING_OK)
            .rejectReasons(Collections.emptyList())
            .notificationStatus(NotificationConstants.NOTIFICATION_STATUS_KO)
            .build();

    private NotificationManagerRecoverRepository repository;

    @MockBean
    MongoTemplate mongoTemplate;

    @BeforeEach
    void setup() {
        repository = new NotificationManagerRecoverRepositoryImpl(mongoTemplate);
    }

    @Test
    void testStuck() {
        Mockito.when(mongoTemplate.findAndModify(
                        Mockito.any(Query.class),
                        Mockito.any(Update.class),
                        Mockito.any(FindAndModifyOptions.class),
                        Mockito.eq(Notification.class)))
                .thenReturn(RECOVER_NOTIFICATION);

        Notification result = repository.findKoToRecover();

        Assertions.assertEquals(RECOVER_NOTIFICATION, result);
    }

    @Test
    void testNew() {
        Mockito.when(mongoTemplate.findAndModify(
                        Mockito.eq(STUCK_QUERY),
                        Mockito.any(Update.class),
                        Mockito.any(FindAndModifyOptions.class),
                        Mockito.eq(Notification.class)))
                .thenReturn(null);

        Notification expectedNotification = KO_NOTIFICATION.toBuilder().retry(1).notificationStatus(NotificationConstants.NOTIFICATION_STATUS_RECOVER).build();
        Mockito.when(mongoTemplate.findAndModify(
                        Mockito.eq(NEW_KO_QUERY),
                        Mockito.any(Update.class),
                        Mockito.any(FindAndModifyOptions.class),
                        Mockito.eq(Notification.class)))
                .thenReturn(expectedNotification);

        Notification result = repository.findKoToRecover();

        Assertions.assertEquals(expectedNotification, result);
    }

}