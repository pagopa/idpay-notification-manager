package it.gov.pagopa.notification.manager.service.io;

import it.gov.pagopa.notification.manager.config.IoNotificationProperties;
import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.NotificationContent;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class IoNotificationCatalog {

    public static final String DEFAULT_PROFILE = "DEFAULT";

    private final Map<String, Map<NotificationEvent, NotificationContent>> profiles;

    public IoNotificationCatalog(
            IoNotificationProperties properties,
            ResourceLoader resourceLoader,
            Environment environment
    ) {
        Map<String, Map<NotificationEvent, NotificationContent>> loaded = new HashMap<>();
        for (String location : properties.getLocations()) {
            loadProfile(resourceLoader.getResource(location), loaded, environment);
        }
        if (!loaded.containsKey(DEFAULT_PROFILE)) {
            throw new IllegalArgumentException("The DEFAULT IO notification profile must be declared");
        }
        profiles = Map.copyOf(loaded);
    }

    public boolean containsProfile(String profile) {
        return profiles.containsKey(profile);
    }

    public Optional<NotificationContent> find(String profile, NotificationEvent event) {
        Map<NotificationEvent, NotificationContent> events = profiles.get(profile);
        return events == null ? Optional.empty() : Optional.ofNullable(events.get(event));
    }

    private void loadProfile(
            Resource resource,
            Map<String, Map<NotificationEvent, NotificationContent>> loaded,
            Environment environment
    ) {
        YamlMapFactoryBean yaml = new YamlMapFactoryBean();
        yaml.setResources(resource);
        Map<String, Object> document = yaml.getObject();
        if (document == null) {
            throw new IllegalArgumentException("Empty IO notification resource: " + resource);
        }
        String profile = requiredText(document.get("profile"), "profile", resource);
        if (loaded.containsKey(profile)) {
            throw new IllegalArgumentException("Duplicate IO notification profile: " + profile);
        }
        Map<?, ?> events = requiredMap(document.get("events"), "events", resource);
        Map<NotificationEvent, NotificationContent> contents = new HashMap<>();
        for (Map.Entry<?, ?> entry : events.entrySet()) {
            NotificationEvent event = NotificationEvent.valueOf(
                    requiredText(entry.getKey(), "event", resource)
            );
            Map<?, ?> content = requiredMap(entry.getValue(), event.name(), resource);
            contents.put(event, new NotificationContent(
                    environment.resolveRequiredPlaceholders(
                            requiredText(content.get("subject"), event + ".subject", resource)),
                    environment.resolveRequiredPlaceholders(
                            requiredText(content.get("markdown"), event + ".markdown", resource))
            ));
        }
        loaded.put(profile, Map.copyOf(contents));
    }

    private Map<?, ?> requiredMap(Object value, String field, Resource resource) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        throw new IllegalArgumentException("Expected a mapping for " + field + " in " + resource);
    }

    private String requiredText(Object value, String field, Resource resource) {
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        throw new IllegalArgumentException("Expected nonblank text for " + field + " in " + resource);
    }
}
