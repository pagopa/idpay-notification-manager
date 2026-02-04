package it.gov.pagopa.common.mongo.retry;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MongoRequestRateTooLargeAutomaticRetryAspectTest {

    @Mock
    ProceedingJoinPoint pjp;

    MongoRequestRateTooLargeAutomaticRetryAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new MongoRequestRateTooLargeAutomaticRetryAspect(
                true, 3, 0,
                true, 5, 0
        );
    }

    @AfterEach
    void cleanup() {
        RequestContextHolder.resetRequestAttributes();
    }

    static class TestController {
        @MongoRequestRateTooLargeApiRetryable(maxRetry = 7, maxMillisElapsed = 100)
        public void endpoint() {
            // This method is intentionally empty because it's used only to test
        }
    }

    static class TestControllerNoAnnotation {
        public void endpoint() {
            // This method is intentionally empty because it's used only to test
        }
    }

    @Test
    void batchContext_enabled_shouldRetry() throws Throwable {
        try (MockedStatic<MongoRequestRateTooLargeRetryableAspect> mocked =
                     mockStatic(MongoRequestRateTooLargeRetryableAspect.class)) {

            mocked.when(() ->
                    MongoRequestRateTooLargeRetryableAspect.executeJoinPointRetryable(
                            pjp, 5, 0)
            ).thenReturn("RETRY_OK");

            Object result = aspect.decorateRepositoryMethods(pjp);

            assertEquals("RETRY_OK", result);
        }
    }

    @Test
    void batchContext_disabled_shouldProceed() throws Throwable {

        aspect = new MongoRequestRateTooLargeAutomaticRetryAspect(
                true, 3, 0,
                false, 5, 0
        );

        when(pjp.proceed()).thenReturn("OK");

        Object result = aspect.decorateRepositoryMethods(pjp);

        assertEquals("OK", result);
    }

    @Test
    void apiContext_withAnnotation_shouldUseAnnotationConfig() throws Throwable {

        setControllerContext(TestController.class.getMethod("endpoint"));

        try (MockedStatic<MongoRequestRateTooLargeRetryableAspect> mocked =
                     mockStatic(MongoRequestRateTooLargeRetryableAspect.class)) {

            mocked.when(() ->
                    MongoRequestRateTooLargeRetryableAspect.executeJoinPointRetryable(
                            pjp, 7, 100)
            ).thenReturn("ANNOTATION_OK");

            Object result = aspect.decorateRepositoryMethods(pjp);

            assertEquals("ANNOTATION_OK", result);
        }
    }

    @Test
    void apiContext_withoutAnnotation_shouldUseDefaultApiConfig() throws Throwable {

        setControllerContext(TestControllerNoAnnotation.class.getMethod("endpoint"));

        try (MockedStatic<MongoRequestRateTooLargeRetryableAspect> mocked =
                     mockStatic(MongoRequestRateTooLargeRetryableAspect.class)) {

            mocked.when(() ->
                    MongoRequestRateTooLargeRetryableAspect.executeJoinPointRetryable(
                            pjp, 3, 0)
            ).thenReturn("DEFAULT_API_OK");

            Object result = aspect.decorateRepositoryMethods(pjp);

            assertEquals("DEFAULT_API_OK", result);
        }
    }


    private void setControllerContext(Method method) {
        HandlerMethod handlerMethod = new HandlerMethod(new Object(), method);

        RequestAttributes attrs = mock(RequestAttributes.class);
        when(attrs.getAttribute(
                HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE,
                RequestAttributes.SCOPE_REQUEST
        )).thenReturn(handlerMethod);

        RequestContextHolder.setRequestAttributes(attrs);
    }

}

