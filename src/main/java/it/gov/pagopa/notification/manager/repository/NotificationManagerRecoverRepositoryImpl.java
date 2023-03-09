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

    @Value("${notification.manager.recover.minutes-before}")
    private long minutesBefore;

    private final MongoTemplate mongoTemplate;

    public NotificationManagerRecoverRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Notification findKoToRecover() {
        Notification stuck;
        if ((stuck = findStuckRecover()) != null) {
            return stuck;
        } else {
            return findNewKo();
        }
    }

    private Notification findNewKo() {

        return mongoTemplate.findAndModify(
                Query.query(
                        Criteria.where(Notification.Fields.notificationStatus).is(NotificationConstants.NOTIFICATION_STATUS_KO)
                ),
                new Update().set(Notification.Fields.notificationStatus, NotificationConstants.NOTIFICATION_STATUS_RECOVER).set(Notification.Fields.retryDate, LocalDateTime.now()),
                FindAndModifyOptions.options().returnNew(true),
                Notification.class
        );
    }

    private Notification findStuckRecover() {

        return mongoTemplate.findAndModify(
                Query.query(
                        Criteria.where(Notification.Fields.notificationStatus).is(NotificationConstants.NOTIFICATION_STATUS_RECOVER)
                                .and(Notification.Fields.retryDate).lt(LocalDateTime.now().minusMinutes(minutesBefore))

                ),
                new Update().set(Notification.Fields.retryDate, LocalDateTime.now()),
                FindAndModifyOptions.options().returnNew(true),
                Notification.class
        );
    }

}
