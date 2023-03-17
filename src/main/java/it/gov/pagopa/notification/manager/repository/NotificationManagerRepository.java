package it.gov.pagopa.notification.manager.repository;

import it.gov.pagopa.notification.manager.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationManagerRepository extends MongoRepository<Notification, String>, NotificationManagerRecoverRepository {}
