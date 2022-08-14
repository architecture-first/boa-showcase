package com.architecture.first.framework.business.vicinity.config;

import com.architecture.first.framework.business.vicinity.Vicinity;
import com.architecture.first.framework.business.vicinity.VicinityProxy;
import com.architecture.first.framework.business.vicinity.info.VicinityInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.clients.jedis.JedisPooled;

/**
 * Configuration to support Redis
 */
@Configuration
@Profile("vicinityClient")
public class VicinityConfig {

    @Bean
    public Vicinity vicinity() {
        return new VicinityProxy();
    }

}
