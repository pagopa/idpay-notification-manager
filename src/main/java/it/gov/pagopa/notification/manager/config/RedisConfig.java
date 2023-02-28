package it.gov.pagopa.notification.manager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeAdditionalInfoDTO;
import java.time.Duration;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@Configuration
public class RedisConfig {
  @Bean
  public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {

    ObjectMapper mapper = new ObjectMapper();
    mapper.findAndRegisterModules();

    Jackson2JsonRedisSerializer<InitiativeAdditionalInfoDTO> serializer = new Jackson2JsonRedisSerializer<>(InitiativeAdditionalInfoDTO.class);
    serializer.setObjectMapper(mapper);

    return builder -> builder
        .withCacheConfiguration("initiativeToken",
            RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1))
                .serializeValuesWith(
                    SerializationPair.fromSerializer(serializer)));
  }
}
