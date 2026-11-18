package it.gov.pagopa.notification.manager.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Utilities {
    private Utilities() {}

    public static String sanitizeString(String str){
        return str == null? null: str.replaceAll("[\\r\\n]", "").replaceAll("[^\\w\\s-]", "");
    }

    public static String sanitizeForLog(String value) {
        if (value == null) {
            return "null";
        }
        return value.replace('\r', '_').replace('\n', '_');
    }

}
