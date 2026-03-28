package it.gov.pagopa.common.mongo.retry;

import it.gov.pagopa.common.mongo.retry.exception.MongoRequestRateTooLargeRetryExpiredException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class MongoRequestRateTooLargeRetryerTest {


    @Test
    void isRequestRateTooLargeException_shouldDetect() {
        DataAccessException ex1 = new DummyDataAccessException("TooManyRequests");
        DataAccessException ex2 = new DummyDataAccessException("Error=16500, something");
        DataAccessException ex3 = new DummyDataAccessException("other error");

        assertTrue(MongoRequestRateTooLargeRetryer.isRequestRateTooLargeException(ex1));
        assertTrue(MongoRequestRateTooLargeRetryer.isRequestRateTooLargeException(ex2));
        assertFalse(MongoRequestRateTooLargeRetryer.isRequestRateTooLargeException(ex3));
    }

    @Test
    void getRetryAfterMs_shouldParse() {
        DataAccessException ex = new DummyDataAccessException("TooManyRequests RetryAfterMs=1234");
        DataAccessException exNo = new DummyDataAccessException("TooManyRequests");

        assertEquals(1234L, MongoRequestRateTooLargeRetryer.getRetryAfterMs(ex));
        assertNull(MongoRequestRateTooLargeRetryer.getRetryAfterMs(exNo));
    }

    @Test
    void execute_shouldReturnWithoutRetry() throws InterruptedException {
        String result = MongoRequestRateTooLargeRetryer.execute("flow", () -> "OK", 3, 1000);
        assertEquals("OK", result);
    }

    @Test
    void execute_shouldRetryAndSucceed() throws InterruptedException {

        AtomicInteger counter = new AtomicInteger(0);

        Supplier<String> logic = () -> {
            if (counter.incrementAndGet() < 3) {
                throw new DummyDataAccessException("TooManyRequests RetryAfterMs=1");
            }
            return "SUCCESS";
        };

        String result = MongoRequestRateTooLargeRetryer.execute("flow", logic, 5, 1000);
        assertEquals("SUCCESS", result);
        assertEquals(3, counter.get());
    }

    @Test
    void execute_shouldFailAfterMaxRetry() {
        Supplier<String> logic = () -> {
            throw new DummyDataAccessException("TooManyRequests");
        };

        MongoRequestRateTooLargeRetryExpiredException ex = assertThrows(
                MongoRequestRateTooLargeRetryExpiredException.class,
                () -> MongoRequestRateTooLargeRetryer.execute("flow", logic, 2, 1000)
        );

        assertTrue(ex.getMessage().contains("flow"));
    }


    @Test
    void execute_shouldFailAfterMaxMillisElapsed() {

        Supplier<String> logic = () -> {
            throw new DummyDataAccessException("TooManyRequests RetryAfterMs=50");
        };

        MongoRequestRateTooLargeRetryExpiredException ex = assertThrows(
                MongoRequestRateTooLargeRetryExpiredException.class,
                () -> MongoRequestRateTooLargeRetryer.execute("flow", logic, 10, 20)
        );

        assertTrue(ex.getMessage().contains("flow"));
    }


    @Test
    void execute_shouldThrowNonRetryable() {
        Supplier<String> logic = () -> {
            throw new DummyDataAccessException("OtherError");
        };

        DataAccessException ex = assertThrows(
                DataAccessException.class,
                () -> MongoRequestRateTooLargeRetryer.execute("flow", logic, 5, 1000)
        );

        assertEquals("OtherError", ex.getMessage());
    }

    private static class DummyDataAccessException extends DataAccessException {
        protected DummyDataAccessException(String msg) {
            super(msg);
        }
    }
}
