package it.gov.pagopa.notification.manager.repository;

import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Slf4j
public class NotificationManagerRecoverRepositoryImpl implements NotificationManagerRecoverRepository {
    private final MongoTemplate mongoTemplate;

    public NotificationManagerRecoverRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Notification> findKoToRecover() {
        log.info("[NOTIFY] Fetching KO notifications and changing their status to RECOVER");

        return mongoTemplate.find(
                        Query.query(
                                Criteria.where(Notification.Fields.notificationId).isNull()
                                        .and(Notification.Fields.notificationStatus).is(NotificationConstants.NOTIFICATION_STATUS_KO)
                        ),
                        Notification.class
                )
                .stream()
                .map(this::updateRecoverNotification)
                .toList();
    }

    private Notification updateRecoverNotification(Notification n) {
        log.debug("[NOTIFY] Setting status RECOVER of KO notification with id {}", n.getId());

        n.setNotificationStatus(NotificationConstants.NOTIFICATION_STATUS_RECOVER);
        return mongoTemplate.save(n);
    }
}
