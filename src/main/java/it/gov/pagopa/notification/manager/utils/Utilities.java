package it.gov.pagopa.notification.manager.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Utilities {
    private Utilities() {}

    public static String sanitizeString(String str){
        if(str == null) {
            return null;
        }
        return str.replaceAll("[\\r\\n]", "") // remove new line and carriage return
                .replaceAll("[^\\w\\s-]", ""); // allow only alphanumeric, whitespace, dash
    }

    public static String sanitizeForLog(String value) {
        if (value == null) {
            return "null";
        }
        return value.replace('\r', '_').replace('\n', '_');
    }

}
