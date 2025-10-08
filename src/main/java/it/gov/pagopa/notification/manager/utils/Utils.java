package it.gov.pagopa.notification.manager.utils;

import it.gov.pagopa.notification.manager.dto.PersonNameable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@NoArgsConstructor
public class Utils {

    @NotNull
    public static <T extends PersonNameable> String getNameSurname(T dto) {
        return dto.getName() + " " + dto.getSurname();
    }
}
