package it.gov.pagopa.notification.manager.config;

import it.gov.pagopa.notification.manager.dto.initiative.InitiativeAdditionalInfoDTO;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Configuration
public class RedisConfig {

  @Bean
  public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {

    ObjectMapper mapper = new ObjectMapper();

    JacksonJsonRedisSerializer<InitiativeAdditionalInfoDTO> serializer = new JacksonJsonRedisSerializer<>(
        mapper, InitiativeAdditionalInfoDTO.class);

    return builder -> builder
        .withCacheConfiguration("initiativeToken",
            RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1))
                .serializeValuesWith(
                    SerializationPair.fromSerializer(serializer)));
  }
}
