package it.gov.pagopa.notification.manager.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import it.gov.pagopa.notification.manager.connector.IOBackEndRestClient;
import it.gov.pagopa.notification.manager.connector.PdvDecryptRestClient;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeFeignRestClient;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(clients = {IOBackEndRestClient.class, PdvDecryptRestClient.class, InitiativeFeignRestClient.class})
@EnableCaching
public class NotificationManagerConfig {

  @Bean
  Config config(){
    Config config = new Config();
    MapConfig mapConfig = new MapConfig();
    mapConfig.setTimeToLiveSeconds(86400);
    config.getMapConfigs().put("IoTokenCache", mapConfig);
    return config;
  }

  private HazelcastInstance createHazelcastInstance(Config config) {
    config.getJetConfig().setEnabled(true);
    return createHazelcastInstance(config);
  }
}
