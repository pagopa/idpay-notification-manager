package it.gov.pagopa.notification.manager.repository;

import it.gov.pagopa.notification.manager.model.Notification;
import it.gov.pagopa.notification.manager.model.Notification.Fields;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Slf4j
public class NotificationManagerRepositoryExtendedImpl implements NotificationManagerRepositoryExtended{
    private final MongoTemplate mongoTemplate;

    public NotificationManagerRepositoryExtendedImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Notification> deletePaged(String initiativeId, int pageSize) {
        Pageable pageable = PageRequest.of(0, pageSize);
        return mongoTemplate.findAllAndRemove(
                Query.query(Criteria.where(Fields.initiativeId).is(initiativeId)).with(pageable),
                Notification.class
        );
    }
}
