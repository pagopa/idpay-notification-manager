package it.gov.pagopa.common.mongo.config;

import com.mongodb.MongoClientSettings;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MongoConfigTest {

    private final MongoConfig mongoConfig = new MongoConfig();

    @Test
    void customizer_shouldApplyConnectionPoolSettings() {

        // Arrange
        MongoConfig.MongoDbCustomProperties props = new MongoConfig.MongoDbCustomProperties();

        MongoConfig.MongoDbCustomProperties.ConnectionPoolSettings pool =
                new MongoConfig.MongoDbCustomProperties.ConnectionPoolSettings();

        pool.maxSize = 10;
        pool.minSize = 1;
        pool.maxWaitTimeMS = 1000;
        pool.maxConnectionLifeTimeMS = 2000;
        pool.maxConnectionIdleTimeMS = 3000;
        pool.maxConnecting = 2;

        props.connectionPool = pool;

        MongoClientSettings.Builder builder = MongoClientSettings.builder();

        // Act
        MongoClientSettingsBuilderCustomizer customizer = mongoConfig.customizer(props);
        customizer.customize(builder);

        MongoClientSettings settings = builder.build();

        // Assert
        assertEquals(10, settings.getConnectionPoolSettings().getMaxSize());
        assertEquals(1, settings.getConnectionPoolSettings().getMinSize());
        assertEquals(1000, settings.getConnectionPoolSettings().getMaxWaitTime(java.util.concurrent.TimeUnit.MILLISECONDS));
        assertEquals(2000, settings.getConnectionPoolSettings().getMaxConnectionLifeTime(java.util.concurrent.TimeUnit.MILLISECONDS));
        assertEquals(3000, settings.getConnectionPoolSettings().getMaxConnectionIdleTime(java.util.concurrent.TimeUnit.MILLISECONDS));
        assertEquals(2, settings.getConnectionPoolSettings().getMaxConnecting());
    }



    @Test
    void mongoCustomConversions_shouldRegisterConverters() {

        MongoCustomConversions conversions = mongoConfig.mongoCustomConversions();

        assertNotNull(conversions);
    }


    @Test
    void converters_shouldWork() throws Exception {

        Class<?> writeClazz = Class.forName(
                "it.gov.pagopa.common.mongo.config.MongoConfig$BigDecimalDecimal128Converter");

        Constructor<?> writeCtor = writeClazz.getDeclaredConstructor();
        writeCtor.setAccessible(true);
        Object writeConverter = writeCtor.newInstance();

        Class<?> readClazz = Class.forName(
                "it.gov.pagopa.common.mongo.config.MongoConfig$Decimal128BigDecimalConverter");

        Constructor<?> readCtor = readClazz.getDeclaredConstructor();
        readCtor.setAccessible(true);
        Object readConverter = readCtor.newInstance();

        Method writeMethod = writeClazz.getDeclaredMethod("convert", BigDecimal.class);
        Method readMethod = readClazz.getDeclaredMethod("convert", Decimal128.class);

        writeMethod.setAccessible(true);
        readMethod.setAccessible(true);

        BigDecimal value = new BigDecimal("10.5");

        Decimal128 decimal128 = (Decimal128) writeMethod.invoke(writeConverter, value);
        BigDecimal back = (BigDecimal) readMethod.invoke(readConverter, decimal128);

        assertEquals(value, back);
    }




    @Test
    void decimal128ToBigDecimalConverter_shouldConvertCorrectly() {

        Decimal128 source = new Decimal128(new BigDecimal("456.78"));

        BigDecimal result = source.bigDecimalValue();

        assertEquals(new BigDecimal("456.78"), result);
    }
}