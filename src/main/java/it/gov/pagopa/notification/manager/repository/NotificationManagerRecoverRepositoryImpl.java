package it.gov.pagopa.notification.manager.repository;

import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@Slf4j
public class NotificationManagerRecoverRepositoryImpl implements NotificationManagerRecoverRepository {

    @Value("${notification.manager.recover.max-retries:3}")
    private long maxRetries;
    @Value("${notification.manager.recover.minutes-before:1440}")
    private long minutesBefore;

    private final MongoTemplate mongoTemplate;

    public NotificationManagerRecoverRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Notification findKoToRecover(LocalDateTime startTime) {
        Notification stuck;
        if ((stuck = findStuckRecover()) != null) {
            return stuck;
        } else {
            return findNewKo(startTime);
        }
    }

    private Notification findNewKo(LocalDateTime startTime) {
        Criteria criteria = Criteria.where(Notification.Fields.notificationStatus).is(NotificationConstants.NOTIFICATION_STATUS_KO)
                .andOperator(
                        Criteria.where(Notification.Fields.retry).lt(maxRetries),
                        Criteria.where(Notification.Fields.operationType).ne(NotificationConstants.OPERATION_TYPE_REMINDER),
                        new Criteria().orOperator(
                                Criteria.where(Notification.Fields.retryDate).isNull(),
                                Criteria.where(Notification.Fields.retryDate).lt(startTime)
                        )
                );

        return mongoTemplate.findAndModify(
                Query.query(criteria),
                new Update().set(Notification.Fields.notificationStatus, NotificationConstants.NOTIFICATION_STATUS_RECOVER).set(Notification.Fields.retryDate, LocalDateTime.now()),
                FindAndModifyOptions.options().returnNew(true),
                Notification.class
        );
    }

    private Notification findStuckRecover() {
        Criteria criteria = Criteria.where(Notification.Fields.notificationStatus).is(NotificationConstants.NOTIFICATION_STATUS_RECOVER)
                .andOperator(
                        Criteria.where(Notification.Fields.retry).lt(maxRetries),
                        Criteria.where(Notification.Fields.retryDate).lt(LocalDateTime.now().minusMinutes(minutesBefore)),
                        Criteria.where(Notification.Fields.operationType).ne(NotificationConstants.OPERATION_TYPE_REMINDER)
                );

        return mongoTemplate.findAndModify(
                Query.query(criteria),
                new Update().set(Notification.Fields.retryDate, LocalDateTime.now()),
                FindAndModifyOptions.options().returnNew(true),
                Notification.class
        );
    }

}
