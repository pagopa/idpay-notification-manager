package it.gov.pagopa.notification.manager.utils;

import it.gov.pagopa.notification.manager.dto.PersonNameable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UtilsTest {

    private PersonNameable createPerson(String name, String surname) {
        return new PersonNameable() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getSurname() {
                return surname;
            }
        };
    }

    @Test
    void getNameSurname_returnsFullName() {
        PersonNameable person = createPerson("Mario", "Rossi");
        String result = Utils.getNameSurname(person);
        Assertions.assertEquals("Mario Rossi", result);
    }

    @Test
    void getNameSurname_withEmptyStrings() {
        PersonNameable person = createPerson("", "");
        String result = Utils.getNameSurname(person);
        Assertions.assertEquals(" ", result);
    }

    @Test
    void getNameSurname_withNullValues() {
        PersonNameable person = createPerson(null, null);
        String result = Utils.getNameSurname(person);
        Assertions.assertEquals("null null", result);
    }

    @Test
    void getNameSurname_withSurnameOnly() {
        PersonNameable person = createPerson(null, "Verdi");
        String result = Utils.getNameSurname(person);
        Assertions.assertEquals("null Verdi", result);
    }

    @Test
    void getNameSurname_withNameOnly() {
        PersonNameable person = createPerson("Luca", null);
        String result = Utils.getNameSurname(person);
        Assertions.assertEquals("Luca null", result);
    }
}

