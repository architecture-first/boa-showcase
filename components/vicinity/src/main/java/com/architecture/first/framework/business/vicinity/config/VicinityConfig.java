package com.architecture.first.framework.business.vicinity.config;

import com.architecture.first.framework.business.vicinity.Vicinity;
import com.architecture.first.framework.business.vicinity.VicinityServer;
import com.architecture.first.framework.business.vicinity.info.VicinityInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration to support Redis
 */
@Configuration
@Profile("vicinityServer")
public class VicinityConfig {

    @Bean
    public Vicinity vicinity() {
        return new VicinityServer();
    }

}
