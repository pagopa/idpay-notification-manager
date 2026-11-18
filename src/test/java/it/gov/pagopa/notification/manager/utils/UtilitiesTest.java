package it.gov.pagopa.notification.manager.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class UtilitiesTest {

    @Test
    void testPrivateConstructor() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<Utilities> constructor = Utilities.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Utilities instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    void sanitizeString_nullInput_returnsNull() {
        assertNull(Utilities.sanitizeString(null));
    }

    @Test
    void sanitizeString_removesNewLinesAndSpecialCharacters() {
        String input = "Hello\r\nWorld! 123_abc-def@#$%^&*()";
        String expected = "HelloWorld 123_abc-def";

        assertEquals(expected, Utilities.sanitizeString(input));
    }

    @Test
    void sanitizeString_keepsAlphanumericSpacesAndDashes() {
        String input = "Valid string-with_underscores and 123 numbers";
        assertEquals(input, Utilities.sanitizeString(input));
    }

    @Test
    void sanitizeForLog_nullInput_returnsNullLiteral() {
        assertEquals("null", Utilities.sanitizeForLog(null));
    }

    @Test
    void sanitizeForLog_replacesCarriageReturnAndNewlineWithUnderscore() {
        String input = "first\rsecond\nthird\r\nfourth";
        String expected = "first_second_third__fourth";

        assertEquals(expected, Utilities.sanitizeForLog(input));
    }

    @Test
    void sanitizeForLog_cleanInput_returnsUnchanged() {
        String input = "ordinary-log-message 123";
        assertEquals(input, Utilities.sanitizeForLog(input));
    }
}

