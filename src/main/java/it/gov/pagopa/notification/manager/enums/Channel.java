package it.gov.pagopa.notification.manager.enums;

import lombok.Getter;

@Getter
public enum Channel {
    IO("IO"),
    WEB("WEB");

    private final String description;

    Channel(String description) {
        this.description = description;
    }

    public boolean isAppIo() {
        return this == IO;
    }

    public boolean isWeb() {
        return this == WEB;
    }
}
