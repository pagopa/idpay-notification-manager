package it.gov.pagopa.common.mongo.retry;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MongoRequestRateTooLargeRetryableAspectTest {

    private final MongoRequestRateTooLargeRetryableAspect aspect =
            new MongoRequestRateTooLargeRetryableAspect();

    @Test
    void executeJoinPointRetryable_shouldInvokeRetryer() throws Throwable {

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);

        when(signature.toShortString()).thenReturn("flow()");
        when(pjp.getSignature()).thenReturn(signature);

        try (MockedStatic<MongoRequestRateTooLargeRetryer> mocked =
                     mockStatic(MongoRequestRateTooLargeRetryer.class)) {

            mocked.when(() ->
                    MongoRequestRateTooLargeRetryer.execute(
                            eq("flow()"),
                            any(),
                            eq(3L),
                            eq(10L)
                    )
            ).thenReturn("RESULT");

            Object result = MongoRequestRateTooLargeRetryableAspect
                    .executeJoinPointRetryable(pjp, 3, 10);

            assertEquals("RESULT", result);
        }
    }

    @Test
    void executeJoinPointRetryable_shouldCallProceed() throws Throwable {

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);

        when(signature.toShortString()).thenReturn("flow()");
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.proceed()).thenReturn("OK");

        try (MockedStatic<MongoRequestRateTooLargeRetryer> mocked =
                     mockStatic(MongoRequestRateTooLargeRetryer.class)) {

            mocked.when(() ->
                    MongoRequestRateTooLargeRetryer.execute(any(), any(), anyLong(), anyLong())
            ).thenAnswer(invocation -> {
                var supplier = invocation.getArgument(1, java.util.function.Supplier.class);
                return supplier.get();
            });

            Object result = MongoRequestRateTooLargeRetryableAspect
                    .executeJoinPointRetryable(pjp, 3, 0);

            assertEquals("OK", result);
            verify(pjp).proceed();
        }
    }

    // --------------------------------------------------

    @Test
    void executeJoinPointRetryable_shouldWrapCheckedException() throws Throwable {

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);

        when(signature.toShortString()).thenReturn("flow()");
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.proceed()).thenThrow(new Exception("checked"));

        try (MockedStatic<MongoRequestRateTooLargeRetryer> mocked =
                     mockStatic(MongoRequestRateTooLargeRetryer.class)) {

            mocked.when(() ->
                    MongoRequestRateTooLargeRetryer.execute(any(), any(), anyLong(), anyLong())
            ).thenAnswer(invocation -> {
                var supplier = invocation.getArgument(1, java.util.function.Supplier.class);
                return supplier.get();
            });

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> MongoRequestRateTooLargeRetryableAspect
                            .executeJoinPointRetryable(pjp, 3, 0)
            );

            assertTrue(ex.getMessage().contains("REQUEST_RATE_TOO_LARGE_RETRY"));
        }
    }

    @Test
    void executeJoinPointRetryable_shouldPropagateRuntimeException() throws Throwable {

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);

        when(signature.toShortString()).thenReturn("flow()");
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.proceed()).thenThrow(new RuntimeException("boom"));

        try (MockedStatic<MongoRequestRateTooLargeRetryer> mocked =
                     mockStatic(MongoRequestRateTooLargeRetryer.class)) {

            mocked.when(() ->
                    MongoRequestRateTooLargeRetryer.execute(any(), any(), anyLong(), anyLong())
            ).thenAnswer(invocation -> {
                var supplier = invocation.getArgument(1, java.util.function.Supplier.class);
                return supplier.get();
            });

            assertThrows(
                    RuntimeException.class,
                    () -> MongoRequestRateTooLargeRetryableAspect
                            .executeJoinPointRetryable(pjp, 3, 0)
            );
        }
    }

    @Test
    void mongoRequestTooLargeRetryable_shouldDelegateToExecuteJoinPointRetryable() throws Throwable {

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MongoRequestRateTooLargeRetryable annotation =
                mock(MongoRequestRateTooLargeRetryable.class);

        when(annotation.maxRetry()).thenReturn(4L);
        when(annotation.maxMillisElapsed()).thenReturn(20L);

        try (MockedStatic<MongoRequestRateTooLargeRetryableAspect> mocked =
                     mockStatic(MongoRequestRateTooLargeRetryableAspect.class, CALLS_REAL_METHODS)) {

            mocked.when(() ->
                    MongoRequestRateTooLargeRetryableAspect
                            .executeJoinPointRetryable(pjp, 4, 20)
            ).thenReturn("DELEGATED");

            Object result = aspect.mongoRequestTooLargeRetryable(pjp, annotation);

            assertEquals("DELEGATED", result);
        }
    }
}
